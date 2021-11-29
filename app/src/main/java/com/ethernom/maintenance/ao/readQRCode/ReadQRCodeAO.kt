package com.ethernom.maintenance.ao.readQRCode

import android.content.Context
import android.util.Log
import com.ethernom.maintenance.ao.*
import com.ethernom.maintenance.ao.debugProcess.DebugProcessState

class ReadQRCodeAO(ctx: Context) {
    private val context: Context = ctx
    private val tag = javaClass.simpleName

    /* Common AO Variable */
    private var readQrCodeFsm = arrayOf(
        FSM(currentState = ReadQRCodeState.INIT,            event = AoEvent.COMMON_INIT,                    nextState = ReadQRCodeState.WAIT_REQ,       af = ::afNothing),
        FSM(currentState = ReadQRCodeState.WAIT_REQ,        event = ReadQRCodeEvent.READ_QR_CODE_REQ,       nextState = ReadQRCodeState.READ_QR_CODE,   af = ::afReadQRRequest),
        FSM(currentState = ReadQRCodeState.READ_QR_CODE,    event = ReadQRCodeEvent.READ_QR_CODE_REQ,       nextState = ReadQRCodeState.READ_QR_CODE,   af = ::afReadQRResponse),
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
        id = AoId.AO_APP_ID,
        currentState = DebugProcessState.INIT,
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

        return true
    }

    private fun afTimeoutCapsule(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afTimeoutCapsule")
        /** Send_Event (AO_READ_QR, READ_QR_FAILURE{capsule_timeout}) **/

        return true
    }

    private fun afReadQRCodeFailure(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afReadQRCodeFailure")
        /** Broadcast Receiver(status) to UI to show Message to User **/

        return true
    }

    private fun afReadQRCompleted(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afReadQRCompleted")
        /** Broadcast Receiver(status) to UI to show Message to User **/
        return true
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