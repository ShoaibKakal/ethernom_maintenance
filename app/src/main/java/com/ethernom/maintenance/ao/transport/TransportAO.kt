package com.ethernom.maintenance.ao.transport

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import com.ethernom.maintenance.MainApplication
import com.ethernom.maintenance.ao.link.LinkAPI
import com.ethernom.maintenance.ao.link.LinkType
import com.ethernom.maintenance.ao.*
import com.ethernom.maintenance.ui.commonAO
import com.ethernom.maintenance.utils.ETH_TP_HEADER_SIZE
import com.ethernom.maintenance.utils.TransportCmd
import com.ethernom.maintenance.utils.Utils
import com.ethernom.maintenance.utils.hexa
import java.util.*
import kotlin.collections.ArrayList

@Suppress("DEPRECATION")
class TransportAO (ctx:Context){
    private val tag: String = javaClass.simpleName
    private val intent = Intent(BROADCAST_INTERRUPT)

    /** Common AO Variable */
    private var tpFsm = arrayOf(
        FSM( currentState = TpState.INIT,       event = AoEvent.COMMON_INIT,          nextState = TpState.OPEN,     af = ::afNothing ),
        FSM( currentState = TpState.OPEN,       event = TpEvent.OPEN,                 nextState = TpState.OPEN,     af = ::afOpen ),

        FSM( currentState = TpState.OPEN,       event = TpEvent.RESET,                 nextState = TpState.OPEN,     af = ::afReset),

        FSM( currentState = TpState.OPEN,       event = TpEvent.DISCOVERED,           nextState = TpState.OPEN,     af = ::afDiscovered),
        FSM( currentState = TpState.OPEN,       event = TpEvent.SELECT,               nextState = TpState.OPEN,     af = ::afSelect ),
        FSM( currentState = TpState.OPEN,       event = TpEvent.OPEN,                 nextState = TpState.OPEN,     af = ::afOpen ),
        FSM( currentState = TpState.OPEN,       event = TpEvent.CONN_AVB,             nextState = TpState.LISTEN,   af = ::afConnAvailable ),
        FSM( currentState = TpState.OPEN,       event = TpEvent.CONN_TO,              nextState = TpState.LISTEN,   af = ::afConnTimeout ),
        FSM( currentState = TpState.LISTEN,     event = TpEvent.LL_CB_DATA,           nextState = TpState.CONFIRM,  af = ::afConnectionRequest ),
        FSM( currentState = TpState.CONFIRM,    event = TpEvent.LL_CB_DATA,           nextState = TpState.DATA,     af = ::afConnectionConfirm ),
        FSM( currentState = TpState.DATA,       event = TpEvent.SEND,                 nextState = TpState.DATA,     af = ::afSend ),
        FSM( currentState = TpState.DATA,       event = TpEvent.LL_CB_DATA,           nextState = TpState.DATA,     af = ::afReceive),
        FSM( currentState = TpState.DATA,       event = TpEvent.DISCONNECT,           nextState = TpState.DATA,   af = ::afDisconnectRequest ),
        FSM( currentState = TpState.DATA,       event = TpEvent.CLOSE,                nextState = TpState.OPEN,     af = ::afClose ),
        FSM( currentState = TpState.DATA,       event = TpEvent.RESET,                 nextState = TpState.OPEN,     af = ::afReset),
        FSM( currentState = AO_TABLE_END,       event = INVALID_EVENT,                nextState = AO_TABLE_END,     af = ::afNothing ),
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

    val tpAcb1 = ACB(
        id = AoId.AO_TP1_ID,
        currentState = TpState.INIT,
        eventQ = eventQ1,
        fsm = tpFsm,
        //[LL, TP, ...]
        srvDescriptors = arrayOf(null, null)
    )

    val tpAcb2 = ACB(
        id = AoId.AO_TP2_ID,
        currentState = TpState.INIT,
        eventQ = eventQ2,
        fsm = tpFsm,
        //[LL, TP, ...]
        srvDescriptors = arrayOf(null, null)
    )

    /** Transport Descriptor specific data declared in this variable */
    var sd1 = SocketDescriptor(srcPort = null, srcAddr = byteArrayOf(), destPort = null, destAddr = byteArrayOf())
    var sd2 = SocketDescriptor(srcPort = null, srcAddr = byteArrayOf(), destPort = null, destAddr = byteArrayOf())

    /** Transport Descriptor declared in this variable; */
    var td1 = SrvDesc(aoUser = null, aoService = tpAcb1, sd = sd1 )
    var td2 = SrvDesc(aoUser = null, aoService = tpAcb2, sd = sd2 )

    /** Transport Descriptors Array {service descriptor} declared in this list; */
    var tpDescArr: ArrayList<SrvDesc> = arrayListOf(td1, td2)


    /** TP AO Variable */
    private var context = ctx
    private var buffer: ArrayList<Byte> = ArrayList()
    private var bufferSize = 0
    private var linkAPI = LinkAPI(context)

    /**
     **********************************************************************
     * Action Function
     **********************************************************************
     */

    /** Do nothing Action Function */
    private fun afNothing(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "Transport Init call")
        return true
    }

