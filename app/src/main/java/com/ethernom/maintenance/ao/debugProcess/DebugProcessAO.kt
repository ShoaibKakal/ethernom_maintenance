package com.ethernom.maintenance.ao.debugProcess

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ethernom.maintenance.ao.*
import com.ethernom.maintenance.ao.cm.CmType
import com.ethernom.maintenance.errorCode.ErrorCode
import com.ethernom.maintenance.model.CapsuleOAModel
import com.ethernom.maintenance.model.CapsuleStatusModel
import com.ethernom.maintenance.model.DebugProcessModel
import com.ethernom.maintenance.model.RequestFailureModel
import com.ethernom.maintenance.ui.cmAPI
import com.ethernom.maintenance.ui.commonAO
import com.ethernom.maintenance.utils.*
import com.ethernom.maintenance.utils.AppConstant.TIMER
import com.ethernom.maintenance.utils.Conversion.convertHexToDec

class DebugProcessAO(ctx: Context) {
    private val tag: String = javaClass.simpleName
    private val context: Context = ctx
    private var debugProcessTimeout: Boolean = false
    private var updateCTTimeout: Boolean = false
    private lateinit var mHandler: Handler

    /* Common AO Variable */
    private var debugProcessFsm = arrayOf(
        FSM(currentState = DebugProcessState.INIT,          event = AoEvent.COMMON_INIT,                        nextState = DebugProcessState.WAIT_REQ,         af = ::afNothing),
        FSM(currentState = DebugProcessState.WAIT_REQ,      event = DebugProcessEvent.DEBUG_PROCESS_REQ,        nextState = DebugProcessState.DEBUG_PROCESS,    af = ::afDebugProcessRequest),
        FSM(currentState = DebugProcessState.DEBUG_PROCESS, event = AoEvent.C2A_DBP_RES,                        nextState = DebugProcessState.DEBUG_PROCESS,    af = ::afDebugProcessResponse),
        FSM(currentState = DebugProcessState.DEBUG_PROCESS, event = DebugProcessEvent.TIMEOUT_CAPSULE,          nextState = DebugProcessState.DEBUG_PROCESS,    af = ::afTimeoutCapsule),
        FSM(currentState = DebugProcessState.DEBUG_PROCESS, event = DebugProcessEvent.DEBUG_PROCESS_FAILURE,    nextState = DebugProcessState.DEBUG_PROCESS,    af = ::afDebugProcessFailure),
        FSM(currentState = DebugProcessState.DEBUG_PROCESS, event = AoEvent.C2A_DPD_RES,                        nextState = DebugProcessState.UPDATE_CT,    af = ::afDebugProcessDataResponse),
        FSM(currentState = DebugProcessState.UPDATE_CT, event = DebugProcessEvent.UPDATE_CT_REQ,            nextState = DebugProcessState.UPDATE_CT,    af = ::afUpdateCTRequest),
        FSM(currentState = DebugProcessState.UPDATE_CT, event = AoEvent.C2A_UCT_RES,                        nextState = DebugProcessState.UPDATE_CT,    af = ::afUpdateCTResponse),
        FSM(currentState = DebugProcessState.UPDATE_CT, event = DebugProcessEvent.CLOSE_REQ,                nextState = DebugProcessState.UPDATE_CT,    af = ::afCloseUpdateCTRequest),
        FSM(currentState = DebugProcessState.UPDATE_CT, event = DebugProcessEvent.TIMEOUT_UPDATE_CT,        nextState = DebugProcessState.UPDATE_CT,    af = ::afTimeoutUpdateCT),
        FSM(currentState = DebugProcessState.UPDATE_CT, event = DebugProcessEvent.DEBUG_PROCESS_COMPLETED,  nextState = DebugProcessState.WAIT_REQ,         af = ::afDebugProcessCompleted),
        FSM(currentState = AO_TABLE_END,                    event = INVALID_EVENT,                              nextState = AO_TABLE_END,                       af = ::afNothing),
    )

