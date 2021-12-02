package com.ethernom.maintenance.ao.link

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.ethernom.maintenance.MainApplication
import com.ethernom.maintenance.ao.ACB
import com.ethernom.maintenance.ao.DescIdx
import com.ethernom.maintenance.ao.EventBuffer
import com.ethernom.maintenance.ao.SrvDesc
import com.ethernom.maintenance.ui.commonAO

class LinkAPI(ctx:Context) {
    private val tag: String = javaClass.simpleName

    fun llOpen(userAO: ACB): SrvDesc? {
        val freeLd = commonAO!!.linkAO.linkDescArr.find { it.aoUser == null } ?: return null
        Log.d(tag, "found link")
        // bind ao_user & ao_link
        freeLd.aoUser = userAO // bind to ao user to free link descriptor
        // aoService = aoLink ACB; initialized during static initialization
        // init ACB of user AO   to point to LD
        freeLd.aoUser!!.srvDescriptors[DescIdx.LL_DESC] = freeLd
        freeLd.aoService.srvDescriptors[DescIdx.LL_DESC] = freeLd

        // Send Open Event to own Event Queue
        val ef = EventBuffer(eventId = LinkEvent.OPEN, srvDesc = freeLd)
        commonAO!!.sendEvent(freeLd.aoService.id, ef)

        return freeLd
    }

    fun llDiscover(ld: SrvDesc, remoteDevice:String):Int? {
        val currentLd = commonAO!!.linkAO.linkDescArr.find { it.aoService.id == ld.aoService.id } ?: return null
        // Update remote device to Link Descriptor
        currentLd.ld!!.mfgSN = remoteDevice
        Log.d(tag, "Remote Device $remoteDevice")
        val ef = EventBuffer(eventId = LinkEvent.DISCOVER, srvDesc = currentLd)
        commonAO!!.sendEvent(ld.aoService.id, ef)
        return 0
    }

    fun llSelect(ld: SrvDesc, remoteDevice: LinkDescriptor):Int? {
        val currentLd = commonAO!!.linkAO.linkDescArr.find { it.aoService.id == ld.aoService.id } ?: return null
        currentLd.ld = remoteDevice
        // Send Select event to Own AO
        val ef = EventBuffer(eventId = LinkEvent.SELECT, srvDesc = currentLd)
        commonAO!!.sendEvent(ld.aoService.id, ef)
        return 0
    }

    fun llSend(ld: SrvDesc, data:ByteArray): Int {
        val ef = EventBuffer(eventId = LinkEvent.SEND, srvDesc = ld, buffer = data)
        commonAO!!.sendEvent(ld.aoService.id, ef)
        return 0
    }

    fun llClose(ld: SrvDesc):Int {
        val ef = EventBuffer(eventId = LinkEvent.CLOSE, srvDesc = ld)
        commonAO!!.sendEvent(ld.aoService.id, ef)
        return 0
    }

    fun llReset(ld: SrvDesc){
        val ef = EventBuffer(eventId = LinkEvent.RESET, srvDesc = ld)
        commonAO!!.sendEvent(ld.aoService.id, ef)
    }
}

data class LinkDescriptor(
    var deviceName:String = "",
    var mfgSN:String = "", // Manufacturer Serial Number
    var uuid: String = "",
    var type: Byte = 0, // Link Type Can be [BLE, NFC, USB]
    var mtu: Int = 0, // Maximum transfer unit
    // Link Specification
    var ble: BluetoothDevice? = null
)