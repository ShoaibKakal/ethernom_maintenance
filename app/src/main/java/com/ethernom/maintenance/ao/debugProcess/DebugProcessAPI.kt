package com.ethernom.maintenance.ao.debugProcess

import com.ethernom.maintenance.ao.AoId
import com.ethernom.maintenance.ao.EventBuffer
import com.ethernom.maintenance.ui.commonAO

class DebugProcessAPI {
    fun debugProcessRequest(){
        val event = EventBuffer(eventId = DebugProcessEvent.DEBUG_PROCESS_REQ)
        commonAO!!.sendEvent(AoId.AO_DBP_ID, event)
        commonAO!!.aoRunScheduler()
    }

    fun updateCTRequest(status: Boolean){
        val event = EventBuffer(eventId = DebugProcessEvent.UPDATE_CT_REQ, updateCTStatus = status)
        commonAO!!.sendEvent(AoId.AO_DBP_ID, event)
        commonAO!!.aoRunScheduler()
    }

    fun closeUpdateCT(){
        val event = EventBuffer(eventId = DebugProcessEvent.CLOSE_REQ)
        commonAO!!.sendEvent(AoId.AO_DBP_ID, event)
        commonAO!!.aoRunScheduler()
    }
}