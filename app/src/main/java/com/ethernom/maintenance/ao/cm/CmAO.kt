package com.ethernom.maintenance.ao.cm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ethernom.maintenance.ao.transport.TransportAPI
import com.ethernom.maintenance.ao.*
import com.ethernom.maintenance.ao.cm.repository.LoginRepository
import com.ethernom.maintenance.ao.cm.repository.UnregisterRepository
import com.ethernom.maintenance.ao.link.LinkDescriptor
import com.ethernom.maintenance.model.LoginRequestBody
import com.ethernom.maintenance.model.LoginResponse
import com.ethernom.maintenance.model.UnregisterRequestBody
import com.ethernom.maintenance.model.UnregisterResponse
import com.ethernom.maintenance.ui.commonAO
import com.ethernom.maintenance.utils.AppConstant.BLUETOOTH_DEVICE
import com.ethernom.maintenance.utils.AppConstant.CAPSULE_VERSION
import com.ethernom.maintenance.utils.AppConstant.DEVICE_ADVERTISE
import com.ethernom.maintenance.utils.AppConstant.DEVICE_NAME
import com.ethernom.maintenance.utils.AppConstant.DEVICE_READY
import com.ethernom.maintenance.utils.AppConstant.MANUFAC_SERIAL_NUMBER
import com.ethernom.maintenance.utils.AppConstant.MTU
import com.ethernom.maintenance.utils.AppConstant.TYPE
import com.ethernom.maintenance.utils.AppConstant.UUID
import com.ethernom.maintenance.utils.Port
import com.ethernom.maintenance.utils.hexa

class CmAO(ctx: Context) {
    private val tag: String = javaClass.simpleName
    private val context: Context = ctx

    /* AO variable */
    private var td: SrvDesc? = null
    private var transportAPI = TransportAPI(context)

