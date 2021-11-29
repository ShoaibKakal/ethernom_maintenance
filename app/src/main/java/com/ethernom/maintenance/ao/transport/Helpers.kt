package com.ethernom.maintenance.ao.transport

import android.util.Log
import com.ethernom.maintenance.utils.hexa


object Helpers {
    private var ETH_BLE_ENC_HEADER_SIZE = 16
    fun parseEncryptedHeader(packet: ByteArray): Pair<ByteArray, ByteArray> {
        val encHdr = copyBytes(data = packet, startIdx = 0, count = ETH_BLE_ENC_HEADER_SIZE)

        val ct = packet.size - ETH_BLE_ENC_HEADER_SIZE
        var appPayload = ByteArray(0)

        if (ct != 0) {
            appPayload = copyBytes(data = packet, startIdx = ETH_BLE_ENC_HEADER_SIZE.toInt(), count = ct)
        }

        // no need to do this as payload is decrypted later   transportHdr.add(contentsOf: appPayload)
        return Pair(encHdr, appPayload)
    }

     fun makeTransportData(srcPort: Byte, destPort: Byte, control: Byte,interfaces:Byte, cmd: Byte, payload:ByteArray):ByteArray {
        val header =
            makeTransportHeader(
                srcPort,
                destPort,
                control,
                interfaces,
                cmd,
                payload.size
            ) // Construct data transport header
        return header + payload // Concat header and payload
    }

     private fun makeTransportHeader(srcPort: Byte, destPort: Byte,control:Byte, interfaces:Byte, cmd:Byte, payloadLength: Int): ByteArray {
        // length bytes, length is 2 bytes
        val l0 = payloadLength shr 8 // length of byte 1
        val l1 = payloadLength and 0xff // length of byte 0
        val packet = ByteArray(8)
        packet[0] = srcPort // Source port
        packet[1] = destPort // Destination port
        packet[2] = control // Control
        packet[3] = interfaces // Interface
        packet[4] = l0.toByte()
        packet[5] = l1.toByte()
        packet[6] = cmd // Command
        packet[7] = 0.toByte() // CheckSum
        packet[7] =
            getCheckSum(
                packet
            ) // Calculate check sum
        return packet
    }

     fun getCheckSum(packet: ByteArray) :Byte{
        var xorValue = packet[0].toInt()
        // xor the packet header for checksum
        for (j in 1..6) {
            val c = packet[j].toInt()
            xorValue = xorValue xor c
        }
        return xorValue.toByte()
    }

     fun getPayloadLength(l0: Byte, l1: Byte) : Int{ // Get length of payload
         val l = byteArrayOf(l0, l1)
         Log.d("getPayloadLength", l.hexa())
        return l.hexa().toInt(radix = 16)
    }

    fun getBit(byte: Byte, position: Int): Int {
        return byte.toInt() shr position and 1
    }

    fun copyBytes(data: ByteArray, startIdx: Int, count: Int) :ByteArray {
        var out = ByteArray(0)

        for (i in 0 until count) {
            val idx: Int = startIdx + i
            out += data[idx]
        }

        return out
    }

    fun rand(): Int {
        val start  = 0
        val end    = 9
        return (Math.random() * (end - start + 1)).toInt() + start
    }
}