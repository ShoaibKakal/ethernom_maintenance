package com.ethernom.maintenance.ao.cm

import android.content.Context
import com.ethernom.maintenance.MainApplication
import com.ethernom.maintenance.ao.AoId
import com.ethernom.maintenance.ao.CommonAO
import com.ethernom.maintenance.ao.EventBuffer
import com.ethernom.maintenance.ao.link.LinkDescriptor
import com.ethernom.maintenance.ui.commonAO

class CmAPI(ctx: Context) {
    private val tag: String = javaClass.simpleName

    fun cmDiscovery(type: Int) {
        if (type == CmType.capsule) {
            // Send TP Connect Event to own Event Queue
            val ef = EventBuffer(eventId = CmEvent.TP_DISCOVERY)
            commonAO!!.sendEvent(AoId.AO_CM1_ID, ef)
        }
    }

    fun cmSelect(type: Int, remoteDevice: LinkDescriptor) {
        if (type == CmType.capsule) {
            // Send TP Connect Event to own Event Queue
            val ef = EventBuffer(eventId = CmEvent.TP_SELECT, advPkt = remoteDevice)
            commonAO!!.sendEvent(AoId.AO_CM1_ID, ef)
        }
    }

    fun cmConnect(type: Int, csn: String) {
        if (type == CmType.capsule) {
            // Send TP Connect Event to own Event Queue
            val ef = EventBuffer(eventId = CmEvent.TP_CONNECT, csn = csn)
            commonAO!!.sendEvent(AoId.AO_CM1_ID, ef)
        }
    }

    fun cmSend(type: Int, data: ByteArray? = null, svrData: SvrBuffer? = null) {
        if (type == CmType.capsule) {
            // Send TP Send Event to own Event Queue
            val ef = EventBuffer(eventId = CmEvent.TP_SEND, buffer = data)
            commonAO!!.sendEvent(AoId.AO_CM1_ID, ef)
        } else if(type == CmType.server) {
            // Send HTTP Send Event to own Event Queue
            val ef = EventBuffer(eventId = CmEvent.HTTP_SEND, svrBuffer = svrData)
            commonAO!!.sendEvent(AoId.AO_CM2_ID, ef)
        }
    }

    fun cmDisconnect(type: Int) {
        if (type == CmType.capsule) {
            // Send TP Close Event to own Event Queue
            val ef = EventBuffer(eventId = CmEvent.TP_CLOSE)
            commonAO!!.sendEvent(AoId.AO_CM1_ID, ef)
        }
    }
    fun cmReset(type: Int) {
        if (type == CmType.capsule) {
            // Send TP Reset Event to own Event Queue
            val ef = EventBuffer(eventId = CmEvent.RESET)
            commonAO!!.sendEvent(AoId.AO_CM1_ID, ef)
        }
    }
}

object CmType {
    const val capsule = 0
    const val server = 1
}