    /** Common AO Variable */
    private var cmFsm = arrayOf(
        FSM(currentState = CmState.INIT, event = AoEvent.COMMON_INIT, nextState = CmState.READY, af = ::cmInit),
        // Discovery
        FSM(currentState = CmState.READY, event = CmEvent.TP_DISCOVERY, nextState = CmState.DISCOVERY, af = ::af2_1Cm),
        FSM(currentState = CmState.DISCOVERY, event = AoEvent.TP_DISCOVERED, nextState = CmState.DISCOVERY, af = ::af2_2Cm),
        FSM(currentState = CmState.DISCOVERY, event = CmEvent.TP_SELECT, nextState = CmState.TP_CONNECT, af = ::af2_3Cm),
        FSM(currentState = CmState.DISCOVERY, event = CmEvent.DISCOVERY_RESET, nextState = CmState.READY, af = ::af13Cm),

        // TP
        FSM(currentState = CmState.READY, event = CmEvent.TP_CONNECT, nextState = CmState.TP_CONNECT, af = ::af2Cm),
        FSM(currentState = CmState.TP_CONNECT, event = AoEvent.TP_CONNECTING, nextState = CmState.TP_CONNECT, af = ::af3Cm),
        // Reset
        FSM(currentState = CmState.TP_CONNECT, event = CmEvent.RESET, nextState = CmState.READY, af = ::af13Cm),

        FSM(currentState = CmState.TP_CONNECT, event = AoEvent.TP_CONN_TO, nextState = CmState.READY, af = ::af4Cm),
        FSM(currentState = CmState.TP_CONNECT, event = AoEvent.TP_CONN_CRM, nextState = CmState.TP_DATA, af = ::af5Cm),
        FSM(currentState = CmState.TP_DATA, event = AoEvent.TP_DATA_REC, nextState = CmState.TP_DATA, af = ::af6Cm),
        FSM(currentState = CmState.TP_DATA, event = CmEvent.TP_SEND, nextState = CmState.TP_DATA, af = ::af7Cm),
        FSM(currentState = CmState.TP_DATA, event = CmEvent.TP_CLOSE, nextState = CmState.READY, af = ::af8Cm),
        FSM(currentState = CmState.TP_DATA, event = AoEvent.TP_DISC, nextState = CmState.READY, af = ::af9Cm),
        // Reset
        FSM(currentState = CmState.TP_DATA, event = CmEvent.RESET, nextState = CmState.READY, af = ::af13Cm),

        // TCP
        FSM(currentState = CmState.READY, event = CmEvent.HTTP_START, nextState = CmState.HTTP_DATA, af = ::afNothing),
        FSM(currentState = CmState.HTTP_DATA, event = CmEvent.HTTP_SEND, nextState = CmState.HTTP_DATA, af = ::af11Cm),
        FSM(currentState = CmState.HTTP_DATA, event = AoEvent.HTTP_DATA_REC, nextState = CmState.HTTP_DATA, af = ::af12Cm),

        FSM(currentState = AO_TABLE_END, event = INVALID_EVENT, nextState = AO_TABLE_END, af = ::afNothing),
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
        producerIdx = 0
    )

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
        producerIdx = 0
    )

    /** Initialize all acb_t here */

    val cmAcb1 = ACB(
        id = AoId.AO_CM1_ID,
        currentState = CmState.INIT,
        eventQ = eventQ1,
        fsm = cmFsm,
        //[LL, TP, ...]
        srvDescriptors = arrayOf(null, null)
    )

    val cmAcb2 = ACB(
        id = AoId.AO_CM2_ID,
        currentState = CmState.INIT,
        eventQ = eventQ2,
        fsm = cmFsm,
        //[LL, TP, ...]
        srvDescriptors = arrayOf(null, null)
    )

    /**
     **********************************************************************
     * Action Function
     **********************************************************************
     */

    /** Do nothing Action Function */
    private fun afNothing(acb: ACB, buffer: EventBuffer): Boolean {
        return true
    }

    private fun cmInit(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "CM Init call")
        if(acb.id == AoId.AO_CM2_ID) {
            val ef = EventBuffer(eventId = CmEvent.HTTP_START)
            // Send HTTP START Event
            commonAO!!.sendEvent(aoId = acb.id,  buff = ef)
        }
        return true
    }

    /** TP Discovery Action Function */
    private fun af2_1Cm(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "CM TP Discovery call")
        td = transportAPI.tpOpen(acb, Port.CT_SOURCE, Port.CT_DESTINATION, "")
        if (td == null) Log.d(tag, "Open TP Fail")
        return true
    }


    /** TP Discovered Action Function */
    private fun af2_2Cm(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "CM TP Discovered call ${buffer.advPkt?.deviceName}")
        val intent = Intent(CmBRAction.ACT_TP_ADV_PKT)
        val ll = LinkDescriptor(
            deviceName = buffer.advPkt?.deviceName!!,
            mfgSN = buffer.advPkt?.mfgSN!!,
            uuid = buffer.advPkt?.uuid!!,
            type = buffer.advPkt?.type!!,
            mtu = buffer.advPkt?.mtu!!,
            version = buffer.advPkt!!.version,
            ble = buffer.advPkt?.ble,
        )
        val bundle = Bundle()
        bundle.putSerializable(DEVICE_ADVERTISE, ll)
        intent.putExtras(bundle)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

        return true
    }

    /** TP Select Action Function */
    private fun af2_3Cm(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "CM TP Select call")
        transportAPI.tpSelect(td!!, buffer.advPkt!!)
        return true
    }

    ///! TP ///

    /** TP Connect Action Function */
    private fun af2Cm(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "CM TP Connect call")

        td = transportAPI.tpOpen(acb, Port.CT_SOURCE, Port.CT_DESTINATION, buffer.csn)
        if (td == null) Log.d(tag, "Open TP Fail")

        return true
    }

    /** TP Connecting Action Function */
    private fun af3Cm(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "CM TP Connecting call")

        sendBroadCastReceiver(CmBRAction.ACT_TP_CON_REQUEST, "")

        return true
    }

    /** TP Connect Timeout Action Function */
    private fun af4Cm(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "CM TP Connect Timeout call")

        sendBroadCastReceiver(CmBRAction.ACT_TP_CON_TIMEOUT, "")

        return true
    }

    /** TP Connect Confirm Action Function */
    private fun af5Cm(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "CM TP Connect Confirm call")
        val intent = Intent(CmBRAction.ACT_TP_CON_READY)
        val bundle = Bundle()
        bundle.putSerializable(DEVICE_READY, buffer.srvDesc!!.ld)
        intent.putExtras(bundle)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

        return true
    }

    /** TP Data Receive  Action Function */
    private fun af6Cm(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "CM TP Data Receive  call => ${buffer.buffer?.hexa()}")

        // Send event to SRI
        val ef = EventBuffer(eventId = AoEvent.CM_DATA_REC, buffer = buffer.buffer)
        commonAO!!.sendEvent(AoId.AO_SRI_ID, ef)

        return true
    }

    /** TP send Action Function */
    private fun af7Cm(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "CM TP send  call")

        transportAPI.tpSend(td!!, buffer.buffer!!)

        return true
    }

    /** TP Close Action Function */
    private fun af8Cm(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "CM TP Close  call")
        transportAPI.tpClose(td!!)

        return true
    }

    /** TP Disconnect Action Function */
    private fun af9Cm(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "CM TP Disconnect  call")
        sendBroadCastReceiver(CmBRAction.ACT_TP_DISC, "")

        return true
    }

    /** TP Reset Action Function */
    private fun af13Cm(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "CM TP reset  call")
        transportAPI.tpReset(td!!)

        return true
    }

    /// HTTP ///

    /** HTTP Send Action Function */
    private fun af11Cm(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "HTTP SEND call")
        when (buffer.svrBuffer?.type) {
            SvrBufferType.unregisterReq -> {
                UnregisterRepository(context).unregisterRequest(buffer.svrBuffer!!.unregisterRequestBody!!)
            }

            SvrBufferType.loginReq -> {
                LoginRepository(context).loginRequest(buffer.svrBuffer!!.loginRequestBody!!)
            }
        }

        return true
    }

    /** HTTP DATA REC Action Function */
    private fun af12Cm(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "HTTP DATA REC call")
        // Send event to SRI
        val ef = EventBuffer(eventId = AoEvent.CM_DATA_REC, svrBuffer = buffer.svrBuffer)
        commonAO!!.sendEvent(AoId.AO_SRI_ID, ef)
        return true
    }

    /**
     **********************************************************************
     * Helper Function
     **********************************************************************
     */
    private fun sendBroadCastReceiver(act: String, data: String) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(act).putExtra("DATA", data))
    }
}

