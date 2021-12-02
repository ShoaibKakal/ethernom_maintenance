package com.ethernom.maintenance.model

import java.io.Serializable

data class DeviceModel(
    var llId: Int,
    var deviceName: String,
    var manufactureSerialNumber: String,
    var uuid: String)