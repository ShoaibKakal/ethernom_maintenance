package com.ethernom.maintenance.ao.capsuleFactoryReset

import com.ethernom.maintenance.ao.AoId
import com.ethernom.maintenance.ao.EventBuffer
import com.ethernom.maintenance.ui.commonAO

object CapsuleFactoryResetAPI {
    fun capsuleFactoryResetRequest(){
        val event = EventBuffer(eventId = CapsuleFactoryResetEvent.RESET_REQ)
        commonAO!!.sendEvent(aoId = AoId.AO_CFR_ID, event)
        commonAO!!.aoRunScheduler()
    }
}