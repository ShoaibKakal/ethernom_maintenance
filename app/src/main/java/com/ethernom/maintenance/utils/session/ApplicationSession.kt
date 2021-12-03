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
        CAPSULE_VERSION, DEVICE_NAME, APP_LOCATION, APP_BLE_CNN
    }

    override val preferenceName: String
        get() = APP_SHARED_PREFS

    fun setCapsuleVersion(value: String) {
        save(SharedPreKeyType.CAPSULE_VERSION.toString(), value)
    }

    fun getCapsuleVersion() : String {
        return get(SharedPreKeyType.CAPSULE_VERSION.toString(), "").toString()
    }

    fun setDeviceName(value: String) {
        save(SharedPreKeyType.DEVICE_NAME.toString(), value)
    }

    fun getDeviceName() : String {
        return get(SharedPreKeyType.DEVICE_NAME.toString(), "").toString()
    }

    fun setAppLocationPermission(value: Boolean){
        save(SharedPreKeyType.APP_LOCATION.toString(), value)
    }

    fun getAppLocationPermission(): Boolean {
        return `is`(SharedPreKeyType.APP_LOCATION.toString(), false)
    }

    fun setAppBleCNNPermission(value: Boolean) {
        save(SharedPreKeyType.APP_BLE_CNN.toString(), value)
    }

    fun getAppBleCNNPermission(): Boolean{
        return `is`(SharedPreKeyType.APP_BLE_CNN.toString(), false)
    }




}
data class DeviceInfo(var name: String, var csn: String)
data class DeviceCertificate(var csn: String, var mfsn: String, var publicKey: String)