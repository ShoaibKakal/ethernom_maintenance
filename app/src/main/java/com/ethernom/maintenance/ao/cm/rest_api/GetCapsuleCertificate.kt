package com.ethernom.maintenance.ao.cm.rest_api

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ethernom.maintenance.MainApplication
import com.ethernom.maintenance.ao.AoEvent
import com.ethernom.maintenance.ao.AoId

import com.ethernom.maintenance.ao.BROADCAST_INTERRUPT
import com.ethernom.maintenance.ao.EventBuffer
import com.ethernom.maintenance.ao.cm.SvrBuffer
import com.ethernom.maintenance.ao.cm.SvrBufferType
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GetCapsuleCertificate(ctx: Context) {
    private val tag: String = javaClass.simpleName
    private val context = ctx
    private val application: MainApplication = (context.applicationContext as MainApplication)
    private val intent = Intent(BROADCAST_INTERRUPT)

    fun getCapsuleCert(data: String) {
        val call: Call<CapsuleCertResponse> = ApiClient.getClient.getCapsuleCert()
        call.enqueue(object : Callback<CapsuleCertResponse> {
            override fun onResponse(call: Call<CapsuleCertResponse>?, response: Response<CapsuleCertResponse>?) {
                if (response!!.isSuccessful) {
                    Log.d(tag, "Downloading onResponse Success ${response.body()}")

                    val svrBuffer = SvrBuffer(type = SvrBufferType.capsuleCertRsp, capsuleCertResponse = response.body())
                    val ef = EventBuffer(eventId = AoEvent.HTTP_DATA_REC, svrBuffer =svrBuffer)
                    application.commonAO!!.sendEvent(AoId.AO_CM2_ID, ef)
                } else {
                    sendEventToAO()
                    Log.d(tag, "Downloading onResponse Fail")
                }

                // Send DATA REC event to CM AO
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            }

            override fun onFailure(call: Call<CapsuleCertResponse>?, t: Throwable?) {
                Log.d(tag, "Downloading onFailure ${t?.message}")
                sendEventToAO()
                // Send DATA REC event to CM AO
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            }

        })
    }
    fun sendEventToAO() {
        val svrBuffer = SvrBuffer(type = SvrBufferType.capsuleCertRsp, responseFailed = true)
        val ef = EventBuffer(eventId = AoEvent.HTTP_DATA_REC, svrBuffer =svrBuffer)
        application.commonAO!!.sendEvent(AoId.AO_CM2_ID, ef)
    }
}

data class CapsuleCertResponse (
        var results: String? = ""
)