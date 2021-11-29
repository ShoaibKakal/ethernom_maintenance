package com.ethernom.maintenance.ao.login

import android.content.Context
import android.util.Log
import com.ethernom.maintenance.ao.*
import com.ethernom.maintenance.ao.select.SelectState

class LoginAO (ctx: Context){
    private val tag: String = javaClass.simpleName
    private val context: Context = ctx

    /* Common AO Variable */
    private var loginFsm = arrayOf(
        FSM(currentState = LoginState.INIT,         event = AoEvent.COMMON_INIT,        nextState = LoginState.WAIT_REQ,    af = ::afNothing),
        FSM(currentState = LoginState.WAIT_REQ,     event = LoginEvent.LOGIN_REQ,       nextState = LoginState.LOGIN,       af = ::afLoginRequest),
        FSM(currentState = LoginState.LOGIN,        event = LoginEvent.LOGIN_RSP,       nextState = LoginState.LOGIN,       af = ::afLoginResponse),
        FSM(currentState = LoginState.LOGIN,        event = LoginEvent.TIMEOUT_SERVER,  nextState = LoginState.LOGIN,       af = ::afTimeoutServer),
        FSM(currentState = LoginState.LOGIN,        event = LoginEvent.LOGIN_FAILURE,   nextState = LoginState.LOGIN,       af = ::afLoginFailure),
        FSM(currentState = LoginState.LOGIN,        event = LoginEvent.LOGIN_COMPLETE,  nextState = LoginState.WAIT_REQ,       af = ::afLoginFailure),
        FSM(currentState = AO_TABLE_END,            event = INVALID_EVENT,              nextState = AO_TABLE_END,           af = ::afNothing),
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

    val loginAcb = ACB(
        id = AoId.AO_APP_ID,
        currentState = LoginState.INIT,
        eventQ = eventQ,
        fsm = loginFsm,
        //[LL, TP, ...]
        srvDescriptors = arrayOf(null, null)
    )

    // af function
    private fun afNothing(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afNothing")
        return true
    }

    private fun afLoginRequest(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afLoginRequest")
        /**Send login request to the server.**/
        //cmSend(srv, Login_Request{username,password}) to CM
        //Set timer 10sec

        return true
    }

    private fun afLoginResponse(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afLoginResponse")
        /** Check status **/
        /**
         * - If(status == success)
         * Send_event (AO_LOGIN, LOGIN_COMPLETED)
         * - Else
         * Send_event (AO_LOGIN, LOGIN_FAILURE{Login_failed})
        **/

        return true
    }

    private fun afTimeoutServer(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afTimeoutServer")
        /** Send_event(AO_LOGIN, LOGIN_FAILURE{timeout}) **/

        return true
    }

    private fun afLoginFailure(acb: ACB, buffer: EventBuffer): Boolean{
        Log.d(tag, "afLoginFailure")
        /** Broadcast Receiver(status failure) to UI to show message to User **/

        return true
    }
    private fun afLoginCompleted(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afLoginCompleted")
        /** Broadcast Receiver(Login Complete) to UI  transition to Discover Page **/
        return true
    }
}

object LoginState{
    const val INIT = 0
    const val WAIT_REQ = 1000
    const val LOGIN = 2000
}

object LoginEvent{
    const val LOGIN_REQ = 16
    const val LOGIN_RSP = 17
    const val TIMEOUT_SERVER = 18
    const val LOGIN_FAILURE = 19
    const val LOGIN_COMPLETE = 20
}

object LoginBRAction{
    const val ACT_LOGIN_COMPLETE = "com.ethernom.maintenance.ACT_LOGIN_COMPLETE"
    const val ACT_LOGIN_FAILURE = "com.ethernom.maintenance.ACT_LOGIN_FAILURE"
}