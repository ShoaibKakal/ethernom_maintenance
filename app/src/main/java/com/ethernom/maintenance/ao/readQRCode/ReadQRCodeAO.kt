package com.ethernom.maintenance.ao.readQRCode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ethernom.maintenance.ao.*
import com.ethernom.maintenance.ao.capsuleFactoryReset.CapsuleFactoryResetEvent
import com.ethernom.maintenance.ao.cm.CmType
import com.ethernom.maintenance.errorCode.ErrorCode
import com.ethernom.maintenance.model.RequestFailureModel
import com.ethernom.maintenance.ui.cmAPI
import com.ethernom.maintenance.ui.commonAO
import com.ethernom.maintenance.utils.*
import com.ethernom.maintenance.utils.AppConstant.TIMER
import com.ethernom.maintenance.utils.Conversion.convertHexToAscII

class ReadQRCodeAO(ctx: Context) {
    private val context: Context = ctx
    private val tag = javaClass.simpleName
    private var timeoutCapsule: Boolean = false
    private lateinit var mHandler: Handler

    /* Common AO Variable */
    private var readQrCodeFsm = arrayOf(
        FSM(currentState = ReadQRCodeState.INIT,            event = AoEvent.COMMON_INIT,                    nextState = ReadQRCodeState.WAIT_REQ,       af = ::afNothing),
        FSM(currentState = ReadQRCodeState.WAIT_REQ,        event = ReadQRCodeEvent.READ_QR_CODE_REQ,       nextState = ReadQRCodeState.READ_QR_CODE,   af = ::afReadQRRequest),
        FSM(currentState = ReadQRCodeState.READ_QR_CODE,    event = AoEvent.C2A_RQR_RES,                    nextState = ReadQRCodeState.READ_QR_CODE,   af = ::afReadQRResponse),
        FSM(currentState = ReadQRCodeState.READ_QR_CODE,    event = ReadQRCodeEvent.TIMEOUT_CAPSULE,        nextState = ReadQRCodeState.READ_QR_CODE,   af = ::afTimeoutCapsule),
        FSM(currentState = ReadQRCodeState.READ_QR_CODE,    event = ReadQRCodeEvent.READ_QR_CODE_FAILURE,   nextState = ReadQRCodeState.READ_QR_CODE,   af = ::afReadQRCodeFailure),
        FSM(currentState = ReadQRCodeState.READ_QR_CODE,    event = ReadQRCodeEvent.READ_QR_CODE_COMPLETED, nextState = ReadQRCodeState.WAIT_REQ,       af = ::afReadQRCompleted),
        FSM(currentState = AO_TABLE_END,                    event = INVALID_EVENT,                          nextState = AO_TABLE_END,                   af = ::afNothing),
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

    val readQRCodeAcb = ACB(
        id = AoId.AO_RQR_ID,
        currentState = ReadQRCodeState.INIT,
        eventQ = eventQ,
        fsm = readQrCodeFsm,
        //[LL, TP, ...]
        srvDescriptors = arrayOf(null, null)
    )

        // af functions
    private fun afNothing(acb: ACB, buffer: EventBuffer): Boolean {
        return true
    }

    private fun afReadQRRequest(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afReadQRRequest")
        /** Send the read QR code request to capsule
         * - cmSend(capsule,  Read_QR_Request)  to CM
         * - Set timer 10sec
         **/

        val payload = ByteArray(0)
        val data = Utils.makeAPDUHeader(APPCmd.A2C_RQR_REQ, payload)
        cmAPI!!.cmSend(CmType.capsule, data, null)

        timeoutCapsule = true
        mHandler = Handler(Looper.getMainLooper())
        mHandler.postDelayed({
            if(timeoutCapsule){
                timeoutCapsule = false
                val event = EventBuffer(eventId = CapsuleFactoryResetEvent.TIMEOUT_CAPSULE)
                commonAO!!.sendEvent(aoId = AoId.AO_RQR_ID, event)
            }
        }, TIMER)
        return true
    }

    private fun afReadQRResponse(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afReadQRResponse")
        /** Check status
         * - If status == Allow
         * cmSend(capsule, Read_QR_Complete) to CM
         * Set timer 10sec
         * Send_event(AO_READ_QR, READ_QR_COMPLETE)
         * - Else
         * Send_Event(AO_READ_QR, READ_QR_FAILURE(Read_QR_Unavailable))
         **/
        removeTimeout(timeoutCapsule)
        val data = buffer.buffer
        // Get status response from buffer
        //  +---------------+-----------------+
        //  | APP Header(4) | Payload         |
        //  +---------------+-----------------+
        val payloadData = data!!.copyOfRange(APDU_HEADER_SIZE, data.size)
        val status = payloadData.copyOfRange(0, 1)
        Log.d(tag, "status: ${status.hexa()}")
        if(status[0] == AppConstant.CAPSULE_RES_ALLOW) {
            // Payload format
            // +------------------+-----------------+-----------------+
            // |     Status(1)    | Device Name(N)  |       SN(8)     |
            // +------------------+-----------------+-----------------+
            val dataFormat = payloadData.copyOfRange(1, payloadData.size).hexa().split("1f")
            val deviceName = convertHexToAscII(dataFormat[0])
            val sn = dataFormat[1]

            val payload = ByteArray(0)
            val dataPayload = Utils.makeAPDUHeader(APPCmd.A2C_RQR_COM, payload)
            cmAPI!!.cmSend(CmType.capsule, dataPayload, null)
            val event = EventBuffer(ReadQRCodeEvent.READ_QR_CODE_COMPLETED, deviceName = deviceName, serialNumber = sn)
            commonAO!!.sendEvent(AoId.AO_RQR_ID, event)
        } else {
            // Payload format
            // +------------------+------------------+
            // |     Status(1)    |  Error Code(1)   |
            // +------------------+------------------+
            val errorCode = payloadData.copyOfRange(1, 2)
            Log.d(tag, "errorCode: ${errorCode.hexa()}")
            val errorMessage = ErrorCode.readQRCodeError[errorCode[0].toInt()]
            val requestFailure = RequestFailureModel(errorCode = errorCode[0].toInt(), errorMessage = errorMessage!!)
            val event = EventBuffer(eventId = ReadQRCodeEvent.READ_QR_CODE_FAILURE, requestFailure = requestFailure)
            commonAO!!.sendEvent(AoId.AO_RQR_ID, event)
        }

        return true
    }

    private fun afTimeoutCapsule(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afTimeoutCapsule")
        /** Send_Event (AO_READ_QR, READ_QR_FAILURE{capsule_timeout}) **/
        val errorMessage = ErrorCode.readQRCodeError[2]
        val requestFailure = RequestFailureModel(2, errorMessage = errorMessage!!)
        val event = EventBuffer(eventId = ReadQRCodeEvent.READ_QR_CODE_FAILURE, requestFailure = requestFailure)
        commonAO!!.sendEvent(AoId.AO_RQR_ID, event)
        return true
    }

    private fun afReadQRCodeFailure(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afReadQRCodeFailure")
        /** Broadcast Receiver(status) to UI to show Message to User **/
        val bundle = Bundle()
        bundle.putSerializable(AppConstant.CAPSULE_FAILURE_KEY, buffer.requestFailure)
        sendBroadCast(ReadQRCodeBRAction.READ_QR_CODE_FAILURE, bundle)
        return true
    }

    private fun afReadQRCompleted(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afReadQRCompleted")
        /** Broadcast Receiver(status) to UI to show Message to User **/
        val bundle = Bundle()
        bundle.putString(AppConstant.DEVICE_KEY, buffer.deviceName)
        bundle.putString(AppConstant.SERIAL_NUMBER_KEY, buffer.serialNumber)
        sendBroadCast(ReadQRCodeBRAction.READ_QR_CODE_COMPLETED, bundle)
        cmAPI!!.cmReset(CmType.capsule)
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

object ReadQRCodeState{
    const val INIT = 0
    const val WAIT_REQ = 1000
    const val READ_QR_CODE = 2000
}

object ReadQRCodeEvent{
    const val READ_QR_CODE_REQ = 16
    const val TIMEOUT_CAPSULE = 17
    const val READ_QR_CODE_FAILURE = 18
    const val READ_QR_CODE_COMPLETED = 19
}

object ReadQRCodeBRAction{
    const val READ_QR_CODE_RESPONSE = "com.ethernom.maintenance.READ_QR_CODE_RESPONSE"
    const val READ_QR_CODE_FAILURE = "com.ethernom.maintenance.READ_QR_CODE_FAILURE"
    const val READ_QR_CODE_COMPLETED = "com.ethernom.maintenance.READ_QR_CODE_COMPLETED"
}