    private var eventQ = EventQ(
        buffer = arrayOf(
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
        ), full = 0,
        consumerIdx = 0,
        producerIdx = 0)

    val debugProcessAcb = ACB(
        id = AoId.AO_DBP_ID,
        currentState = DebugProcessState.INIT,
        eventQ = eventQ,
        fsm = debugProcessFsm,
        //[LL, TP, ...]
        srvDescriptors = arrayOf(null, null)
    )

    // af functions
    private fun afNothing(acb: ACB, buffer: EventBuffer): Boolean {
        return true
    }

    private fun afDebugProcessRequest(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afDebugProcessRequest")
        /** Send the debug process request to capsule
        * - cmSend(capsule,  Debug_Request)  to CM
        * - Set timer 10sec
        **/
        val payload = ByteArray(0)
        val data = Utils.makeAPDUHeader(APPCmd.A2C_DBP_REQ, payload)
        cmAPI!!.cmSend(CmType.capsule, data, null)
        debugProcessTimeout = true

        mHandler = Handler(Looper.getMainLooper())
        mHandler.postDelayed({
            if(debugProcessTimeout){
                debugProcessTimeout = false
                val event = EventBuffer(eventId = DebugProcessEvent.TIMEOUT_CAPSULE)
                commonAO!!.sendEvent(AoId.AO_DBP_ID, event)
            }
        },TIMER)

        return true
    }

    private fun afDebugProcessResponse(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afDebugProcessResponse")
        /** Check status
         * - If status == Allow
         * Broadcast Receiver(Debug_Process_Response) to UI
         * cmSend(capsule,  DBP_Data_Request {battery_lvl, ct_status, ao_data} to CM
         * Set timer 10sec
         * - Else
         * Send_Event(AO_DEBUG, DEBUG_PROCESS_FAILURE(Debug_Unavailable)
         **/
        removeTimeout(debugProcessTimeout)
        val data = buffer.buffer
        // Get status response from buffer
        //  +---------------+-----------------+
        //  | APP Header(4) | Payload         |
        //  +---------------+-----------------+
        val payloadData = data!!.copyOfRange(APDU_HEADER_SIZE, data.size)
        val status = payloadData.copyOfRange(0, 1)
        Log.d(tag, "status: ${status.hexa()}")
        if(status[0] == AppConstant.CAPSULE_RES_ALLOW) {
            val payload = ByteArray(0)
            val cmd = APPCmd.A2C_DBP_DATA_REQ
            val dataAPDU = Utils.makeAPDUHeader(cmd, payload)
            cmAPI!!.cmSend(CmType.capsule, dataAPDU, null)
            debugProcessTimeout = true

            Handler(Looper.getMainLooper()).postDelayed({
                if(debugProcessTimeout){
                    debugProcessTimeout = false
                    val event = EventBuffer(eventId = DebugProcessEvent.TIMEOUT_CAPSULE)
                    commonAO!!.sendEvent(AoId.AO_DBP_ID, event)
                }
            },TIMER)

        } else {
            // Payload format
            // +------------------+------------------+
            // |     Status(1)    |  Error Code(1)   |
            // +------------------+------------------+
            val errorCode = payloadData.copyOfRange(1, 2)
            Log.d(tag, "errorCode: ${errorCode.hexa()}")
            val errorMessage = ErrorCode.debugProcessError[errorCode[0].toInt()]
            val requestFailure = RequestFailureModel(errorCode = errorCode[0].toInt(), errorMessage = errorMessage!!)
            val event = EventBuffer(eventId = DebugProcessEvent.DEBUG_PROCESS_FAILURE, requestFailure = requestFailure)
            commonAO!!.sendEvent(AoId.AO_DBP_ID, event)
        }

        return true
    }

