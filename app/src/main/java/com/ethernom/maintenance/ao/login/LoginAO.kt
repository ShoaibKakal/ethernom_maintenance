package com.ethernom.maintenance.ao.login

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
import com.ethernom.maintenance.ui.cmAPI
import com.ethernom.maintenance.ui.commonAO
import com.ethernom.maintenance.utils.AppConstant
import com.ethernom.maintenance.utils.AppConstant.TIMER

class LoginAO (ctx: Context){
    private val tag: String = javaClass.simpleName
    private val context: Context = ctx
    private lateinit var mHandler: Handler
    private var loginTimeout: Boolean = false

    /* Common AO Variable */
    private var loginFsm = arrayOf(
        FSM(currentState = LoginState.INIT,         event = AoEvent.COMMON_INIT,        nextState = LoginState.WAIT_REQ,    af = ::afNothing),
        FSM(currentState = LoginState.WAIT_REQ,     event = LoginEvent.LOGIN_REQ,       nextState = LoginState.LOGIN,       af = ::afLoginRequest),
        FSM(currentState = LoginState.LOGIN,        event = AoEvent.HTTP_LOGIN_RES,       nextState = LoginState.LOGIN,       af = ::afLoginResponse),
        FSM(currentState = LoginState.LOGIN,        event = LoginEvent.TIMEOUT_SERVER,  nextState = LoginState.LOGIN,       af = ::afTimeoutServer),
        FSM(currentState = LoginState.LOGIN,        event = LoginEvent.LOGIN_FAILURE,   nextState = LoginState.WAIT_REQ,       af = ::afLoginFailure),
        FSM(currentState = LoginState.LOGIN,        event = LoginEvent.LOGIN_COMPLETE,  nextState = LoginState.WAIT_REQ,       af = ::afLoginCompleted),
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
        id = AoId.AO_LOG_ID,
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
        val serverBuffer = SvrBuffer(SvrBufferType.loginReq, loginRequestBody = buffer.loginRequestBody)
        cmAPI!!.cmSend(CmType.server, null, serverBuffer)
        loginTimeout = true

        mHandler = Handler(Looper.getMainLooper())
        mHandler.postDelayed({
            if(loginTimeout){
                loginTimeout = false
                val event = EventBuffer(eventId = LoginEvent.TIMEOUT_SERVER)
                commonAO!!.sendEvent(AoId.AO_LOG_ID, event)
            }
        }, TIMER)


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
        removeTimeout(loginTimeout)
        val status = buffer.svrBuffer!!.loginResponse!!.status
        if(!buffer.svrBuffer!!.responseFailed!!){
            val event = if(status == AppConstant.LOGIN_SUCCESS) {
                EventBuffer(LoginEvent.LOGIN_COMPLETE)
            } else {
                val error = ErrorCode.loginError[0]
                EventBuffer(LoginEvent.LOGIN_FAILURE, requestFailure = RequestFailureModel(errorCode = 0, errorMessage = error!!))
            }
            commonAO!!.sendEvent(AoId.AO_LOG_ID, event)
        } else {
            val event = if(status.contains("Failed to connect")){
                val error = ErrorCode.loginError[0]
                EventBuffer(LoginEvent.LOGIN_FAILURE, requestFailure = RequestFailureModel(errorCode = 0, errorMessage = error!!))
            } else {
                val error = ErrorCode.loginError[2]
                EventBuffer(LoginEvent.LOGIN_FAILURE, requestFailure = RequestFailureModel(errorCode = 2, errorMessage = error!!))
            }
            commonAO!!.sendEvent(AoId.AO_LOG_ID, event)
        }

        return true
    }

    private fun afTimeoutServer(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afTimeoutServer")
        /** Send_event(AO_LOGIN, LOGIN_FAILURE{timeout}) **/
        val error = ErrorCode.loginError[1]
        val eventBuffer = EventBuffer(eventId = LoginEvent.LOGIN_FAILURE, requestFailure = RequestFailureModel(errorCode = 1, errorMessage = error!!) )
        commonAO!!.sendEvent(AoId.AO_LOG_ID, eventBuffer)
        return true
    }

    private fun afLoginFailure(acb: ACB, buffer: EventBuffer): Boolean{
        Log.d(tag, "afLoginFailure")
        /** Broadcast Receiver(status failure) to UI to show message to User **/
        val bundle = Bundle()
        bundle.putSerializable(AppConstant.LOGIN_FAILED, buffer.requestFailure)
        sendBroadCast(LoginBRAction.ACT_LOGIN_FAILURE, bundle)
        return true
    }
    private fun afLoginCompleted(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "afLoginCompleted")
        /** Broadcast Receiver(Login Complete) to UI  transition to Discover Page **/
        sendBroadCast(LoginBRAction.ACT_LOGIN_COMPLETE)
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

object LoginState{
    const val INIT = 0
    const val WAIT_REQ = 1000
    const val LOGIN = 2000
}

object LoginEvent{
    const val LOGIN_REQ = 16
    const val TIMEOUT_SERVER = 17
    const val LOGIN_FAILURE = 18
    const val LOGIN_COMPLETE = 19
}

object LoginBRAction{
    const val ACT_LOGIN_COMPLETE = "com.ethernom.maintenance.ACT_LOGIN_COMPLETE"
    const val ACT_LOGIN_FAILURE = "com.ethernom.maintenance.ACT_LOGIN_FAILURE"
}