package com.ethernom.maintenance.ao.readQRCode

import com.ethernom.maintenance.ao.AoId
import com.ethernom.maintenance.ao.EventBuffer
import com.ethernom.maintenance.ui.commonAO

object ReadQRCodeAPI {
    fun readQRCodeRequest(){
        val event = EventBuffer(eventId = ReadQRCodeEvent.READ_QR_CODE_REQ)
        commonAO!!.sendEvent(AoId.AO_RQR_ID, event)
    }
}