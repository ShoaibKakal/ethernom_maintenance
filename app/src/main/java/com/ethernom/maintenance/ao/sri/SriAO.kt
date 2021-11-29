package com.ethernom.maintenance.ao.sri

import android.content.Context
import android.util.Log
import com.ethernom.maintenance.MainApplication
import com.ethernom.maintenance.ao.cm.SvrBufferType
import com.ethernom.maintenance.ao.*
import com.ethernom.maintenance.utils.APPCmd

class SriAO(ctx:Context) {
    private val tag: String = javaClass.simpleName
    private val application : MainApplication = ctx.applicationContext as MainApplication

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
        Log.d(tag, "SRI Data Receive  call")
        if (buffer.svrBuffer != null) {
            when(buffer.svrBuffer?.type) {
                SvrBufferType.capsuleCertRsp -> {
                    // Send capsule Onboard rsp Event to own Event Queue
                    val ef = EventBuffer(eventId = AoEvent.HTTP_SRV_CERT_RSP, svrBuffer = buffer.svrBuffer)
                    application.commonAO!!.sendEvent(AoId.AO_APP_ID, ef) //!TODO APP AO
                }

                SvrBufferType.verifyCertRsp -> {
                    // Send capsule Onboard rsp Event to own Event Queue
                    val ef = EventBuffer(eventId = AoEvent.HTTP_VERIFY_CERT_RSP, svrBuffer = buffer.svrBuffer)
                    application.commonAO!!.sendEvent(AoId.AO_APP_ID, ef) //!TODO APP AO
                }
            }
        } else {
            when (buffer.buffer!![0]) {

                APPCmd.C2A_COB_RSP -> {
                    val event = EventBuffer(eventId = AoEvent.C2A_ONBOARD_RSP, buffer = buffer.buffer)
                    application.commonAO!!.sendEvent(aoId = AoId.AO_APP_ID, event)  //!TODO App AO
                }

                APPCmd.C2A_COB_CER_RSP -> {
                    val event = EventBuffer(eventId = AoEvent.C2A_SAVE_CERT_RSP, buffer = buffer.buffer)
                    application.commonAO!!.sendEvent(aoId = AoId.AO_APP_ID, event)  //!TODO App AO
                }

                APPCmd.C2A_COB_VER_RSP -> {
                    val event = EventBuffer(eventId = AoEvent.C2A_VERIFY_CERT_RSP, buffer = buffer.buffer)
                    application.commonAO!!.sendEvent(aoId = AoId.AO_APP_ID, event)  //!TODO App AO
                }

                APPCmd.C2A_COB_CERT_REQ -> {
//                    val event = EventBuffer(eventId = AoEvent.C2A_VERIFY_CERT_RQST, buffer = buffer.buffer)
//                    commonAO!!.sendEvent(aoId = AoId.AO_APP_ID, event)  //!TODO App AO
                }

                APPCmd.C2A_COB_COM -> {
                    val event = EventBuffer(eventId = AoEvent.C2A_ONBOARD_COMPLETED, buffer = buffer.buffer)
                    application.commonAO!!.sendEvent(aoId = AoId.AO_APP_ID, event)  //!TODO App AO
                }

                APPCmd.C2A_C_DISCONNECT -> {
                    val event = EventBuffer(eventId = AoEvent.C2A_DISCONNECT, buffer = buffer.buffer)
                    application.commonAO!!.sendEvent(aoId = AoId.AO_APP_ID, event)  //!TODO App AO
                }

                // special timestamp
                APPCmd.C2A_Timestamp_Rqst -> {
                    // Send timestam[p req Event to own Event Queue
                    val ef = EventBuffer(eventId = AoEvent.C2A_TIMESTAMP_REQ, buffer = buffer.buffer)
                    application.commonAO!!.sendEvent(AoId.AO_APP_ID, ef) //!TODO onboard AO
//                    commonAO!!.sendEvent(AoId.AO_OB_ID, ef) //!TODO matching AO
//                    commonAO!!.sendEvent(AoId.AO_OB_ID, ef) //!TODO uploading AO
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