    private fun afOpen(acb: ACB, data: EventBuffer): Boolean {
        Log.d(tag, "open call")
        // Get td from transport Descriptor array
        val td = tpDescArr.find { it.aoService.id == acb.id }

        val ld = linkAPI.llOpen(acb)

        td!!.aoService.srvDescriptors[DescIdx.LL_DESC] = ld

        linkAPI.llDiscover(ld!!, data.srvDesc!!.sd!!.remoteDevice)

        return true
    }

    private fun afDiscovered(acb: ACB, data: EventBuffer): Boolean {
        // Get td from transport Descriptor array
        val td = tpDescArr.find { it.aoService.id == acb.id }
        // Send Event to App AO
        val ef = EventBuffer(eventId = AoEvent.TP_DISCOVERED, advPkt = data.advPkt)
        commonAO!!.sendEvent(aoId = td!!.aoUser!!.id, buff = ef)
        return true
    }

    private fun afSelect(acb: ACB, data: EventBuffer): Boolean {
        // Get td from transport Descriptor array
        val td = tpDescArr.find { it.aoService.id == acb.id }
        linkAPI.llSelect(td!!.aoService.srvDescriptors[DescIdx.LL_DESC]!!, data.advPkt!!)
        return true
    }

    private fun afConnAvailable(acb: ACB, data: EventBuffer): Boolean {
        Log.d(tag, "connect available call")
        val ld = data.srvDesc!!.ld
        val td = tpDescArr.find { it.aoService.id == acb.id }
        td!!.ld = ld

        return true
    }

    private fun afConnTimeout(acb: ACB, data: EventBuffer): Boolean {
        Log.d(tag, "connection time out call")
        // Get td from transport Descriptor array
        val td = tpDescArr.find { it.aoService.id == acb.id }
        // Send Event to App AO
        val ef = EventBuffer(eventId = AoEvent.TP_CONN_TO, srvDesc = td)
        commonAO!!.sendEvent(aoId = td!!.aoUser!!.id, buff = ef)

        return true
    }

    /*
      Data format
      +-----------------------------------------------++------------------------+
      |           Transport Header                    ||  Application Payload   |
      +----+----+------+-----+------------+-----+-----++--------+---------------+
          | SP | DP | Ctrl | Inf | Length(2)  | Cmd | CKS || PL Cmd |     Payload   |
      +----+----+------+-----+------------+-----+-----++--------+---------------+
    */
    private fun afConnectionRequest(acb: ACB, data: EventBuffer): Boolean {
        Log.d(tag, "connectionRequest call")
        // Re assemble data packets
        val res = checkHeaderAndReassemblePackets(data.buffer!!) // Check header & reassemble
        if (!res) return false

        // Check data buffer is not connection request return false
        if (buffer[6] != TransportCmd.C2H_CONNECT_REQ) return false
        // Get td from transport Descriptor array
        val td = tpDescArr.find { it.aoService.id == acb.id }
        val sd = td!!.sd
        val ipAddress = Utils.getIPAddressV4() // get IP Address v4 on mobile
        // Update source port and destination port in socket descriptor
        sd!!.srcAddr = ipAddress
        // +1 of digit number 2 for assign to destination address
        ipAddress[2] = (ipAddress[2].toInt() + 1).toByte()
        sd.destAddr = ipAddress
        // Construct connection accept payload
        val connectionAccept = sd.srcAddr!! + sd.destAddr!!
        // Construct transport data & Send Event to Event Q Link AO
        llSend(td, TransportCmd.H2C_CONNECT_RSP, false, connectionAccept)

        // Send Event to App AO
        val ef = EventBuffer(eventId = AoEvent.TP_CONNECTING, srvDesc = td)
        commonAO!!.sendEvent(aoId = td.aoUser!!.id, buff = ef)

        clearBuffer()
        return true
    }