    private fun afTimeoutCapsule(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afTimeoutCapsule")
        /** Send_Event (AO_DEBUG, DEBUG_PROCESS_FAILURE{capsule_timeout})} **/
        val errorMessage = ErrorCode.debugProcessError[2]
        val requestFailure = RequestFailureModel(2, errorMessage = errorMessage!!)
        val event = EventBuffer(eventId = DebugProcessEvent.DEBUG_PROCESS_FAILURE, requestFailure = requestFailure)
        commonAO!!.sendEvent(AoId.AO_DBP_ID, event)
        return true
    }

    private fun afDebugProcessFailure(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afDebugProcessFailure")
        /** Broadcast Receiver(status) to UI to show Message to User **/
        val bundle = Bundle()
        bundle.putSerializable(AppConstant.CAPSULE_FAILURE_KEY, buffer.requestFailure)
        sendBroadCast(DebugProcessBRAction.ACT_DEBUG_PROCESS_FAILURE, bundle)
        return true
    }

    private fun afDebugProcessDataResponse(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afDebugProcessDataResponse")
        /** Broadcast Receiver(battery_lvl, ct_status, ao_data) to UI for display **/
        //  +---------------+-------+--------------+---------------+--------------+--------------+----------------------+-----
        //  | APP Header(4) | BL(4) | CT-STATUS(1) | CTS-STATUS(1) | PA-STATUS(1) | TS-STATUS(1) | AO(1)|CS(1)|EVENT(1) | ....
        //  +---------------+-------+--------------+---------------+------+-------+--------------+----------------------+-----
        debugProcessTimeout = false
        val payload = buffer.buffer
        val payloadData = payload!!.copyOfRange(APDU_HEADER_SIZE, payload.size)
        val debugPayload = debugProcessData(payloadData)
        Log.d(tag, "debugPayload: $debugPayload")
        val bundle = Bundle()
        bundle.putSerializable(AppConstant.DEBUG_DATA_RES_KEY, debugPayload)
        sendBroadCast(DebugProcessBRAction.ACT_DEBUG_PROCESS_DATA_RSP, bundle)
        return true
    }

    private fun afUpdateCTRequest(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afUpdateCTRequest")
        /** Send the update CT request(status) to capsule
         * - cmSend(capsule, Update_CT{status}) to CM
         * - Set timer 10sec
         **/
        val ctStatusByte = if(buffer.updateCTStatus!!) 0x01.toByte()
        else 0x00.toByte()
        val payload = Utils.concatPayloadUpdateCT(ctStatusByte)
        val data = Utils.makeAPDUHeader(APPCmd.A2C_DBP_CT_REQ,payload )
        cmAPI!!.cmSend(CmType.capsule, data, null)
        updateCTTimeout = true

        mHandler.postDelayed({
            if(updateCTTimeout){
                updateCTTimeout = false
                sendBroadCast(DebugProcessBRAction.ACT_TIMEOUT_UPDATE_CT)
            }
        }, TIMER)

        return true
    }

    private fun afUpdateCTResponse(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afUpdateCTRResponse")
        /** Send the debug process completed to capsule
         * - cmSend(capsule, DEBUG_COMPLETED)  to CM
         * - Set timer 10sec
         * - Send_event (AO_DEBUG, DEBUG_COMPLETED)
         **/
        removeTimeout(updateCTTimeout)
        sendBroadCast(DebugProcessBRAction.ACT_UPDATE_CT_RES)
        Handler(Looper.getMainLooper()).postDelayed({

        }, 100)
        return true
    }

    private fun afCloseUpdateCTRequest(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afCloseRequest")
        /** Send the debug process completed to capsule
         * - cmSend(capsule, DEBUG_COMPLETED)  to CM
         * - Set timer 10sec
         * - Send_event (AO_DEBUG, DEBUG_COMPLETED)
         **/

        val payload = ByteArray(0)
        val data = Utils.makeAPDUHeader(APPCmd.A2C_DBP_COM,payload )
        cmAPI!!.cmSend(CmType.capsule, data, null)

        Handler(Looper.getMainLooper()).postDelayed({
            val event = EventBuffer(eventId = DebugProcessEvent.DEBUG_PROCESS_COMPLETED, completedType = 1)
            commonAO!!.sendEvent(AoId.AO_DBP_ID, event)
            commonAO!!.aoRunScheduler()
        }, 100)

        return true
    }

