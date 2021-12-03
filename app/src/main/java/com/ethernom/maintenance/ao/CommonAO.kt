/**
 *******************************************************************************
 *
 * @file CommonAO.kt
 *
 * @brief Collection of all common function use for Active Object Environment
 *
 * @copyright Copyright (C) Ethernom 2021
 *
 *******************************************************************************
 */

package com.ethernom.maintenance.ao

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.ethernom.maintenance.ao.capsuleFactoryReset.CapsuleFactoryResetAO
import com.ethernom.maintenance.ao.cm.CmAO
import com.ethernom.maintenance.ao.cm.SvrBuffer
import com.ethernom.maintenance.ao.debugProcess.DebugProcessAO
import com.ethernom.maintenance.ao.link.LinkAO
import com.ethernom.maintenance.ao.link.LinkDescriptor
import com.ethernom.maintenance.ao.login.LoginAO
import com.ethernom.maintenance.ao.readQRCode.ReadQRCodeAO
import com.ethernom.maintenance.ao.sri.SriAO
import com.ethernom.maintenance.ao.transport.SocketDescriptor
import com.ethernom.maintenance.ao.transport.TransportAO
import com.ethernom.maintenance.model.DebugProcessModel
import com.ethernom.maintenance.model.RequestFailureModel
import com.ethernom.maintenance.ui.commonAO
import kotlin.reflect.KFunction2

@SuppressLint("NewApi")
class CommonAO(ctx: Context) {
    private val tag: String = javaClass.simpleName

    lateinit var linkAO: LinkAO
    lateinit var transportAO: TransportAO
    private lateinit var cmAO: CmAO
    private lateinit var sriAO: SriAO

    private lateinit var capsuleFactoryResetAO: CapsuleFactoryResetAO
    private lateinit var debugProcessAO: DebugProcessAO
    private lateinit var readQRCodeAO: ReadQRCodeAO
    private lateinit var loginAO: LoginAO


    private var aoScheduler: AOScheduler

    private val linkAOs: Array<ACB>
        get() = arrayOf(linkAO.linkAcb1, linkAO.linkAcb2)
    private val transportAOs: Array<ACB>
        get() = arrayOf(transportAO.tpAcb1, transportAO.tpAcb2)
    private val cmAOs: Array<ACB>
        get() = arrayOf(cmAO.cmAcb1, cmAO.cmAcb2)
    private val sriAOs: Array<ACB>
        get() = arrayOf(sriAO.sriAcb)
    private val capsuleFactoryResetAOs: Array<ACB>
        get() = arrayOf(capsuleFactoryResetAO.capsuleFactoryResetAcb)
    private val debugProcessAOs: Array<ACB>
        get() = arrayOf(debugProcessAO.debugProcessAcb)
    private val readQRCodeAOs: Array<ACB>
        get() = arrayOf(readQRCodeAO.readQRCodeAcb)
    private val loginAOs: Array<ACB>
        get() = arrayOf(loginAO.loginAcb)

    init {
        initialAO(ctx)
        aoScheduler = AOScheduler(ec = 0, acbTable = (linkAOs + transportAOs + cmAOs + sriAOs + capsuleFactoryResetAOs + debugProcessAOs + readQRCodeAOs + loginAOs ))

        val ef = EventBuffer(eventId = AoEvent.COMMON_INIT)
        // Send Init Event for all Active Object
        commonAO.apply {
            sendEvent(aoId = AoId.AO_LL1_ID, buff = ef)
            sendEvent(aoId = AoId.AO_LL2_ID, buff = ef)
            sendEvent(aoId = AoId.AO_TP1_ID, buff = ef)
            sendEvent(aoId = AoId.AO_TP2_ID, buff = ef)
            sendEvent(aoId = AoId.AO_CM1_ID, buff = ef)
            sendEvent(aoId = AoId.AO_CM2_ID, buff = ef)
            sendEvent(aoId = AoId.AO_SRI_ID, buff = ef)

            sendEvent(aoId = AoId.AO_CFR_ID, buff = ef)
            sendEvent(aoId = AoId.AO_DBP_ID, buff = ef)
            sendEvent(aoId = AoId.AO_RQR_ID, buff = ef)
            sendEvent(aoId = AoId.AO_LOG_ID, buff = ef)
        }
    }

    private fun initialAO(ctx: Context) {
        linkAO = LinkAO(ctx)
        transportAO = TransportAO(ctx)
        cmAO = CmAO(ctx)
        sriAO = SriAO(ctx)

        capsuleFactoryResetAO = CapsuleFactoryResetAO(ctx)
        debugProcessAO = DebugProcessAO(ctx)
        readQRCodeAO = ReadQRCodeAO(ctx)
        loginAO = LoginAO(ctx)
    }

    fun sendEvent(aoId: Int, buff: EventBuffer): Boolean {
        val result: Boolean

        val acb: ACB = aoScheduler.acbTable[aoId]
        // add buffer into AO event queue by producer
        result = producer(acb, buff)
        if (result)
            aoScheduler.ec++ // increase total event counter if add buffer success
        return result
    }

    fun aoRunScheduler() {
        var buff: EventBuffer? = null
        var acb: ACB? = null

        // checking if total event counter is not empty
        while (aoScheduler.ec > 0) {
            // loop through all Active Objects in ACB Tables
            for (i in aoScheduler.acbTable.indices) {
                acb = aoScheduler.acbTable[i]
                buff = consumer(acb) // consume event
                if (buff != null) {
                    aoScheduler.ec-- // decrease total event counter
                    aoFindPatternFsm(acb, buff) // pass ACB to FSM
                }
            }
        }

    } // end

