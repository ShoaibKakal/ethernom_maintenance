package com.ethernom.maintenance.ao.login

import com.ethernom.maintenance.ao.AoId
import com.ethernom.maintenance.ao.EventBuffer
import com.ethernom.maintenance.model.LoginRequestBody
import com.ethernom.maintenance.ui.commonAO

class LoginAPI {
    fun loginRequest(username:String, password: String){
        val eventBuffer = EventBuffer(LoginEvent.LOGIN_REQ, loginRequestBody = LoginRequestBody(user = username, pass = password))
        commonAO!!.sendEvent(AoId.AO_LOG_ID, eventBuffer)
        commonAO!!.aoRunScheduler()
    }
}