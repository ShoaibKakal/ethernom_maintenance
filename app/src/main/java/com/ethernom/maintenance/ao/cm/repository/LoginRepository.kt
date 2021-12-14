package com.ethernom.maintenance.ao.cm.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ethernom.maintenance.ao.AoEvent
import com.ethernom.maintenance.ao.AoId
import com.ethernom.maintenance.ao.BROADCAST_INTERRUPT
import com.ethernom.maintenance.ao.EventBuffer
import com.ethernom.maintenance.ao.cm.SvrBuffer
import com.ethernom.maintenance.ao.cm.SvrBufferType
import com.ethernom.maintenance.ao.cm.restApi.ApiClient
import com.ethernom.maintenance.model.LoginRequestBody
import com.ethernom.maintenance.model.LoginResponse
import com.ethernom.maintenance.ui.commonAO
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginRepository (ctx: Context){
    private val tag: String = javaClass.simpleName
    private val context = ctx
    private val intent = Intent(BROADCAST_INTERRUPT)

    fun loginRequest(loginRequestBody: LoginRequestBody){
        Log.d(tag, "loginRequestBody :$loginRequestBody")
        val call: Call<LoginResponse> = ApiClient.getClient.loginRequest(loginRequestBody)
        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                Log.d(tag, "Login onResponse Success ${response.body()}")
                if (response.isSuccessful) {
                    // Send DATA REC event to CM AO
                    val svrBuffer = SvrBuffer(type = SvrBufferType.loginRes, loginResponse = response.body()!!)
                    val ef = EventBuffer(eventId = AoEvent.HTTP_DATA_REC, svrBuffer = svrBuffer)
                    commonAO!!.sendEvent(AoId.AO_CM2_ID, ef)

                } else {
                    sendEventToAO()
                    Log.d(tag, "Login onResponse Fail")
                }
                // send broadcast interrupt
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.d(tag, "message: ${t.message}")
                sendEventToAO()
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            }
        })
    }

    private fun sendEventToAO() {
        val svrBuffer = SvrBuffer(type = SvrBufferType.unregisterRes, responseFailed = true)
        val ef = EventBuffer(eventId = AoEvent.HTTP_DATA_REC, svrBuffer =svrBuffer)
        commonAO!!.sendEvent(AoId.AO_CM2_ID, ef)
    }
}