    private fun afTimeoutUpdateCT(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afTimeoutUpdateCT")
        /** Broadcast Receiver(update CT timeout) to UI **/
        sendBroadCast(DebugProcessBRAction.ACT_TIMEOUT_UPDATE_CT)
        return true
    }

    private fun afDebugProcessCompleted(acb: ACB, buffer: EventBuffer) : Boolean {
        Log.d(tag, "afDebugProcessCompleted")
        /**
         * Broadcast Receiver(Debug Complete ) to UI
         */
        val bundle = Bundle()
        bundle.putInt(AppConstant.COMPLETE_TYPE_KEY, buffer.completedType!!)
        sendBroadCast(DebugProcessBRAction.ACT_DEBUG_PROCESS_COMPLETED, bundle)
        return true
    }

    private fun sendBroadCast(action: String, bundle: Bundle? = null){
        val intentAction = Intent()
        if(bundle != null) intentAction.putExtras(bundle)
        intentAction.action = action
        LocalBroadcastManager.getInstance(context).sendBroadcast(intentAction)
    }

    private fun removeTimeout(timer: Boolean){
        if (!timer) return
        mHandler.removeCallbacksAndMessages(null)
    }

    private fun debugProcessData(dataPayload: ByteArray): DebugProcessModel{
        val bl = dataPayload.copyOfRange(0, 4).hexa()
        val ct = dataPayload.copyOfRange(4, 5)
        val cts= dataPayload.copyOfRange(5, 6)
        val pa = dataPayload.copyOfRange(6, 7)
        val uob= dataPayload.copyOfRange(7, 8)
        val ts = dataPayload.copyOfRange(8, 9)

        // convert Battery level to float
        val asLong: Long = bl.toLong(16)
        val asInt = asLong.toInt()
        val blLevel = java.lang.Float.intBitsToFloat(asInt)

        val valueTrue = 0x01.toByte()
        val ctValue = ct[0] == valueTrue
        val ctsValue = if (cts[0] == valueTrue) "Yes" else "No"
        val proximityAlarm = if (pa[0] == valueTrue) "Enable" else "Disable"
        val uobValue = if (uob[0] == valueTrue) "Yes" else "No"
        val tsValue = if (ts[0] == valueTrue) "Yes" else "No"

        val aoPayload = dataPayload.copyOfRange(9, dataPayload.size)
        val dataList = ArrayList<CapsuleOAModel>()

        var i = 0
        while (i < aoPayload.size) {
            val ao = copyRange(aoPayload, i, i + 1)
            val cs = copyRange(aoPayload, i + 1, i + 2)
            val event = copyRange(aoPayload,i + 2, i + 3)

            val aoId = convertHexToDec(ao.hexa())
            val csId = convertHexToDec(cs.hexa())
            val eventId = convertHexToDec(event.hexa())
            val res = hashMap(aoId.toInt(), csId.toInt())
            val data = CapsuleOAModel(ao = res.first, cs = res.second, event = eventId)
            dataList.add(data)
            i += 3// increase for 3 byte
        }

        return DebugProcessModel(bl = blLevel, ct = ctValue, cts = ctsValue, pa = proximityAlarm, uob = uobValue, ts = tsValue, capsuleOAs = dataList)
    }

    private fun copyRange(payload: ByteArray, fromIndex: Int, toIndex: Int) : ByteArray{
        return try {
            payload.copyOfRange(fromIndex, toIndex)
        } catch (ex: IndexOutOfBoundsException){
            ByteArray(100)
        }
    }

