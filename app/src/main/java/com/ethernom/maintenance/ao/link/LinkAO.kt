package com.ethernom.maintenance.ao.link

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ethernom.maintenance.MainApplication
import com.ethernom.maintenance.ao.transport.TpEvent
import com.ethernom.maintenance.ao.*
import com.ethernom.maintenance.ao.link.ble.BLEDialogActivity
import com.ethernom.maintenance.ao.link.ble.L2CAP
import com.ethernom.maintenance.ui.commonAO
import com.ethernom.maintenance.utils.Utils
import kotlin.collections.ArrayList

@RequiresApi(Build.VERSION_CODES.Q)
class LinkAO(ctx: Context) {
    private val tag: String = javaClass.simpleName
    private val intent = Intent(BROADCAST_INTERRUPT)

    /** Common AO Path */
    private var linkFsm = arrayOf(
        FSM( currentState = LinkState.INIT,       event = AoEvent.COMMON_INIT,          nextState = LinkState.OPEN,       af = ::afNothing ),
        FSM( currentState = LinkState.OPEN,       event = LinkEvent.OPEN,               nextState = LinkState.OPEN,       af = ::afNothing ),
        FSM( currentState = LinkState.OPEN,       event = LinkEvent.DISCOVER,           nextState = LinkState.DISCOVERY,  af = ::afDiscover ),
        FSM( currentState = LinkState.OPEN,       event = LinkEvent.BLE_ON,             nextState = LinkState.DISCOVERY,  af = ::afBleOn ),

        FSM( currentState = LinkState.DISCOVERY,  event = LinkEvent.RESET,              nextState = LinkState.OPEN,       af = ::afReset ),

        FSM( currentState = LinkState.DISCOVERY,  event = LinkEvent.ADV_IND,            nextState = LinkState.DISCOVERY,  af = ::afAdvertiseIndicate ),
        FSM( currentState = LinkState.DISCOVERY,  event = LinkEvent.SELECT,             nextState = LinkState.CONNECTING, af = ::afSelect ),
        FSM( currentState = LinkState.CONNECTING, event = LinkEvent.CONNECTION_TIMEOUT, nextState = LinkState.DISCOVERY,  af = ::afConnectionTimeout ),
        FSM( currentState = LinkState.CONNECTING, event = LinkEvent.CONNECTION_RESPOND, nextState = LinkState.DATA,       af = ::afConnectionRespond),
        FSM( currentState = LinkState.DATA,       event = LinkEvent.SEND,               nextState = LinkState.DATA,       af = ::afSend ),
        FSM( currentState = LinkState.DATA,       event = LinkEvent.LL_RECEIVE_CB,      nextState = LinkState.DATA,       af = ::afReceive ),
        FSM( currentState = LinkState.DATA,       event = LinkEvent.CLOSE,              nextState = LinkState.OPEN,       af = ::afClose ),

        FSM( currentState = LinkState.DATA,  event = LinkEvent.RESET,                   nextState = LinkState.OPEN,       af = ::afReset ),
        FSM( currentState = AO_TABLE_END,         event = INVALID_EVENT,                nextState = AO_TABLE_END,         af = ::afNothing ),
    )

