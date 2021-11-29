package com.ethernom.maintenance.utils

import android.util.Log
import java.util.*
import kotlin.experimental.and


object Conversion {
    /* For convert integer to UUID*/
    fun convertFromInteger(i: Int): UUID? {
        val msb = 0x0000000000001000L
        val lsb = -0x7fffff7fa064cb05L
        val value = (i and ((-0x1).toLong()).toInt()).toLong()
        return UUID(msb or (value shl 32), lsb)
    }

    fun toUnsignedIntArray(byteArray: ByteArray): IntArray {
        val ret = IntArray(byteArray.size)
        for (i in byteArray.indices) {
            ret[i] = (byteArray[i] and 0xff.toByte()).toInt() // Range 0 to 255, not -128 to 128
        }
        return ret
    }

    /*Convert hex to ASCII*/
    fun convertHexToAscII(hex: String): String? {
        if (hex.length % 2 != 0) {
            System.err.println("Invalid hex string.")
            return ""
        }
        val builder = StringBuilder()
        var i = 0
        while (i < hex.length) {

            // Step-1 Split the hex string into two character group
            val s = hex.substring(i, i + 2)

            // Step-2 Convert the each character group into integer using valueOf method
            val n = Integer.valueOf(s, 16)
          //  Log.d("TAG", "n $n")
            // Step-3 Cast the integer value to char
            builder.append(n.toChar())
            i += 2
        }

        return builder.toString()
    }

//    fun convertHexToString(hex: String?): String? {
//        var result = ""
//        result = try {
//            val bytes: ByteArray = Hex.decode(hex)
//            String(bytes, StandardCharsets.UTF_8)
//        } catch (e: DecoderException) {
//            throw IllegalArgumentException("Invalid Hex format!")
//        }
//        return result
//    }

    // Hex -> Decimal -> Char
    fun convertHexToString(hex: String): String? {
        val result = StringBuilder()

        // split into two chars per loop, hex, 0A, 0B, 0C...
        var i = 0
        while (i < hex.length - 1) {
            val tempInHex = hex.substring(i, i + 2)
            Log.d("TAG", "tempInHex $tempInHex")
            //convert hex to decimal
            val decimal = tempInHex.toInt(16)
            Log.d("TAG", "decimal $decimal")

            // convert the decimal to char
            result.append(decimal.toChar())
            i += 2
        }
        return result.toString()
    }

    fun trim(bytes: ByteArray): ByteArray? {
        var i = bytes.size - 1
        while (i >= 0 && bytes[i].toInt() == 0) {
            --i
        }
        return bytes.copyOf(i + 1)
    }

    fun hexaToByteArray(str:String):ByteArray {
        val value = ByteArray(str.length / 2)
        for (i in value.indices) {
            val index = i * 2
            val j: Int = str.substring(index, index + 2).toInt(16)
            value[i] = j.toByte()
        }
        return value
    }

    fun hexToDecimal(hex: String): Int {
        return hex.toInt(16)
    }
}

private val HEX_CHARS = "0123456789abcdef".toCharArray()
fun ByteArray.hexa(): String {
    val result = StringBuffer()

    forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(HEX_CHARS[firstIndex])
        result.append(HEX_CHARS[secondIndex])
    }

    return result.toString().lowercase()
}

fun String.hexa(): ByteArray? {
    val str = this

    val `val` = ByteArray(str.length / 2)

    for (i in `val`.indices) {
        val index = i * 2
        val j: Int = str.substring(index, index + 2).toInt(16)
        `val`[i] = j.toByte()
    }

    return `val`
}

