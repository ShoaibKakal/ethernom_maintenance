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
    // Capsule Onboarding
    const val C2A_COB_REQ = 0x90.toByte() // Capsule Onboarding Request
    const val A2C_COB_RSP = 0xE0.toByte() // Capsule Onboarding Response

    const val C2A_COB_CERT_REQ = 0x91.toByte() // Verify Certificate Request
    const val A2C_COB_CERT_RSP = 0xE1.toByte() // Verify Certificate Response

    const val C2A_COB_COM = 0x92.toByte() // Capsule Onboarding Complete
    const val A2C_COB_ACK = 0xE2.toByte() // Capsule Onboarding Acknowledge

    // Capsule Onboard new = = = = = = = = = = = = = = = = = = = = = = = =
    const val A2C_COB_REQ       = 0x90.toByte()  // Capsule Onboarding Request
    const val C2A_COB_RSP       = 0xE0.toByte()  // Capsule Onboarding Response
    const val A2C_COB_CER_REQ   = 0x91.toByte()  // Save Certificate Request
    const val C2A_COB_CER_RSP   = 0xE1.toByte()  // Save Certificate Response
    const val A2C_COB_VER_REQ   = 0x92.toByte()  // Verify Certificate Request
    const val C2A_COB_VER_RSP   = 0xE2.toByte()  // Verify Certificate Response
    const val A2C_COB_COM       = 0x93.toByte()  // Capsule Onboarding Complete
    // = = = = = = = == =  = = = = = = = = = = = = = = = = = = = = = = = =


//    const val C2A_COB_REQ = 0x90.toByte() // Capsule Onboarding Request
//    const val A2C_COB_RSP = 0xE0.toByte() // Capsule Onboarding Response
//    const val C2A_COB_COM = 0x91.toByte() // Capsule Onboarding Complete
//    const val A2C_COB_ACK = 0xE1.toByte() // Capsule Onboarding Acknowledge

    // Global event
    const val C2A_C_DISCONNECT = 0x05.toByte() // capsule send disconnect
    const val C2A_Timestamp_Rqst = 0x12.toByte()
}

object Port {
    const val CT_SOURCE = 0x60.toByte()
    const val CT_DESTINATION = 0x65.toByte()
}


// Ethernom Transport Header Length = 8 , Ethernom Transport Encryption header Length = 16 ,
var ETH_TP_HEADER_SIZE: Int = 8
var APDU_HEADER_SIZE: Int = 4