    /* Find pattern matching in Finite State Machine */
    private fun aoFindPatternFsm(acb: ACB, buff: EventBuffer) {
        var i = 0
        val currentState = acb.currentState
        val currentEvent = buff.eventId

        val fsm: Array<FSM> = acb.fsm
        var fsmEntry: FSM? = null

        var error = true
        do // loop until match current state/event
        {
            fsmEntry = fsm[i]
            if (fsmEntry.currentState == currentState && fsmEntry.event == currentEvent) {
                error = false
                val result = fsmEntry.af(acb, buff)//execute action function
                if (result) {
                    acb.currentState = fsmEntry.nextState
                } else if (result == null) {
                    Log.d(
                        tag,
                        "Implementation problem on AO:${acb.id} - state:$currentState - event:$currentEvent"
                    )
                } // Implementation problem
                break
            }
            i++ // increase FSM index
        } while (fsmEntry!!.currentState != AO_TABLE_END)
        if (error) {
            // Error
            Log.d(tag, "ERROR")
        }
    }

    private fun producer(acb: ACB, buff: EventBuffer): Boolean {

        val eventQ = acb.eventQ

        return if (eventQ.full < QUEUE_CAPACITY) {
            // TODO: Enter CS (DI)
            // insert event into slot pointed to by producer index
            eventQ.buffer[eventQ.producerIdx] = buff
            // increase AO event queue capacity
            eventQ.full++
            // increase producer index and wrap around
            eventQ.producerIdx = (eventQ.producerIdx + 1) % QUEUE_CAPACITY // 0 or remainder

            // TODO: Exit CS (EI)
            true
        } else false
    }

    private fun consumer(acb: ACB): EventBuffer? {
        var buff: EventBuffer? = null

        // TODO: Enter CS (DI)
        val eventQ = acb.eventQ
        if (eventQ.full != 0) {
            // consumer buf into slot that pointed to by consumer index
            buff = eventQ.buffer[eventQ.consumerIdx]
            // decrease AO event queue capacity
            eventQ.full--
            // increase consumer index and wrap around
            eventQ.consumerIdx = ((eventQ.consumerIdx + 1) % QUEUE_CAPACITY) // 0 or remainder
        }
        // TODO: Exit CS (EI)
        return buff
    }

}

const val QUEUE_CAPACITY: Int = 8
const val AO_TABLE_END: Int = 0xff
const val INVALID_EVENT: Int = 0xff
const val BROADCAST_INTERRUPT: String = "com.ethernom.contact_tracing.broadcast_interrupt"


object AoId {
    const val AO_LL1_ID = 0
    const val AO_LL2_ID = 1
    const val AO_TP1_ID = 2
    const val AO_TP2_ID = 3
    const val AO_CM1_ID = 4
    const val AO_CM2_ID = 5
    const val AO_SRI_ID = 6
    const val AO_CFR_ID = 7
    const val AO_DBP_ID = 8
    const val AO_RQR_ID = 9
    const val AO_LOG_ID = 10
}

/** 0 to 24 For Common Event */
object AoEvent {
    const val COMMON_INIT = 0
    const val TP_DISCOVERED = 1
    const val TP_CONN_TO = 2
    const val TP_CONNECTING = 3
    const val TP_CONN_CRM = 4
    const val TP_DATA_REC = 5
    const val TP_DISC = 6
    const val CM_DATA_REC = 7
    const val HTTP_DATA_REC = 8

    // Event receive capsule factory reset
    const val C2A_CFR_RES = 9
    const val HTTP_RESET_CERT_RES = 10
    const val C2A_FRD_RES = 11

    // Event receive debug process
    const val C2A_DBP_RES = 12
    const val C2A_DPD_RES = 13
    const val C2A_UCT_RES = 14

    // Event receive Read QR Code
    const val C2A_RQR_RES = 15


}

/** Index of all Service Descriptor */
object DescIdx {
    const val LL_DESC = 0
    const val TP_DESC = 1
}

/** Service Descriptors */
data class SrvDesc(
    var aoUser: ACB?,
    var aoService: ACB,
    // Service Specific Data
    var ld: LinkDescriptor? = null,
    var sd: SocketDescriptor? = null,
)

/** Event Buffer Content Of Event ID And Service Descriptor And Data Buffer **/
data class EventBuffer(
    var eventId: Int,
    var srvDesc: SrvDesc? = null,
    var buffer: ByteArray? = byteArrayOf(),
    var svrBuffer: SvrBuffer? = null,
    var csn: String = "",
    var advPkt: LinkDescriptor? = null,
    val requestFailure: RequestFailureModel? = null,
    val deviceName: String? = "",
    val serialNumber: String? = "",
    val debugProcessModel: DebugProcessModel? = null,
    val updateCTStatus: Boolean? = false,
    val completedType: Int? = 0
)

/** Active Object Event Queue */
data class EventQ(
    var buffer: Array<EventBuffer> = arrayOf<EventBuffer>(),
    var full: Int,
    var consumerIdx: Int,
    var producerIdx: Int
)

/** Active object finite state machine table entry */
data class FSM(
    var currentState: Int,
    var event: Int,
    var nextState: Int,
    var af: KFunction2<ACB, EventBuffer, Boolean>
)

/** Active Object Control Block – one per active object */
data class ACB(
    var id: Int,
    var currentState: Int,
    var eventQ: EventQ,
    var fsm: Array<FSM>,
    // Service Descriptor  [LD, TD , ...]
    var srvDescriptors: Array<SrvDesc?>
)

/** Active Object Scheduler */
data class AOScheduler(
    var ec: Int,
    var acbTable: Array<ACB>
)
