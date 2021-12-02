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
import com.ethernom.maintenance.model.UnregisterRequestBody
import com.ethernom.maintenance.model.UnregisterResponse
import com.ethernom.maintenance.ui.commonAO
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UnregisterRepository(ctx: Context) {
    private val tag: String = javaClass.simpleName
    private val context = ctx
    private val intent = Intent(BROADCAST_INTERRUPT)
    fun unregisterRequest(unregisterRequestBody: UnregisterRequestBody){
        val call: Call<UnregisterResponse> = ApiClient.getClient.unregister(unregisterRequestBody.cert)
        call.enqueue(object : Callback<UnregisterResponse> {
            override fun onResponse(call: Call<UnregisterResponse>, response: Response<UnregisterResponse>) {
                if (response.isSuccessful) {
                    Log.d(tag, "Unregister onResponse Success ${response.body()}")

                    // Send DATA REC event to CM AO
                    val svrBuffer = SvrBuffer(type = SvrBufferType.unregisterRes, unregisterResponse = response.body()!!)
                    val ef = EventBuffer(eventId = AoEvent.HTTP_DATA_REC, svrBuffer = svrBuffer)
                    commonAO!!.sendEvent(AoId.AO_CM2_ID, ef)

                } else {
                    sendEventToAO()
                    Log.d(tag, "Unregister onResponse Fail")
                }
                // send broadcast interrupt
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            }

            override fun onFailure(call: Call<UnregisterResponse>, t: Throwable) {
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