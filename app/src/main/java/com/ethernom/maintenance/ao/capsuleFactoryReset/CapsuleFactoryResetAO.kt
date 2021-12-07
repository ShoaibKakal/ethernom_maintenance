package com.ethernom.maintenance.ao.capsuleFactoryReset

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ethernom.maintenance.ao.*
import com.ethernom.maintenance.ao.cm.CmType
import com.ethernom.maintenance.ao.cm.SvrBuffer
import com.ethernom.maintenance.ao.cm.SvrBufferType
import com.ethernom.maintenance.errorCode.ErrorCode
import com.ethernom.maintenance.model.RequestFailureModel
import com.ethernom.maintenance.model.UnregisterRequestBody
import com.ethernom.maintenance.ui.cmAPI
import com.ethernom.maintenance.ui.commonAO
import com.ethernom.maintenance.utils.*
import com.ethernom.maintenance.utils.AppConstant.CAPSULE_FAILURE_KEY
import com.ethernom.maintenance.utils.AppConstant.TIMER

class CapsuleFactoryResetAO(ctx:Context) {
    private val tag: String = javaClass.simpleName
    private val context: Context = ctx
    private var timerCapsuleRequest: Boolean = false
    private var timerServerRequest: Boolean = false
    private lateinit var mHandler: Handler