    private fun afConnectionConfirm(acb: ACB, data: EventBuffer): Boolean {
        Log.d(tag, "connectionConfirm call")
        // Re assemble data packets
        val res = checkHeaderAndReassemblePackets(data.buffer!!) // Check header & reassemble
        if (!res) return false

        // Check data buffer is not connection confirm return false
        if (buffer[6] != TransportCmd.C2H_CONNECT_CFM) return false
        // Get td from transport Descriptor array
        val td = tpDescArr.find { it.aoService.id == acb.id }
        // Send Event to App AO

        // Log.d("TAG", "afConnectionConfirm ${td!!.ld!!.deviceName}")

        val ef = EventBuffer(eventId = AoEvent.TP_CONN_CRM, srvDesc = td)
        commonAO!!.sendEvent(aoId = td!!.aoUser!!.id, buff = ef)
        // Start Keep Alive
        startKeepAlive(td)
        timeout = true

        clearBuffer()
        return true
    }


    private fun afDisconnectRequest(acb: ACB, data: EventBuffer): Boolean {
        Log.d(tag, "disconnectRequest call")
        val td = tpDescArr.find { it.aoService.id == acb.id }

        // Send Event to Own AO
        val ef1 = EventBuffer(eventId = TpEvent.CLOSE, srvDesc =  td)
        commonAO!!.sendEvent(aoId = acb.id, buff = ef1)

        // Send Event to App AO
        val ef = EventBuffer(eventId = AoEvent.TP_DISC, srvDesc =  td)
        commonAO!!.sendEvent(aoId = td!!.aoUser!!.id, buff = ef)


        return true
    }

    private fun afSend(acb: ACB, data: EventBuffer): Boolean {
        Log.d(tag, "send call")
        // Get td from transport Descriptor array
        val td = tpDescArr.find { it.aoService.id == acb.id }
        Log.d(tag, "sd found")
        // Construct transport data & Send Event to Event Q Link AO
        llSend(td!!, TransportCmd.H2C_DATA, false, data.buffer!!)
        return true
    }

    private fun afReceive(acb: ACB, data: EventBuffer): Boolean {
        Log.d(tag, "receive call")
        // Re assemble data packets
        val res = checkHeaderAndReassemblePackets(data.buffer!!) // Check header & reassemble
        if (!res) return false

        // Get td from transport Descriptor array
        val td = tpDescArr.find { it.aoService.id == acb.id }
        // Check if Disconnect request
        if(buffer[6] == TransportCmd.C2H_DISCONNECT_REQ) {
            // Send Event to Event Q of TP AO it self
            val ef = EventBuffer(eventId = TpEvent.DISCONNECT, srvDesc = td)
            commonAO!!.sendEvent(aoId = td!!.aoService.id, buff = ef)
            clearBuffer()
            return true
        }

        // Check data buffer is not data return false
        if (buffer[6] != TransportCmd.H2C_DATA) return false

        //  Remove header before call back to app
        val payload = buffer.toByteArray().copyOfRange(ETH_TP_HEADER_SIZE, buffer.size)
        // Send Event to Event Q of App AO
        val ef = EventBuffer(eventId = AoEvent.TP_DATA_REC, buffer = payload, srvDesc = td)
        commonAO!!.sendEvent(aoId = td!!.aoUser!!.id, buff = ef)
        clearBuffer()
        return true
    }

    private fun afClose (acb: ACB, data: EventBuffer): Boolean {
        Log.d(tag, "close call")
        // reset keep alive timeout
        timeout = false
        // Get td from transport Descriptor array
        val td = tpDescArr.find { it.aoService.id == acb.id }

        // Close Link AO
        linkAPI.llClose(td!!.aoService.srvDescriptors[DescIdx.LL_DESC]!!)

        // Unbind descriptor from both user and service AO
        td.aoService.srvDescriptors[DescIdx.TP_DESC] = null
        td.aoUser!!.srvDescriptors[DescIdx.TP_DESC] = null

        // Clear ao_user in link descriptor
        td.aoUser = null
        return true
    }

