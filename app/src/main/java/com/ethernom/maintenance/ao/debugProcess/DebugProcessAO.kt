package com.ethernom.maintenance.ao.debugProcess

import android.content.Context
import android.util.Log
import com.ethernom.maintenance.ao.*
import com.ethernom.maintenance.ao.select.SelectState

class DebugProcessAO(ctx: Context) {
    private val tag: String = javaClass.simpleName
    private val context: Context = ctx

    /* Common AO Variable */
    private var debugProcessFsm = arrayOf(
        FSM(currentState = DebugProcessState.INIT,          event = AoEvent.COMMON_INIT,                        nextState = DebugProcessState.WAIT_REQ,         af = ::afNothing),
        FSM(currentState = DebugProcessState.WAIT_REQ,      event = DebugProcessEvent.DEBUG_PROCESS_REQ,        nextState = DebugProcessState.DEBUG_PROCESS,    af = ::afDebugProcessRequest),
        FSM(currentState = DebugProcessState.DEBUG_PROCESS, event = DebugProcessEvent.DEBUG_PROCESS_REQ,        nextState = DebugProcessState.DEBUG_PROCESS,    af = ::afDebugProcessResponse),
        FSM(currentState = DebugProcessState.DEBUG_PROCESS, event = DebugProcessEvent.TIMEOUT_CAPSULE,          nextState = DebugProcessState.DEBUG_PROCESS,    af = ::afTimeoutCapsule),
        FSM(currentState = DebugProcessState.DEBUG_PROCESS, event = DebugProcessEvent.DEBUG_PROCESS_FAILURE,    nextState = DebugProcessState.DEBUG_PROCESS,    af = ::afDebugProcessFailure),
        FSM(currentState = DebugProcessState.DEBUG_PROCESS, event = DebugProcessEvent.DEBUG_PROCESS_DATA_RSP,   nextState = DebugProcessState.DEBUG_PROCESS,    af = ::afDebugProcessDataResponse),
        FSM(currentState = DebugProcessState.DEBUG_PROCESS, event = DebugProcessEvent.UPDATE_CT_REQ,            nextState = DebugProcessState.DEBUG_PROCESS,    af = ::afUpdateCTRequest),
        FSM(currentState = DebugProcessState.DEBUG_PROCESS, event = DebugProcessEvent.UPDATE_CT_RSP,            nextState = DebugProcessState.DEBUG_PROCESS,    af = ::afUpdateCTResponse),
        FSM(currentState = DebugProcessState.DEBUG_PROCESS, event = DebugProcessEvent.CLOSE_REQ,                nextState = DebugProcessState.DEBUG_PROCESS,    af = ::afCloseUpdateCTRequest),
        FSM(currentState = DebugProcessState.DEBUG_PROCESS, event = DebugProcessEvent.TIMEOUT_UPDATE_CT,        nextState = DebugProcessState.DEBUG_PROCESS,    af = ::afTimeoutUpdateCT),
        FSM(currentState = DebugProcessState.DEBUG_PROCESS, event = DebugProcessEvent.DEBUG_PROCESS_COMPLETED,  nextState = DebugProcessState.WAIT_REQ,         af = ::afDebugProcessCompleted),
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
        id = AoId.AO_APP_ID,
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

        return true
    }

    private fun afTimeoutCapsule(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afTimeoutCapsule")
        /** Send_Event (AO_DEBUG, DEBUG_PROCESS_FAILURE{capsule_timeout})} **/

        return true
    }

    private fun afDebugProcessFailure(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afDebugProcessFailure")
        /** Broadcast Receiver(status) to UI to show Message to User **/

        return true
    }

    private fun afDebugProcessDataResponse(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afDebugProcessDataResponse")
        /** Broadcast Receiver(battery_lvl, ct_status, ao_data) to UI for display **/

        return true
    }

    private fun afUpdateCTRequest(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afUpdateCTRequest")
        /** Send the update CT request(status) to capsule
         * - cmSend(capsule, Update_CT{status}) to CM
         * - Set timer 10sec
         **/

        return true
    }

    private fun afUpdateCTResponse(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afUpdateCTRResponse")
        /** Send the debug process completed to capsule
         * - cmSend(capsule, DEBUG_COMPLETED)  to CM
         * - Set timer 10sec
         * - Send_event (AO_DEBUG, DEBUG_COMPLETED)
         **/

        return true
    }

    private fun afCloseUpdateCTRequest(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afCloseRequest")
        /** Send the debug process completed to capsule
         * - cmSend(capsule, DEBUG_COMPLETED)  to CM
         * - Set timer 10sec
         * - Send_event (AO_DEBUG, DEBUG_COMPLETED)
         **/

        return true
    }

    private fun afTimeoutUpdateCT(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afTimeoutUpdateCT")
        /** Broadcast Receiver(update CT timeout) to UI **/

        return true
    }

    private fun afDebugProcessCompleted(acb: ACB, buffer: EventBuffer) : Boolean {
        Log.d(tag, "afDebugProcessCompleted")
        /**
         * Broadcast Receiver(Debug Complete ) to UI
         * Display all AO , CS, Event Q and battery Level  to the UI
         */


        return true
    }


}

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
    const val DEBUG_PROCESS_DATA_RSP = 19
    const val UPDATE_CT_REQ = 20
    const val UPDATE_CT_RSP = 21
    const val CLOSE_REQ = 22
    const val TIMEOUT_UPDATE_CT = 23
    const val DEBUG_PROCESS_COMPLETED = 24
}

object DebugProcessBRAction {
    const val ACT_DEBUG_PROCESS_RSP = "com.ethernom.maintenance.ACT_DEBUG_PRECESS_RSP"
    const val ACT_DEBUG_PROCESS_FAILURE = "com.ethernom.maintenance.ACT_DEBUG_PROCESS_FAILURE"
    const val ACT_DEBUG_PROCESS_DATA_RSP = "com.ethernom.maintenance.ACT_DEBUG_PROCESS_DATA_RSP"
    const val ACT_TIMEOUT_UPDATE_CT = "com.ethernom.maintenance.ACT_TIMEOUT_UPDATE_CT"
    const val ACT_DEBUG_PROCESS_COMPLETED = "com.ethernom.maintenance.ACT_DEBUG_PROCESS_COMPLETED"
}