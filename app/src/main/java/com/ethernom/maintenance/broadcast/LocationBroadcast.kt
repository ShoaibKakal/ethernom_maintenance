package com.ethernom.maintenance.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class LocationBroadcast : BroadcastReceiver() {
    private var isGpsEnabled: Boolean = false
    private var isNetworkEnabled: Boolean = false

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "LocationBroadcast Receiver")
        intent.action?.let { act ->
            if (act.matches("android.location.PROVIDERS_CHANGED".toRegex())) {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                Log.i(TAG, "Location Providers changed, is GPS Enabled: $isGpsEnabled")

                //Start your Activity if location was enabled:
                if (isGpsEnabled || isNetworkEnabled) {
                    Log.d(TAG, "LOCATION ON")
                    sendBroadcastReceiver(context, LocationBC.LOCATION_ENABLE)
                }else {
                    sendBroadcastReceiver(context, LocationBC.LOCATION_DISABLE)
                }
            }
        }
    }

    companion object {
        private val TAG = "LocationProviderChanged"
    }

    private fun sendBroadcastReceiver(context: Context, act: String) {
        val intent = Intent(act)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}

object LocationBC{
    const val LOCATION_ENABLE = "com.ethernom.maintenance.LOCATION_ENABLE"
    const val LOCATION_DISABLE = "com.ethernom.maintenance.LOCATION_DISABLE"
}