    /* Common AO Variable */
    private var capsuleFactoryResetFsm = arrayOf(
        FSM(currentState = CapsuleFactoryResetState.INIT,           event = AoEvent.COMMON_INIT,                        nextState = CapsuleFactoryResetState.WAIT_REQ,          af = ::afNothing),
        FSM(currentState = CapsuleFactoryResetState.WAIT_REQ,       event = CapsuleFactoryResetEvent.RESET_REQ,         nextState = CapsuleFactoryResetState.SYSTEM_RESET,      af = ::afSpecialFactoryResetRequest),
        FSM(currentState = CapsuleFactoryResetState.SYSTEM_RESET,   event = AoEvent.C2A_CFR_RES,                        nextState = CapsuleFactoryResetState.SYSTEM_RESET,      af = ::afSpecialFactoryResetResponse),
        FSM(currentState = CapsuleFactoryResetState.SYSTEM_RESET,   event = CapsuleFactoryResetEvent.TIMEOUT_CAPSULE,   nextState = CapsuleFactoryResetState.SYSTEM_RESET,      af = ::afTimeoutCapsule),
        FSM(currentState = CapsuleFactoryResetState.SYSTEM_RESET,   event = CapsuleFactoryResetEvent.RESET_FAILURE,     nextState = CapsuleFactoryResetState.SYSTEM_RESET,      af = ::afSpecialFactoryResetFailure),
        FSM(currentState = CapsuleFactoryResetState.SYSTEM_RESET,   event = AoEvent.HTTP_RESET_CERT_RES,                nextState = CapsuleFactoryResetState.SYSTEM_RESET,      af = ::afResetCertResponse),
        FSM(currentState = CapsuleFactoryResetState.SYSTEM_RESET,   event = CapsuleFactoryResetEvent.TIMEOUT_SERVER,    nextState = CapsuleFactoryResetState.SYSTEM_RESET,      af = ::afTimeoutServer),
        FSM(currentState = CapsuleFactoryResetState.SYSTEM_RESET,   event = CapsuleFactoryResetEvent.RESET_COMPLETED,   nextState = CapsuleFactoryResetState.WAIT_REQ,          af = ::afSpecialFactoryResetCompleted),
        FSM(currentState = AO_TABLE_END,                            event = INVALID_EVENT,                              nextState = AO_TABLE_END,                               af = ::afNothing),
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

    val capsuleFactoryResetAcb = ACB(
        id = AoId.AO_CFR_ID,
        currentState = CapsuleFactoryResetState.INIT,
        eventQ = eventQ,
        fsm = capsuleFactoryResetFsm,
        //[LL, TP, ...]
        srvDescriptors = arrayOf(null, null)
    )

    // af functions
    private fun afNothing(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afNothing")
        return true
    }

    private fun afSpecialFactoryResetRequest(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afResetRequest")
        /** Send the special factory reset request to the capsule
         * - cmSend(capsule,  Special_Factory_Reset_Request )  to CM
         * - Set timer 10sec
         */

        val payload = ByteArray(0)
        val data = Utils.makeAPDUHeader(APPCmd.A2C_FR_REQ, payload)
        cmAPI!!.cmSend(CmType.capsule, data, null)

        timerCapsuleRequest = true
        mHandler = Handler(Looper.getMainLooper())
        mHandler.postDelayed({
            if(timerCapsuleRequest){
                timerCapsuleRequest = false
                val event = EventBuffer(eventId = CapsuleFactoryResetEvent.TIMEOUT_CAPSULE)
                commonAO!!.sendEvent(aoId = AoId.AO_CFR_ID, event)
            }
        }, TIMER)

        return true
    }

    private fun afSpecialFactoryResetResponse(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afResetResponse")
        /** Check status
        * - IF status == Allow
        * Broadcast Receiver(Request_Response) to UI
        * cmSend(srv, Reset_Cert{cert} )  to CM
        * Set timer 10sec
        * - Else
        * Send_Event (AO_RESET, RESET_FAILURE{Reset_Unavailable})
        **/
        removeTimeout(timerCapsuleRequest)
        val data = buffer.buffer
        // Get status response from buffer
        //  +---------------+-----------------+
        //  | APP Header(4) | Payload         |
        //  +---------------+-----------------+
        val payloadData = data!!.copyOfRange(APDU_HEADER_SIZE, data.size)
        val status = payloadData.copyOfRange(0, 1)
        Log.d(tag, "status: ${status.hexa()}")
        if(status[0] == AppConstant.CAPSULE_RES_DENY) {
            // Payload format
            // +------------------+------------------+
            // |     Status(1)    |  Error Code(1)   |
            // +------------------+------------------+
            val errorCode = payloadData.copyOfRange(1, 2)
            Log.d(tag, "errorCode: ${errorCode.hexa()}")
            val errorMessage = ErrorCode.factoryResetError[errorCode[0].toInt()]
            val requestFailure = RequestFailureModel(errorCode = errorCode[0].toInt(), errorMessage = errorMessage!!)
            val event = EventBuffer(eventId = CapsuleFactoryResetEvent.RESET_FAILURE, requestFailure = requestFailure)
            commonAO!!.sendEvent(AoId.AO_CFR_ID, event)
        } else {
            // Payload format
            // +------------------+------------------+
            // |     Status(1)    | Serial Number(8) |
            // +------------------+------------------+
            val csn = payloadData.copyOfRange(1, 9)
            val svrBuffer= SvrBuffer(SvrBufferType.unregisterReq, unregisterRequestBody = UnregisterRequestBody(cert = csn.hexa()))
            cmAPI!!.cmSend(CmType.server, null, svrBuffer)
            timerServerRequest = true
            mHandler.postDelayed({
                if(timerServerRequest){
                    timerServerRequest = false
                    val event = EventBuffer(eventId = CapsuleFactoryResetEvent.TIMEOUT_SERVER)
                    commonAO!!.sendEvent(aoId = AoId.AO_CFR_ID, event)
                }
            }, TIMER)
        }

        return true
    }

    private fun afTimeoutCapsule(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afTimeoutCapsule")
        /** Send_Event (AO_RESET, RESET_FAILURE{capsule_timeout}) **/
        removeTimeout(timerServerRequest)
        val errorMsg = ErrorCode.factoryResetError[2]
        val event = EventBuffer(eventId = CapsuleFactoryResetEvent.RESET_FAILURE, requestFailure = RequestFailureModel(2, errorMsg!!))
        commonAO!!.sendEvent(AoId.AO_CFR_ID, event)
        return true
    }

    private fun afSpecialFactoryResetFailure(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afResetFailure")
        /** Broadcast Receiver(status) to UI to show Message to User **/
        val bundle = Bundle()
        bundle.putSerializable(CAPSULE_FAILURE_KEY, buffer.requestFailure)
        sendBroadCast(CapsuleFactoryResetBRAction.ACT_RESET_FAILURE, bundle)
        return true
    }

    private fun afResetCertResponse(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afResetCertResponse")
        /** Send the factory reset completed to capsule by pass the token and param
        * - cmSend(capsule,  Factory_Reset_Completed {token, param} )  to CM
        * - Set timer 5sec
        **/
        removeTimeout(timerServerRequest)
        if(buffer.svrBuffer!!.responseFailed!!){
            val errorMsg = ErrorCode.factoryResetError[3]
            val event = EventBuffer(eventId = CapsuleFactoryResetEvent.RESET_FAILURE, requestFailure = RequestFailureModel(3, errorMsg!!))
            commonAO!!.sendEvent(AoId.AO_CFR_ID, event)
            return true
        }

        val payload = Utils.concatPayloadCapsuleFactoryReset()
        val data = Utils.makeAPDUHeader(APPCmd.A2C_FR_COM, payload)
        cmAPI!!.cmSend(CmType.capsule, data, null)

        val event = EventBuffer(eventId = CapsuleFactoryResetEvent.RESET_COMPLETED)
        commonAO!!.sendEvent(AoId.AO_CFR_ID, event)

        return true
    }

    private fun afTimeoutServer(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afTimeoutServer")
        /** Send_Event (AO_RESET, RESET_FAILURE{srv_timeout}) **/
        val errorMsg = ErrorCode.factoryResetError[2]
        val event = EventBuffer(eventId = CapsuleFactoryResetEvent.RESET_FAILURE, requestFailure = RequestFailureModel(2, errorMsg!!))
        commonAO!!.sendEvent(AoId.AO_CFR_ID, event)
        return true
    }

    private fun afSpecialFactoryResetCompleted(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afResetCompleted")
        /** Broadcast Receiver(Reset Complete ) to UI to notify the User **/
        sendBroadCast(CapsuleFactoryResetBRAction.ACT_RESET_COMPLETED)
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
}

object CapsuleFactoryResetState{
    const val INIT = 0
    const val WAIT_REQ = 1000
    const val SYSTEM_RESET = 2000
}

object CapsuleFactoryResetEvent{
    const val RESET_REQ = 16
    const val TIMEOUT_CAPSULE = 17
    const val RESET_FAILURE = 18
    const val TIMEOUT_SERVER = 19
    const val RESET_COMPLETED = 20
}

object CapsuleFactoryResetBRAction{
    const val ACT_RESET_RSP = "com.ethernom.maintenance.ACT_RESET_RSP"
    const val ACT_RESET_FAILURE = "com.ethernom.maintenance.ACT_RESET_FAILURE"
    const val ACT_RESET_COMPLETED = "com.ethernom.maintenance.ACT_RESET_COMPLETED"
}