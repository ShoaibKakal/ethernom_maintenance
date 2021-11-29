package com.ethernom.maintenance.ao.capsuleFactoryReset

import android.content.Context
import android.util.Log
import com.ethernom.maintenance.ao.*
import com.ethernom.maintenance.ao.select.SelectState

class CapsuleFactoryResetAO(ctx:Context) {
    private val tag: String = javaClass.simpleName
    private val context: Context = ctx

    /* Common AO Variable */
    private var capsuleFactoryResetFsm = arrayOf(
        FSM(currentState = CapsuleFactoryResetState.INIT,           event = AoEvent.COMMON_INIT,                        nextState = CapsuleFactoryResetState.WAIT_REQ,          af = ::afNothing),
        FSM(currentState = CapsuleFactoryResetState.WAIT_REQ,       event = CapsuleFactoryResetEvent.RESET_REQ,         nextState = CapsuleFactoryResetState.SYSTEM_RESET,      af = ::afSpecialFactoryResetRequest),
        FSM(currentState = CapsuleFactoryResetState.SYSTEM_RESET,   event = CapsuleFactoryResetEvent.RESET_REQ,         nextState = CapsuleFactoryResetState.SYSTEM_RESET,      af = ::afSpecialFactoryResetResponse),
        FSM(currentState = CapsuleFactoryResetState.SYSTEM_RESET,   event = CapsuleFactoryResetEvent.TIMEOUT_CAPSULE,   nextState = CapsuleFactoryResetState.SYSTEM_RESET,      af = ::afTimeoutCapsule),
        FSM(currentState = CapsuleFactoryResetState.SYSTEM_RESET,   event = CapsuleFactoryResetEvent.RESET_FAILURE,     nextState = CapsuleFactoryResetState.SYSTEM_RESET,      af = ::afSpecialFactoryResetFailure),
        FSM(currentState = CapsuleFactoryResetState.SYSTEM_RESET,   event = CapsuleFactoryResetEvent.RESET_REQ,         nextState = CapsuleFactoryResetState.SYSTEM_RESET,      af = ::afResetCertResponse),
        FSM(currentState = CapsuleFactoryResetState.SYSTEM_RESET,   event = CapsuleFactoryResetEvent.TIMEOUT_SERVER,    nextState = CapsuleFactoryResetState.SYSTEM_RESET,      af = ::afTimeoutServer),
        FSM(currentState = CapsuleFactoryResetState.SYSTEM_RESET,   event = CapsuleFactoryResetEvent.RESET_REQ,         nextState = CapsuleFactoryResetState.SYSTEM_RESET,      af = ::afFactoryResetDone),
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
        id = AoId.AO_APP_ID,
        currentState = SelectState.INIT,
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

        return true
    }

    private fun afTimeoutCapsule(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afTimeoutCapsule")
        /** Send_Event (AO_RESET, RESET_FAILURE{capsule_timeout}) **/

        return true
    }

    private fun afSpecialFactoryResetFailure(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afResetFailure")
        /** Broadcast Receiver(status) to UI to show Message to User **/
        return true
    }

    private fun afResetCertResponse(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afResetCertResponse")
        /** Send the factory reset completed to capsule by pass the token and param
        * - cmSend(capsule,  Factory_Reset_Completed {token, param} )  to CM
        * - Set timer 10sec
        **/

        return true
    }

    private fun afTimeoutServer(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afTimeoutServer")
        /** Send_Event (AO_RESET, RESET_FAILURE{srv_timeout}) **/
        return true
    }

    private fun afFactoryResetDone(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afFactoryResetDone")
        /** Send_Event (AO_RESET, RESET_COMPLETED) **/

        return true
    }

    private fun afSpecialFactoryResetCompleted(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afResetCompleted")
        /** Broadcast Receiver(Reset Complete ) to UI to notify the User **/

        return true
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