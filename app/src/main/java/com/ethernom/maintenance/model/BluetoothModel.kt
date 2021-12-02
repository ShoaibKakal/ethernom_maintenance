package com.ethernom.maintenance.model

import java.io.Serializable

sealed class BluetoothRequest{
    object BLESetting: BluetoothRequest()
    object BLEConnect: BluetoothRequest()
}

sealed class BluetoothResponse: Serializable{
    object BLESettingTurnOn: BluetoothResponse()
    object BLESettingTurnOff: BluetoothResponse()
    object BLEConnectAllow: BluetoothResponse()
    object BLEConnectDeny: BluetoothResponse()
}