    private var eventQ1 = EventQ(
        buffer = arrayOf(
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff)
        ), full = 0,
        consumerIdx = 0,
        producerIdx =0)

    private var eventQ2 = EventQ(
        buffer = arrayOf(
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff),
            EventBuffer(eventId = 0xff)
        ), full = 0,
        consumerIdx = 0,
        producerIdx =0)

    /** Initialize all acb_t here */
    val linkAcb1 = ACB(
        id = AoId.AO_LL1_ID,
        currentState = LinkState.INIT,
        eventQ = eventQ1 ,
        fsm = linkFsm,
        //[LL, TP, ...]
        srvDescriptors = arrayOf(null, null)
    )

    val linkAcb2 = ACB(
        id = AoId.AO_LL2_ID,
        currentState = LinkState.INIT,
        eventQ = eventQ2 ,
        fsm = linkFsm,
        //[LL, TP, ...]
        srvDescriptors = arrayOf(null, null)
    )

    /** Link Descriptor specific data declared in this variable */
    var ldSrv1 = LinkDescriptor(deviceName = "", mfgSN = "", uuid = "", type = LinkType.BLE, mtu = 0)
    var ldSrv2 = LinkDescriptor(deviceName = "", mfgSN = "", uuid = "", type = LinkType.BLE, mtu = 0)

    /** Link Descriptor declared in this variable; */
    var ld1 = SrvDesc(aoUser = null, aoService = linkAcb1, ld = ldSrv1)
    var ld2 = SrvDesc(aoUser = null, aoService = linkAcb2, ld = ldSrv2)

    /** Link Descriptors Array {service descriptor} declared in this list; */
    var linkDescArr: ArrayList<SrvDesc> = arrayListOf(ld1, ld2)


    /**  Action Function Path */
    private var context = ctx
    @RequiresApi(Build.VERSION_CODES.Q)
    private var bluetooth = L2CAP(ctx)
    private var connectReqTimeOut = false
    private var connecting = false
    private var fifoData:ArrayList<ByteArray> = ArrayList()
    private var remoteDevice:String = ""
    private var discoveredList = ArrayList<LinkDescriptor>()

    /**
     **********************************************************************
     * Action Function
     **********************************************************************
     */

    /** Do nothing Action Function */
    private fun afNothing(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "Link Init call ${acb.id}")
        return true
    }

    private fun afDiscover(acb: ACB, buffer: EventBuffer): Boolean{
        Log.d(tag, "discover call ${acb.id}")
        //  find link AO based on Ao id in link descriptor array
        val ld = linkDescArr.find { it.aoService.id == acb.id }

        if(!Utils.isBluetoothEnable) {
            val intent = Intent(context, BLEDialogActivity::class.java).putExtra("BLE_STATUS", true).putExtra("AO_ID", acb.id).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return false
        }
        remoteDevice = buffer.srvDesc!!.ld!!.mfgSN
        bluetooth.stopScan()
        bluetooth.scan(ld!!) // Start To Discover again
        return true
    }

    private fun afBleOn(acb: ACB, buffer: EventBuffer): Boolean {
        //  find link AO based on Ao id in link descriptor array
        val ld = linkDescArr.find { it.aoService.id == acb.id }
        bluetooth.stopScan()
        bluetooth.scan(ld!!) // Start To Discover again
        return true
    }

    private fun afAdvertiseIndicate(acb: ACB, buffer: EventBuffer):Boolean {
        Log.d(tag, "scanCallback call ")
        //  find link AO based on Ao id in link descriptor array
        val ld = linkDescArr.find { it.aoService.id == acb.id }
        val ldSrv = ld!!.ld!!

        // Check if match remote device use serial number base
        Log.d(tag, "Matched ${buffer.srvDesc!!.ld!!.mfgSN.lowercase()} == ${remoteDevice.lowercase()}")

        if (remoteDevice == "") {
            Log.d(tag, "SN NULL ${buffer.srvDesc!!.ld!!.ble!!.address}")

            val active = discoveredList.find { it.ble?.address == buffer.srvDesc!!.ld!!.ble!!.address }
            if(active != null) {
                Log.d(tag, "Data duplicated")
                return false
            }
            val discoveredPkt = LinkDescriptor(
                type = buffer.srvDesc!!.ld!!.type,
                deviceName = buffer.srvDesc!!.ld!!.deviceName,
                mfgSN = buffer.srvDesc!!.ld!!.mfgSN,
                uuid = buffer.srvDesc!!.ld!!.uuid,
                ble = buffer.srvDesc!!.ld!!.ble,
                mtu = getMTU(buffer.srvDesc!!.ld!!.type)
            )

            Log.d("TAG", "BLE LINK EVENT ${discoveredPkt.ble}")
            discoveredList.add(discoveredPkt)
            // Send Discovered event to TP AO
            val ef = EventBuffer(eventId = TpEvent.DISCOVERED, advPkt = discoveredPkt)
            commonAO!!.sendEvent(ld.aoUser!!.id, ef)
            return true
        }

        if (buffer.srvDesc!!.ld!!.mfgSN.lowercase() != remoteDevice.lowercase() && !connecting) return false
        connecting = true
        Log.d(tag, "SN NOT NULL")

        // Update Link descriptor specific data
        ldSrv.type = buffer.srvDesc!!.ld!!.type
        ldSrv.deviceName = buffer.srvDesc!!.ld!!.deviceName
        ldSrv.uuid = buffer.srvDesc!!.ld!!.uuid
        ldSrv.mfgSN = buffer.srvDesc!!.ld!!.mfgSN
        ldSrv.ble = buffer.srvDesc!!.ld!!.ble
        ldSrv.mtu = getMTU(buffer.srvDesc!!.ld!!.type)

        // Send Select event to Own AO
        val ef = EventBuffer(eventId = LinkEvent.SELECT, srvDesc = ld)
        commonAO!!.sendEvent(ld.aoService.id, ef)

        return true
    }

    private fun afSelect(acb: ACB, data: EventBuffer):Boolean {
        Log.d(tag, "select call")
        if(!Utils.isBluetoothEnable) {
            val intent = Intent(context, BLEDialogActivity::class.java).putExtra("BLE_STATUS", false).putExtra("AO_ID", acb.id).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return false
        }

        //  find link AO based on Ao id in link descriptor array
        val ld = linkDescArr.find { it.aoService.id == acb.id }

        bluetooth.stopScan() // Stop Discovery Before connect
        discoveredList.clear() // Clear discover list
        remoteDevice = ""
        bluetooth.connect(ld!!) // Connect to gatt server
        connectReqTimeOut = true
        @Suppress("DEPRECATION")
        Handler().postDelayed({
            if(connectReqTimeOut) {
                // Send Event to Event Q
                val ef = EventBuffer(eventId = LinkEvent.CONNECTION_TIMEOUT, srvDesc = ld)
                commonAO!!.sendEvent(ld.aoService.id, ef)

                // send broadcast interrupt
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            }
        }, 15000)

        return true
    }

    private fun afConnectionTimeout(acb: ACB, buffer: EventBuffer):Boolean {
        connecting = false
        Log.d(tag, "connectionTimeout call")
        //  find link AO based on Ao id in link descriptor array
        val ld = linkDescArr.find { it.aoService.id == acb.id }

        // Send event to User AO for indicate connection timeout
        val ef = EventBuffer(eventId = TpEvent.CONN_TO, srvDesc = ld)
        commonAO!!.sendEvent(ld!!.aoUser?.id!!, ef)

        return true
    }

    private fun afConnectionRespond(acb: ACB, data: EventBuffer):Boolean {
        connectReqTimeOut = false
        connecting = false
        Log.d(tag, "connectionRespond call")
        //  find link AO based on Ao id in link descriptor array
        val ld = linkDescArr.find { it.aoService.id == acb.id }
        val ldSrv = ld!!.ld!!

        // update MTU in device
        ldSrv.mtu = data.srvDesc!!.ld!!.mtu

        Log.d("TAG", "afConnectionRespond ${ld.ld!!.deviceName}")
        // Send event to User AO for indicate connection available
        val ef = EventBuffer(eventId = TpEvent.CONN_AVB, srvDesc = ld)
        commonAO!!.sendEvent(ld.aoUser?.id!!, ef)

        return true
    }

    private fun afSend(acb: ACB, buffer: EventBuffer):Boolean {
        Log.d(tag, "send call")
        //  find link AO based on Ao id in link descriptor array
        val ld = linkDescArr.find { it.aoService.id == acb.id }

        fifo(ld!!, buffer.buffer!!)
        return true
    }

    private fun afReceive(acb: ACB, buffer: EventBuffer):Boolean {
        Log.d(tag, "receive call")
        val data = buffer.buffer
        //  find link AO based on Ao id in link descriptor array
        val ld = linkDescArr.find { it.aoService.id == acb.id }

        // Send event to Transport layer for indicate data
        val ef = EventBuffer(eventId = TpEvent.LL_CB_DATA, srvDesc = ld, buffer = data)
        commonAO!!.sendEvent(ld!!.aoUser?.id!!, ef)

        return true
    }

    private fun afClose(acb: ACB, buffer: EventBuffer):Boolean {
        Log.d("TAG", "LL afClose call")
        // Unbind descriptor from both user and service AO
        val ld = linkDescArr.find { it.aoService.id == acb.id }
        ld!!.aoService.srvDescriptors[DescIdx.LL_DESC] = null
        ld.aoUser!!.srvDescriptors[DescIdx.LL_DESC] = null

        // Clear ao_user in link descriptor
        ld.aoUser = null
        // Disconnect l2cap
        bluetooth.disconnect()

        return true
    }

    private fun afReset(acb: ACB, buffer: EventBuffer):Boolean {
        Log.d("TAG", "LL afReset call")
        // Unbind descriptor from both user and service AO
        val ld = linkDescArr.find { it.aoService.id == acb.id }
        ld!!.aoService.srvDescriptors[DescIdx.LL_DESC] = null
        ld.aoUser!!.srvDescriptors[DescIdx.LL_DESC] = null

        // Clear ao_user in link descriptor
        ld.aoUser = null
        // Stop Discover
        bluetooth.stopScan()
        // Disconnect l2cap
        bluetooth.disconnect()
        return true
    }

    /**
     **********************************************************************
     * Helper Function
     **********************************************************************
     */

    private fun fifo(ld: SrvDesc, packets: ByteArray) {
        if(fifoData.size == 0) {
            fifoData.add(packets)
            sendQueue(ld)
        } else {
            fifoData.add(packets)
        }
    }

    private fun sendQueue(ld: SrvDesc) {
        bluetooth.send(ld, fifoData[0],
            object : WriteResponse {
                override fun onWriteCallBack(buffer: ByteArray?) {
                    Log.d("LinkLayer", "onWriteCallBack ")
                    if(fifoData.size > 1) {
                        fifoData.removeAt(0)
                        sendQueue(ld)
                    } else {
                        fifoData.clear()
                    }
                }
            }) // Call LLsend() Link Layer
    }

    private fun getMTU(type:Byte): Int {
        // Map mtu data with Link Type
        return when(type) {
            LinkType.BLE -> MTU.GATT_MTU
            LinkType.USB -> MTU.USB_MTU
            LinkType.NFC -> MTU.NFC_MTU
            else -> 0
        }
    }
}

object LinkState {
    const val INIT = 0
    const val OPEN = 1000
    const val DISCOVERY = 2000
    const val CONNECTING = 3000
    const val DATA  = 4000
}

object LinkEvent {
    const val OPEN = 24
    const val DISCOVER = 25
    const val BLE_ON = 26
    const val ADV_IND = 27
    const val LL_RECEIVE_CB = 28
    const val SELECT = 29
    const val CONNECTION_TIMEOUT = 30
    const val CONNECTION_RESPOND = 31
    const val SEND = 32
    const val CLOSE = 33
    const val RESET = 34
}
object LinkType {
    const val NFC = 0x01.toByte()
    const val BLE = 0x02.toByte()
    const val USB = 0x03.toByte()
}

object MTU {
    const val GATT_MTU = 512
    const val USB_MTU = 10000
    const val NFC_MTU = 200
}

interface WriteResponse {
    fun onWriteCallBack(buffer: ByteArray?)
}

