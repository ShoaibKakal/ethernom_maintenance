package com.ethernom.maintenance.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ethernom.maintenance.MainApplication
import com.ethernom.maintenance.ao.BROADCAST_INTERRUPT
import com.ethernom.maintenance.ao.CommonAO
import com.ethernom.maintenance.ao.cm.CmAPI
import com.ethernom.maintenance.ao.select.SelectAPI
import com.ethernom.maintenance.R
import com.ethernom.maintenance.databinding.ActivitySplashBinding
import com.ethernom.maintenance.utils.Utils

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var mBinding : ActivitySplashBinding
    private lateinit var application: MainApplication

    private var mLocationPermission: ((status: (Boolean)) -> Unit) = {}
    private val TAG = javaClass.simpleName
    private val PERMISSION_REQUEST_COARSE_LOCATION = 112

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        mBinding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        application = (this.applicationContext as MainApplication)

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateChange, filter)
        // initialize all class needed
        initObject()

        checkLocationPermission {
            Handler().postDelayed({
                if(!Utils.isBluetoothEnable) {
                    bleMessageDialog()
                }else {
                    navigateToNextPage()
                }

            }, 3000)
        }

        // get app version name
        val versionName: String = this.packageManager.getPackageInfo(this.packageName, 0).versionName
        mBinding.appVersion.text = resources.getString(R.string.app_version) + " " + versionName
    }

    @SuppressLint("NewApi")
    private fun initObject() {
        // Initial require below class
        application.foo = "foo"
        application.commonAO = CommonAO(this)
        application.selectAPI = SelectAPI(this)
        application.cmAPI = CmAPI(this)
        // Register interrupt receiver
        val filter = IntentFilter(BROADCAST_INTERRUPT)
        LocalBroadcastManager.getInstance(this).registerReceiver(interruptBroadcastReceiver, filter)

        application.commonAO!!.aoRunScheduler()
    }


    private val interruptBroadcastReceiver = object : BroadcastReceiver() { // Broadcast receiver callback Bluetooth state change
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("tag", "interruptBroadcastReceiver call")
//             commonAO!!.aoRunScheduler()
        }
    }

    @SuppressLint("NewApi")
    private fun checkLocationPermission(locationPermission: ((state: (Boolean)) -> Unit)) {
        this.mLocationPermission = locationPermission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // This is Case 1. Now we need to check further if permission was shown before or not
            Log.d(
                TAG,
                "This is Case 1. Now we need to check further if permission was shown before or not"
            )
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )) {
                // This is Case 4.
                Log.d(TAG, "This is Case 4.")
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ),
                    PERMISSION_REQUEST_COARSE_LOCATION
                )
            } else {
                // This is Case 3. Request for permission here
                Log.d(TAG, "This is Case 3. Request for permission here")
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    PERMISSION_REQUEST_COARSE_LOCATION
                )
            }
        } else {
            // This is Case 2. You have permission now you can do anything related to it
            Log.d(TAG, "This is Case 2. You have permission now you can do anything related to it")
            mLocationPermission(true)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // This is Case 2 (Permission is now granted)
                Log.d(TAG, "This is Case 2 (Permission is now granted)")
                mLocationPermission(true)
            } else {
                mLocationPermission(false)
            }
        }
    }

    private fun bleMessageDialog() {
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.warning))
        builder.setMessage(resources.getString(R.string.turn_on_bluetooth_message_cred))
        builder.setCancelable(true)
        builder.setPositiveButton(resources.getString(R.string.enable)) { dialog, _ ->
            dialog.cancel()
            turnOnBluetooth()
        }
        val bleAlertDialog = builder.create()
        bleAlertDialog.setCancelable(false)
        bleAlertDialog.show()
    }

    private fun turnOnBluetooth(): Boolean {
        val bluetoothAdapter = BluetoothAdapter
            .getDefaultAdapter()
        return bluetoothAdapter?.enable() ?: false
    }

    private val bluetoothStateChange = object : BroadcastReceiver() { // Broadcast receiver callback Bluetooth state change
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent!!.action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

                when (intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )) {
                    BluetoothAdapter.STATE_ON -> {
                        navigateToNextPage()
                        unregisterBroadcastReceiver(context!!)
                    }
                }
            }
        }
    }

    private fun unregisterBroadcastReceiver(ctx: Context) {
        unregisterReceiver(bluetoothStateChange)
    }

    private fun navigateToNextPage() {
        val intent = Intent(this, DiscoverActivity::class.java)
        startActivity(intent)
        finish()
    }
}