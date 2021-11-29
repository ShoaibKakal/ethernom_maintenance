package com.ethernom.maintenance.ao.select

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ethernom.maintenance.ao.*
import com.ethernom.maintenance.utils.AppConstant
import com.ethernom.maintenance.utils.session.DeviceInfo

class SelectAO(ctx: Context) {

    private val tag: String = javaClass.simpleName
    private val context: Context = ctx

    /* Common AO Variable */
    private var selectFsm = arrayOf(
            FSM(currentState = SelectState.INIT,        event = AoEvent.COMMON_INIT,        nextState = SelectState.SELECTING,  af = ::afNothing),
            FSM(currentState = SelectState.SELECTING,   event = SelectEvent.SELECT,         nextState = SelectState.SELECTED,   af = ::select),
            FSM(currentState = SelectState.SELECTED,    event = SelectEvent.QR_DETECTED,    nextState = SelectState.SELECTED,   af = ::detected),
            FSM(currentState = AO_TABLE_END,            event = INVALID_EVENT,              nextState = AO_TABLE_END,           af = ::afNothing),
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
                    EventBuffer(eventId = 0xff),
            ), full = 0,
            consumerIdx = 0,
            producerIdx = 0)

    /** Initialize all acb_t here */

    val selectAcb = ACB(
            id = AoId.AO_APP_ID,
            currentState = SelectState.INIT,
            eventQ = eventQ,
            fsm = selectFsm,
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
        Log.d(tag, "Select AO Init call")
        return true
    }

    private fun select(acb: ACB, buffer: EventBuffer): Boolean {
        Log.d(tag, "select call")
        val intent = Intent(SelectBRAction.SELECTING)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun detected(acb: ACB, buffer: EventBuffer): Boolean {
        // extract QR to get CSN
        // save the Qr include name , csn to local storage

        val deviceInfo = DeviceInfo("Goople!XXX", "0001000200000005")
      //  ApplicationSession.getInstance(context).saveDeviceInfo(deviceInfo)

        // Log.d("TAG", "cert ${buffer.csn}")

        val intent = Intent(SelectBRAction.DETECTED)
        intent.putExtra(AppConstant.CSN, deviceInfo.csn)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

        return true
    }
}

object SelectState {
    const val INIT = 0
    const val SELECTING = 1000
    const val SELECTED = 2000
}

object SelectEvent {
    const val SELECT = 16
    const val QR_DETECTED = 17
}

object SelectBRAction {
    const val SELECTING = "com.ethernom.contact_tracing.selecting"
    const val DETECTED = "com.ethernom.contact_tracing.detected"
}