data class SvrBuffer(
    val type: Int = 0xff, // SvrBufferType
    val responseFailed: Boolean? = false,
    val unregisterRequestBody: UnregisterRequestBody? = null,
    val unregisterResponse: UnregisterResponse? =null,
    val loginRequestBody: LoginRequestBody? = null,
    val loginResponse: LoginResponse? = null
)

object SvrBufferType {
    const val unregisterReq = 0
    const val unregisterRes = 1
    const val loginReq = 2
    const val loginRes = 3
}

object CmState {
    const val INIT = 0
    const val READY = 1000
    const val DISCOVERY = 1001
    const val TP_CONNECT = 2000
    const val TP_DATA = 3000
    const val HTTP_DATA = 4000
}

object CmEvent {
    const val TP_DISCOVERY = 24
    const val TP_SELECT = 25
    const val TP_CONNECT = 26
    const val TP_SEND = 27
    const val TP_CLOSE = 28
    const val HTTP_START = 29
    const val HTTP_SEND = 30
    const val RESET = 31
    const val DISCOVERY_RESET = 32
}

object CmBRAction {
    const val ACT_TP_ADV_PKT = "com.ethernom.contact_tracing.AdvPkt"
    const val ACT_TP_CON_REQUEST = "com.ethernom.contact_tracing.ConnectionRequest"
    const val ACT_TP_CON_TIMEOUT = "com.ethernom.contact_tracing.tpConnectionTo"
    const val ACT_TP_CON_READY = "com.ethernom.contact_tracing.tpConnectionReady"
    const val ACT_TP_DISC = "com.ethernom.contact_tracing.tpDisconnect"

    const val ACT_TCP_CON_TIMEOUT = "com.ethernom.contact_tracing.tcpConnectionTo"
    const val ACT_TCP_CON_READY = "com.ethernom.contact_tracing.tcpConnectionReady"
}