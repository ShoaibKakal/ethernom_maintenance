package com.ethernom.maintenance.ao.transport

import android.content.Context
import android.util.Log
import com.ethernom.maintenance.ao.ACB
import com.ethernom.maintenance.ao.DescIdx
import com.ethernom.maintenance.ao.EventBuffer
import com.ethernom.maintenance.ao.SrvDesc
import com.ethernom.maintenance.ao.link.LinkDescriptor
import com.ethernom.maintenance.ui.commonAO

class TransportAPI(ctx: Context) {
    private val tag: String = javaClass.simpleName

    fun tpOpen(userAO: ACB?, srcPort: Byte, destPort: Byte, remoteDevice: String ): SrvDesc? {
        val freeTd = commonAO!!.transportAO.tpDescArr.find { it.aoUser == null } ?: return null
        Log.d(tag, "found TP")
        // bind ao_user & ao_tp
        freeTd.aoUser = userAO // bind to ao user to free transport descriptor
        // aoService = aoTP ACB; initialized during static initialization
        // init ACB of user AO   to point to TD
        freeTd.aoUser!!.srvDescriptors[DescIdx.TP_DESC] = freeTd
        freeTd.aoService.srvDescriptors[DescIdx.TP_DESC] = freeTd

        // Update Transport Descriptor Specific Data
        freeTd.sd!!.srcPort = srcPort
        freeTd.sd!!.destPort = destPort
        freeTd.sd!!.remoteDevice = remoteDevice

        Log.d(tag, "Remote Device $remoteDevice")
        // Send Open Event to own Event Queue
        val ef = EventBuffer(eventId = TpEvent.OPEN, srvDesc = freeTd)
        commonAO!!.sendEvent(freeTd.aoService.id, ef)

        return freeTd
    }

    fun tpSelect(td: SrvDesc, remoteDevice: LinkDescriptor):Int {
        // find sd by user ao id
        val currentTd = commonAO!!.transportAO.tpDescArr.find { it.aoService.id == td.aoService.id }
        // Send Event to Event Q of TP AO
        val eventBuffer = EventBuffer(eventId = TpEvent.SELECT, advPkt = remoteDevice, srvDesc = currentTd)
        commonAO!!.sendEvent(currentTd!!.aoService.id, eventBuffer)
        return 0
    }

    fun tpSend(td: SrvDesc, data:ByteArray):Int {
        // find sd by user ao id
        val currentTd = commonAO!!.transportAO.tpDescArr.find { it.aoService.id == td.aoService.id }
        // Send Event to Event Q of TP AO
        val eventBuffer = EventBuffer(eventId = TpEvent.SEND, buffer = data, srvDesc = currentTd)
        commonAO!!.sendEvent(currentTd!!.aoService.id, eventBuffer)
        return 0
    }

    fun tpClose(td: SrvDesc):Int {
        // find td by user ao id
        val currentTd = commonAO!!.transportAO.tpDescArr.find { it.aoService.id == td.aoService.id }
        // Send Event to Event Q of TP AO
        val eventBuffer = EventBuffer(eventId = TpEvent.CLOSE, srvDesc = currentTd)
        commonAO!!.sendEvent(currentTd!!.aoService.id, eventBuffer)

        return 0
    }

    fun tpReset(td: SrvDesc):Int {
        // find td by user ao id
        val currentTd = commonAO!!.transportAO.tpDescArr.find { it.aoService.id == td.aoService.id }
        // Send Event to Event Q of TP AO
        val eventBuffer = EventBuffer(eventId = TpEvent.RESET, srvDesc = currentTd)
        commonAO!!.sendEvent(currentTd!!.aoService.id, eventBuffer)

        return 0
    }
}

data class SocketDescriptor (
    var srcAddr: ByteArray? = byteArrayOf(),
    var srcPort: Byte? = null,
    var destAddr: ByteArray? = byteArrayOf(),
    var destPort: Byte? = null,
    var remoteDevice: String = ""
)