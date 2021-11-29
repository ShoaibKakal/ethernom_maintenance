package com.ethernom.maintenance.utils.session

import android.annotation.SuppressLint
import android.content.Context
import com.ethernom.maintenance.base.BaseSession

class ApplicationSession(val context: Context) : BaseSession(context) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: ApplicationSession? = null
        private const val APP_SHARED_PREFS: String = "ContractTracing"

        fun getInstance(context: Context): ApplicationSession {
            if (instance == null)
                instance = ApplicationSession(context)
            return instance!!
        }
    }
    enum class SharedPreKeyType {
        DEVICE_CERTIFICATE,DEVICE_NAME
    }

    override val preferenceName: String
        get() = APP_SHARED_PREFS

    /**
     * DEVICE MODEL
     */

    fun setDeviceCertificate(value: String) {
        save(SharedPreKeyType.DEVICE_CERTIFICATE.toString(), value)
    }

    fun getDeviceCertificate() : String {
        return get(SharedPreKeyType.DEVICE_CERTIFICATE.toString(), "").toString()
    }

    fun setDeviceName(value: String) {
        save(SharedPreKeyType.DEVICE_NAME.toString(), value)
    }

    fun getDeviceName() : String {
        return get(SharedPreKeyType.DEVICE_NAME.toString(), "").toString()
    }

}
data class DeviceInfo(var name: String, var csn: String)
data class DeviceCertificate(var csn: String, var mfsn: String, var publicKey: String)