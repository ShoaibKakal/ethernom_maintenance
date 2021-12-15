package com.ethernom.maintenance.ao.sri

import android.content.Context
import android.util.Log
import com.ethernom.maintenance.ao.*
import com.ethernom.maintenance.ao.cm.SvrBufferType
import com.ethernom.maintenance.ui.commonAO
import com.ethernom.maintenance.utils.APPCmd
import com.ethernom.maintenance.utils.hexa

class SriAO(ctx:Context) {
    private val tag: String = javaClass.simpleName

    /** Common AO Variable */
    private var sriFsm = arrayOf(
        FSM( currentState = SriState.INIT,     event = AoEvent.COMMON_INIT,    nextState = SriState.SRV_INP,   af = ::afNothing ),
        FSM( currentState = SriState.SRV_INP,  event = AoEvent.CM_DATA_REC,     nextState = SriState.SRV_INP,  af = ::af2Cm ),

        FSM( currentState = AO_TABLE_END,       event = INVALID_EVENT,          nextState = AO_TABLE_END,     af = ::afNothing ),
    )

    private var eventQ = EventQ(
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

    val sriAcb = ACB(
        id = AoId.AO_SRI_ID,
        currentState = SriState.INIT,
        eventQ = eventQ,
        fsm = sriFsm,
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
        Log.d(tag, "SRI Init call")
        return true
    }


    /** Data Receive Action Function */
    private fun af2Cm(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "SRI Data Receive  call ${buffer.buffer!!.hexa()}")
        if (buffer.svrBuffer != null) {
            when(buffer.svrBuffer?.type) {
                SvrBufferType.unregisterRes -> {
                    val ef = EventBuffer(eventId = AoEvent.HTTP_RESET_CERT_RES, svrBuffer = buffer.svrBuffer)
                    commonAO!!.sendEvent(AoId.AO_CFR_ID, ef) //!TODO Capsule Factory Reset AO
                }

                SvrBufferType.loginRes -> {
                    val ef = EventBuffer(eventId = AoEvent.HTTP_LOGIN_RES, svrBuffer = buffer.svrBuffer)
                    commonAO!!.sendEvent(AoId.AO_LOG_ID, ef) //!TODO Capsule Factory Reset AO
                }
            }
        } else {
            when (buffer.buffer!![0]) {
                APPCmd.C2A_FR_RSP -> {
                    val event = EventBuffer(eventId = AoEvent.C2A_CFR_RES, buffer = buffer.buffer)
                    commonAO!!.sendEvent(aoId = AoId.AO_CFR_ID, event)  //!TODO Capsule Factory Reset AO
                }

                APPCmd.C2A_RQR_RSP -> {
                    val event = EventBuffer(eventId = AoEvent.C2A_RQR_RES, buffer = buffer.buffer)
                    commonAO!!.sendEvent(aoId = AoId.AO_RQR_ID, event)  //!TODO Read QR CODE AO
                }

                APPCmd.C2A_DBP_RSP -> {
                    val event = EventBuffer(eventId = AoEvent.C2A_DBP_RES, buffer = buffer.buffer)
                    commonAO!!.sendEvent(aoId = AoId.AO_DBP_ID, event)  //!TODO DEBUG Process AO
                }

                APPCmd.C2A_DBP_DATA_RSP -> {
                    val event = EventBuffer(eventId = AoEvent.C2A_DPD_RES, buffer = buffer.buffer)
                    commonAO!!.sendEvent(aoId = AoId.AO_DBP_ID, event)  //!TODO DEBUG Process AO
                }

                APPCmd.C2A_DBP_CT_RSP -> {
                    val event = EventBuffer(eventId = AoEvent.C2A_UCT_RES, buffer = buffer.buffer)
                    commonAO!!.sendEvent(aoId = AoId.AO_DBP_ID, event)  //!TODO DEBUG Process AO
                }
            }
        }
        return true
    }
}

object SriState {
    const val INIT = 0
    const val SRV_INP = 1000
}