    private fun afReset (acb: ACB, data: EventBuffer): Boolean {
        Log.d(tag, "reset call")
        timeout = false
        // Get td from transport Descriptor array
        val td = tpDescArr.find { it.aoService.id == acb.id }

        // Reset Link AO
        linkAPI.llReset(td!!.aoService.srvDescriptors[DescIdx.LL_DESC]!!)

        // Unbind descriptor from both user and service AO
        td.aoService.srvDescriptors[DescIdx.TP_DESC] = null
        td.aoUser!!.srvDescriptors[DescIdx.TP_DESC] = null

        // Clear ao_user in link descriptor
        td.aoUser = null
        return true
    }

    /**
     **********************************************************************
     * Helper Function
     **********************************************************************
     */

    private fun llSend(td: SrvDesc, cmd: Byte, enc: Boolean, payload : ByteArray = ByteArray(0)) {
        val control = setControl(true, enc) // set control by encrypt status
        val data = Helpers.makeTransportData(
            td.sd!!.srcPort!!,
            td.sd!!.destPort!!,
            control,
            LinkType.BLE,
            cmd,
            payload
        ) // Construct data transport
        val packets = fragmentPackets(data, td.aoService.srvDescriptors[DescIdx.LL_DESC]!!.ld!!.mtu)  // Fragment Packet 2K
        for(packet in packets) {
            Log.d(tag, "PKT ${packet.hexa()}")
            // Call Link API
            linkAPI.llSend(td.aoService.srvDescriptors[DescIdx.LL_DESC]!!, packet)
        }
    }

    private fun fragmentPackets(data: ByteArray, maxSize: Int): ArrayList<ByteArray> {
        val result = arrayListOf<ByteArray>()
        for (x in data.indices step maxSize) {
            result.add(data.copyOfRange(x, Math.min(data.size, x + maxSize)))
        }
        return result
    }

    private fun checkHeaderAndReassemblePackets(packet: ByteArray): Boolean {
        if (bufferSize == 0 && packet.size >= ETH_TP_HEADER_SIZE) { // check first packet
            // calculate checksum of header
            val checksum = Helpers.getCheckSum(packet)
            // Check checksum
            if (packet[7] != checksum) {
                return false
            }
            bufferSize = Helpers.getPayloadLength(packet[4], packet[5]) // index 4 & 5 are length of payload
            println("buffer size $bufferSize ")
        }
        println("+++++++reassemble+++++++")
        println("packet <- ${packet.hexa()} ")
        buffer.addAll(packet.toList())
        println("buffer size ${buffer.size - ETH_TP_HEADER_SIZE} ")
        if (bufferSize == buffer.size - ETH_TP_HEADER_SIZE) {
            return true
        }
        return false
    }

    private fun clearBuffer() {
        buffer.clear() // clear buffer
        bufferSize = 0
    }

    @SuppressLint("NewApi")
    private fun setControl(bit0: Boolean, bit7: Boolean): Byte {
        val bitSet = BitSet()
        bitSet.set(0, bit0) // Set Bit 0 to new format bit
        bitSet.set(7, bit7) // Set Bit 7 to encryption bit
        return bitSet.toByteArray()[0]
    }

    /** Keep Alive */
    var kaTd: SrvDesc? = null
    var timeout = true
    val handler = Handler()
    val runnable: Runnable = Runnable {
        Log.d(tag, "timeout")
        if(timeout) {
            // Construct transport data & Send Event to Event Q Link AO
            llSend(kaTd!!, TransportCmd.H2C_KA, false, byteArrayOf())
            commonAO!!.aoRunScheduler()
            restart5SecTimeOut()
        }

    }

    private fun startKeepAlive(td: SrvDesc) {
        kaTd = td
        Log.d(tag, "startKeepAlive")
        // Construct transport data & Send Event to Event Q Link AO
        llSend(td, TransportCmd.H2C_KA, false, byteArrayOf())
        // write inside onCreate method
        handler.postDelayed(runnable,5000)
    }

    private fun restart5SecTimeOut() {
        handler.postDelayed(runnable, 5000)
    }

}

object TpState {
    const val INIT = 0
    const val OPEN = 1000
    const val LISTEN = 2000
    const val CONFIRM = 3000
    const val DATA = 4000
}

object TpEvent {
    const val OPEN = 24
    const val DISCOVERED = 25
    const val SELECT = 26
    const val CONN_AVB = 27
    const val LL_CB_DATA = 28
    const val CONN_TO = 29
    const val SEND = 30
    const val DISCONNECT = 31
    const val CLOSE = 32
    const val RESET = 33
}