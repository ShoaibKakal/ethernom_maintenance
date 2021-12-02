package com.ethernom.maintenance.utils

object TransportCmd {

    const val H2C_DATA = 0x03.toByte()

    const val C2H_CONNECT_REQ = 0x01.toByte()
    const val H2C_CONNECT_RSP = 0x02.toByte()
    const val C2H_CONNECT_CFM = 0x04.toByte()

    const val C2H_DISCONNECT_REQ = 0x05.toByte()
    const val H2C_KA = 0x07.toByte()
}

object APPCmd {
    // Capsule Factory Reset
    const val A2C_FR_REQ = 0x15.toByte() // Factory Reset Request
    const val C2A_FR_RSP = 0x55.toByte() // Factory Reset Response
    const val A2C_FR_COM = 0x16.toByte() // Factory Reset Complete

    // Debug Process
    const val A2C_DBP_REQ       = 0x40.toByte() // Debug Process Request
    const val C2A_DBP_RSP       = 0x80.toByte() // Debug Process Response
    const val A2C_DBP_DATA_REQ  = 0x41.toByte() // Debug Process Data Request
    const val C2A_DBP_DATA_RSP  = 0x81.toByte() // Debug Process Data Response
    const val A2C_DBP_CT_REQ    = 0x42.toByte() // Update CT Request
    const val C2A_DBP_CT_RSP    = 0x82.toByte() // Update CT Response
    const val A2C_DBP_COM       = 0x43.toByte() // Debug Process Complete

    // Read QR Code
    const val A2C_RQR_REQ = 0x44.toByte()    // Read QR Code Request
    const val C2A_RQR_RSP = 0x84.toByte()    // Read QR Code Response
    const val A2C_RQR_COM = 0x45.toByte()    // Read QR Code Complete


}

object Port {
    const val CT_SOURCE = 0x60.toByte()
    const val CT_DESTINATION = 0x65.toByte()
}


// Ethernom Transport Header Length = 8 , Ethernom Transport Encryption header Length = 16 ,
var ETH_TP_HEADER_SIZE: Int = 8
var APDU_HEADER_SIZE: Int = 4