    private fun hashMap(id: Int, state:Int):Pair<String, String> {
        val btn_state = arrayOf("INIT","READY")
        val ct_state = arrayOf("INIT","BEACON","SCAN")
        val ffs_state = arrayOf("INIT","READY","ERASE","COMMIT","WRITE","READ","READ_TEK")
        val fr_state = arrayOf("INIT", "READY")
        val gtw_state = arrayOf("INIT","CMD","CTH","COB","UOB","MTC","ULD","DBP")
        val ka_state = arrayOf("INIT","WAIT_START","KEEP_ALIVE")
        val ldr_state = arrayOf("INIT","WAIT","START")
        val led_state = arrayOf("INIT","OFF","ON")
        val ll_state = arrayOf("INIT","WAIT_OPEN","WAIT_CONNECT","DATA","BEACONNING","SCANNING")
        val prox_state = arrayOf("INIT","READY","BUZZER")
        val spi_state = arrayOf("INIT","OPEN","DATA","OPERATING","WRITE","READ")
        val sr_state = arrayOf("INIT","RECOVERY")
        val sysf_state = arrayOf("INIT","READY","ERASE","WRITE","READ")
        val tmr_state = arrayOf("INIT","DATA")
        val tone_state = arrayOf("INIT","OFF","ON")
        val tp_state = arrayOf("INIT","OPEN","CONNECT","DATA")

        val map = arrayOf(
            AoMap("AO_SPI1", spi_state),
            AoMap("AO_SPI2", spi_state),
            AoMap("AO_SPI3", spi_state),
            AoMap("AO_LL1", ll_state),
            AoMap("AO_LL2", ll_state),
            AoMap("AO_TP1", tp_state),
            AoMap("AO_TP2", tp_state),
            AoMap("AO_TONE", tone_state),
            AoMap("AO_LED", led_state),
            AoMap("AO_BTN", btn_state),
            AoMap("AO_FFS1", ffs_state),
            AoMap("AO_FFS2", ffs_state),
            AoMap("AO_SYSF", sysf_state),
            AoMap("AO_KA", ka_state),
            AoMap("AO_PROX", prox_state),
            AoMap("AO_CT", ct_state),
            AoMap("AO_GTW", gtw_state),
            AoMap("AO_LDR", ldr_state),
            AoMap("AO_TMR", tmr_state),
            AoMap("AO_SR", sr_state),
            AoMap("AO_FR", fr_state),
        )

        if(map.size <= id) return Pair ("INVALID", "INVALID")

        if(map[id].state.size <= state)return Pair (map[id].ao, "INVALID")

        return Pair(map[id].ao, map[id].state[state] )
    }


}

data class AoMap(var ao:String, var state: Array<String>)

object DebugProcessState {
    const val INIT = 0
    const val WAIT_REQ = 1000
    const val DEBUG_PROCESS = 2000
    const val UPDATE_CT = 3000
}

object DebugProcessEvent {
    const val DEBUG_PROCESS_REQ = 16
    const val TIMEOUT_CAPSULE = 17
    const val DEBUG_PROCESS_FAILURE = 18
    const val UPDATE_CT_REQ = 19
    const val CLOSE_REQ = 20
    const val TIMEOUT_UPDATE_CT = 21
    const val DEBUG_PROCESS_COMPLETED = 22
}

object DebugProcessBRAction {
    const val ACT_DEBUG_PROCESS_RSP = "com.ethernom.maintenance.ACT_DEBUG_PRECESS_RSP"
    const val ACT_DEBUG_PROCESS_FAILURE = "com.ethernom.maintenance.ACT_DEBUG_PROCESS_FAILURE"
    const val ACT_DEBUG_PROCESS_DATA_RSP = "com.ethernom.maintenance.ACT_DEBUG_PROCESS_DATA_RSP"
    const val ACT_UPDATE_CT_RES = "com.ethernom.maintenance.ACT_UPDATE_CT_RES"
    const val ACT_TIMEOUT_UPDATE_CT = "com.ethernom.maintenance.ACT_TIMEOUT_UPDATE_CT"
    const val ACT_DEBUG_PROCESS_COMPLETED = "com.ethernom.maintenance.ACT_DEBUG_PROCESS_COMPLETED"
}