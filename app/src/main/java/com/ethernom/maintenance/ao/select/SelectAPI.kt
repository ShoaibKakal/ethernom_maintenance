package com.ethernom.maintenance.ao.select

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.ethernom.maintenance.MainApplication
import com.ethernom.maintenance.ao.AoId
import com.ethernom.maintenance.ao.EventBuffer

@RequiresApi(Build.VERSION_CODES.Q)
class SelectAPI(ctx: Context) {
    private val application: MainApplication = ctx.applicationContext as MainApplication

    fun select() {
        val eventBuffer = EventBuffer(eventId = SelectEvent.SELECT)
        application.commonAO!!.sendEvent(aoId = AoId.AO_SL2_ID, eventBuffer)
        application.commonAO!!.aoRunScheduler()
    }

    fun qrDetected(certInfo: String) {
        val eventBuffer = EventBuffer(eventId = SelectEvent.QR_DETECTED, csn = certInfo)
        application.commonAO!!.sendEvent(aoId = AoId.AO_SL2_ID, eventBuffer)
        application.commonAO!!.aoRunScheduler()
    }
}