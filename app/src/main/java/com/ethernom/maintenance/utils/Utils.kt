@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.ethernom.maintenance.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.Editable
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object Utils {
    var REQUEST_CODE_SET_DEFAULT = 0
    var SET_DEFAULT_CARD = "SET_DEFAULT_CARD"
    var DISCOVER = "DISCOVER"
    var KEY_WEBSITE = "KEY_WEBSITE"

    var KEY_CREDENTIAL = "KEY_CREDENTIAL"
    var KEY_ACCOUNT_ITEM = "KEY_ACCOUNT_ITEM"
    private val nonDigits = Regex("[^\\d]")
    var DATE_FORMAT = "dd/M/yyyy"
    private var isEnable = true
    private var isSlash = false

    val isBluetoothEnable: Boolean
        get() = try {
            val bAdapter = BluetoothAdapter.getDefaultAdapter()
            bAdapter?.isEnabled ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

    // Request location permission
    fun requestLocationPermission(activity: Activity): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            Log.d("Utils", "Checking Location permissions")
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {

                    activity.requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ),
                        1
                    )

                } else {

                    activity.requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ),
                        1
                    )
                }
                return false
            } else {
                Log.d("UtilsLocation", "  Permission is granted")
                return true
            }
        } else return false
    }

    fun getAndroidVersion(): String? {
        var versionName = ""
        try {
            versionName = Build.VERSION.RELEASE.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return versionName
    }



    fun validateCardHolderName(nameOfHolder: String): Boolean {
        val validateCardHolderName = nameOfHolder.lastIndexOf(' ')
        return validateCardHolderName == -1
    }

    fun maskCreditCardNumber(number: String): String {
        return number.replace("\\w(?=\\w{4})".toRegex(), "*").chunked(4).joinToString("-")
    }

    @SuppressLint("SimpleDateFormat")
    fun getDateSchedule(time: Int): String {
        val dtStartDate = Date()
        val sdf = SimpleDateFormat(DATE_FORMAT)
        val calendar = Calendar.getInstance()
        calendar.time = dtStartDate
        calendar.add(Calendar.DATE, time)

        return sdf.format(calendar.time)
    }

    @SuppressLint("SimpleDateFormat")
    fun getDateTime(): String {
        val sdf = SimpleDateFormat(DATE_FORMAT)
        return sdf.format(Date())
    }

    fun getDaysBetweenDates(firstDateValue: String, secondDateValue: String): String {
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())

        val firstDate = sdf.parse(firstDateValue)
        val secondDate = sdf.parse(secondDateValue)

        if (firstDate == null || secondDate == null)
            return 0.toString()

        return (((secondDate.time - firstDate.time) / (1000 * 60 * 60 * 24)) + 1).toString()
    }

    fun digitOnly(string: String): String {
        return string.replace(nonDigits, "")
    }

    fun chunkString(text: String, type: String): String {
        return text.chunked(4).joinToString(type)
    }

    @SuppressLint("SetTextI18n")
    fun formatCardExpiringDate(s: Editable, creditCardExpireEt: AppCompatEditText): String {
        val input = s.toString()
        var mLastInput = ""
        var expiryDate = ""
        val formatter = SimpleDateFormat("MM/yy", Locale.ENGLISH)
        val expiryDateDate = Calendar.getInstance()
        try {
            expiryDateDate.time = formatter.parse(input)
        } catch (e: ParseException) {
            if (s.length == 2 && !mLastInput.endsWith("/") && isSlash) {
                isSlash = false
                val month = input.toInt()
                if (month <= 12) {
                    creditCardExpireEt.setText(creditCardExpireEt.text.toString().substring(0, 1))
                    creditCardExpireEt.setSelection(creditCardExpireEt.text.toString().length)
                } else {
                    s.clear()
                    creditCardExpireEt.setText("")
                    creditCardExpireEt.setSelection(creditCardExpireEt.text.toString().length)
                    //Toast.makeText(this.applicationContext, "Enter a valid month", Toast.LENGTH_LONG).show()
                }
            } else if (s.length == 2 && !mLastInput.endsWith("/") && !isSlash) {
                isSlash = true
                val month = input.toInt()
                if (month <= 12) {
                    creditCardExpireEt.setText(creditCardExpireEt.text.toString() + "/")
                    creditCardExpireEt.setSelection(creditCardExpireEt.text.toString().length)
                } else if (month > 12) {
                    creditCardExpireEt.setText("")
                    creditCardExpireEt.setSelection(creditCardExpireEt.text.toString().length)
                    s.clear()
                    // Toast.makeText(this.applicationContext, "Invalid Month", Toast.LENGTH_LONG).show()
                }
            } else if (s.length == 1) {
                val month = input.toInt()
                if (month in 2..11) {
                    isSlash = true
                    creditCardExpireEt.setText("0" + creditCardExpireEt.text.toString() + "/")
                    creditCardExpireEt.setSelection(creditCardExpireEt.text.toString().length)
                }
            }
            mLastInput = creditCardExpireEt.text.toString()
            expiryDate = mLastInput
            return expiryDate
        }
        return expiryDate
    }

    fun LOG(tag: String, message: String) {
        if (isEnable) {
            Log.d(tag, message)
        }
    }

    fun hideSoftKeyBoard(context: Context, view: View) {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        } catch (e: Exception) {
            // TODO: handle exception
            e.printStackTrace()
        }

    }

    /**
     * Returns MAC address of the given interface name.
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return  mac address or empty string
     */
    fun getMACAddress(interfaceName: String?): String? {
        try {
            val interfaces: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (interfaceName != null) {
                    if (!intf.name.equals(interfaceName, ignoreCase = true)) continue
                }
                val mac: ByteArray = intf.hardwareAddress ?: return ""
                val buf = StringBuilder()
                for (aMac in mac) buf.append(String.format("%02X:", aMac))
                if (buf.isNotEmpty()) buf.deleteCharAt(buf.length - 1)
                return buf.toString()
            }
        } catch (ignored: java.lang.Exception) {
        } // for now eat exceptions
        return ""
        /*try {
            // this is so Linux hack
            return loadFileAsString("/sys/class/net/" +interfaceName + "/address").toUpperCase().trim();
        } catch (IOException ex) {
            return null;
        }*/
    }

    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4   true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    fun getIPAddress(useIPv4: Boolean): String? {
        try {
            val interfaces: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs: List<InetAddress> = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr: String = addr.hostAddress
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (useIPv4) {
                            if (isIPv4) return sAddr
                        } else {
                            if (!isIPv4) {
                                val delim = sAddr.indexOf('%') // drop ip6 zone suffix
                                return if (delim < 0) sAddr.uppercase(Locale.getDefault()) else sAddr.substring(
                                    0,
                                    delim
                                ).uppercase(Locale.getDefault())
                            }
                        }
                    }
                }
            }
        } catch (ignored: java.lang.Exception) {
        } // for now eat exceptions
        return ""
    }

    fun getIPAddressV4(): ByteArray {
        val ipAddressV4 = getIPAddress(true)!!
        val strAdds = ipAddressV4.split('.')
        Log.d("address size", strAdds.size.toString())
        if(strAdds.size == 4) {
            return byteArrayOf(
                strAdds[0].toInt().toByte(),
                strAdds[1].toInt().toByte(),
                strAdds[2].toInt().toByte(),
                strAdds[3].toInt().toByte()
            )
        } else {
            return byteArrayOf(127, 0, 0, 1)
        }
    }

    fun makeAPDUHeader(command: Byte, payload: ByteArray = ByteArray(0)): ByteArray {
        /*
        +---------+---------+--------+--------+----------+
        | CMD (1) | REV (1) | LSB(1) | MSB(1) | Payload  |
        +---------+---------+--------+--------+----------+
        */

        val pLength = payload.size
        val l0 = pLength shr 8 // length of byte 0
        val l1 = pLength and 0xff // length of byte 1

        var apdu = ByteArray(0)
        apdu += command
        apdu += 0x00.toByte() // reserve byte
        apdu += l0.toByte() // [LSB] Least Significant Bit
        apdu += l1.toByte() // [MSB] Most Significant Bit
        apdu += payload
        return apdu
    }

    fun concatPayloadCapsuleFactoryReset(): ByteArray {
        val params = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(0x435E1A11).array()
        var payload = ByteArray(0)
        payload += params

        return payload
    }

    fun concatPayloadUpdateCT(status: Byte) :ByteArray {
        var payload = ByteArray(0)
        payload += status
        return  payload
    }


}