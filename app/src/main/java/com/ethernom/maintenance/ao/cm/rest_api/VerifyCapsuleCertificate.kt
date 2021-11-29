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

class VerifyCapsuleCertificate(ctx: Context) {
    private val tag: String = javaClass.simpleName
    private val context = ctx
    private val application : MainApplication = (ctx.applicationContext as MainApplication)
    private val intent = Intent(BROADCAST_INTERRUPT)

    fun verifyCapsuleCert(cert: String) {
        val call: Call<VerifyCapsuleCertResponse> = ApiClient.getClient.verifyCapsuleCertificate(cert)
        call.enqueue(object : Callback<VerifyCapsuleCertResponse> {
            override fun onResponse(call: Call<VerifyCapsuleCertResponse>?, response: Response<VerifyCapsuleCertResponse>?) {
                if (response!!.isSuccessful) {
                    Log.d(tag, "Verify onResponse Success ${response.body()}")

                    val svrBuffer = SvrBuffer(type = SvrBufferType.verifyCertRsp, verifyCapsuleCertResponse = response.body())
                    val ef = EventBuffer(eventId = AoEvent.HTTP_DATA_REC, svrBuffer =svrBuffer)
                    application.commonAO!!.sendEvent(AoId.AO_CM2_ID, ef)
                } else {
                    sendEventToAO()
                    Log.d(tag, "Downloading onResponse Fail")
                }

                // Send DATA REC event to CM AO
                // send broadcast interrupt
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            }

            override fun onFailure(call: Call<VerifyCapsuleCertResponse>?, t: Throwable?) {
                Log.d(tag, "Downloading onFailure ${t?.message}")
                // send broadcast interrupt
                sendEventToAO()
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            }

        })
    }
    fun sendEventToAO() {
        val svrBuffer = SvrBuffer(type = SvrBufferType.verifyCertRsp, responseFailed = true)
        val ef = EventBuffer(eventId = AoEvent.HTTP_DATA_REC, svrBuffer =svrBuffer)
        application.commonAO!!.sendEvent(AoId.AO_CM2_ID, ef)
    }
}




data class VerifyCapsuleCertResponse (
        var verifyStat: Boolean,
        var sig: String? = ""
)