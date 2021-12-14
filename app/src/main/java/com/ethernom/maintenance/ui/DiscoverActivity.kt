package com.ethernom.maintenance.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.annotation.StringRes
import androidx.core.location.LocationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ethernom.maintenance.R
import com.ethernom.maintenance.adapter.DeviceAdapter
import com.ethernom.maintenance.ao.BROADCAST_INTERRUPT
import com.ethernom.maintenance.ao.cm.CmBRAction
import com.ethernom.maintenance.ao.cm.CmType
import com.ethernom.maintenance.ao.link.LinkDescriptor
import com.ethernom.maintenance.base.BaseActivity
import com.ethernom.maintenance.databinding.ActivityDiscoverBinding
import com.ethernom.maintenance.utils.AppConstant.CAPSULE_VERSION
import com.ethernom.maintenance.utils.AppConstant.DEVICE_ADVERTISE
import com.ethernom.maintenance.utils.AppConstant.DEVICE_NAME
import com.ethernom.maintenance.utils.AppConstant.DEVICE_READY
import com.ethernom.maintenance.utils.AppConstant.TIMER
import com.ethernom.maintenance.utils.Utils
import com.ethernom.maintenance.utils.session.ApplicationSession
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import kotlin.system.exitProcess


class DiscoverActivity : BaseActivity<ActivityDiscoverBinding>() {

    private val tag = "DiscoverTAG"
    private lateinit var mDeviceAdapter: DeviceAdapter
    private lateinit var mAppSession: ApplicationSession
    private val REQUEST_CHECK_LOCATION_SERVICE = 0x1

    private var locationServiceInitCheck: Boolean = true
    private var bluetoothSettingInitCheck: Boolean = true
    private var appInBackground: Boolean = false
    private var deviceItemClick: Boolean = false

    private var connectionTimeout = false
    private var refreshTimeout = false

    private lateinit var mHandler: Handler

    override fun getViewBidingClass(): ActivityDiscoverBinding {
        return ActivityDiscoverBinding.inflate(layoutInflater)
    }

    override fun initView() {
        mAppSession = ApplicationSession.getInstance(this)
        mHandler = Handler(Looper.getMainLooper())

        showToolbar(R.string.discover_toolbar_text)
        initRecyclerView()
        errorLayoutListener()
        registerAllBroadcast()
        binding.btnQuestion.setOnClickListener {
            showSuggestionDialog(R.string.advertise_device_title, R.string.advertise_device_msg, R.string.dialog_ok) {}
        }

        binding.swipeRefresh.setOnRefreshListener {
            startDiscoveryDevice()
            refreshTimeout = false
        }
    }

    override fun onResume() {
        super.onResume()
        appInBackground = false
        checkAppPermissionAndSetting()
    }

    override fun onStop() {
        super.onStop()
        appInBackground = true
    }

    override fun onDestroy() {
        super.onDestroy()
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.unregisterReceiver(appsReceiver)
        unRegisterBluetoothReceiver()
        unRegisterLocationBroadcast()
    }

    private fun checkAppPermissionAndSetting() {
        when {
            allAppPermission -> {
                when (checkAppPermission()[0]) {
                    Manifest.permission.ACCESS_FINE_LOCATION -> {
                        showErrorLayout(R.string.location_permission_title, R.string.location_permission_message)
                    }
                    Manifest.permission.BLUETOOTH_CONNECT -> {
                        showErrorLayout(R.string.nearby_device_permission_title, R.string.nearby_device_permission_message)
                    }
                }
                return
            }
            bleSetting -> {
                if (bluetoothSettingInitCheck) {
                    binding.layoutError.visibility = View.GONE
                    showSuggestionDialog(R.string.bluetooth_device_title, R.string.bluetooth_device_turn_on, R.string.turn_on) {
                        BluetoothAdapter.getDefaultAdapter()?.enable()
                    }
                } else {
                    showErrorLayout(R.string.bluetooth_device_title, R.string.bluetooth_device_message)
                }
                return
            }
            locationService -> {
                if (locationServiceInitCheck)displayLocationSettingsRequest()
                else showErrorLayout(R.string.location_service_title, R.string.location_service_message)
                return
            }

            discoverNearbyDevice -> {
                if(appInBackground) {
                    appInBackground = false
                    return
                }
                startDiscoveryDevice()
            }
        }
    }

    private val allAppPermission: Boolean
        get() = checkAppPermission().isNotEmpty()

    private val bleSetting: Boolean
        get() = !Utils.isBluetoothEnable

    private val locationService: Boolean
        get() {
            val manager = getSystemService(LOCATION_SERVICE) as LocationManager
            return !LocationManagerCompat.isLocationEnabled(manager)
        }

    private val discoverNearbyDevice: Boolean
        get() {
            return true
        }

    private fun displayLocationSettingsRequest() {
        val googleApiClient = GoogleApiClient.Builder(this).addApi(LocationServices.API).build()
        googleApiClient.connect()
        val locationRequest = LocationRequest.create()
        locationRequest.apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000
            fastestInterval = (10000 / 2).toLong()
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())
        result.setResultCallback { rs ->
            val status = rs.status
            when (status.statusCode) {
                LocationSettingsStatusCodes.SUCCESS -> Log.i(tag, "All location settings are satisfied.")
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                    Log.i(tag, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ")
                    locationServiceInitCheck = false
                    try {
                        // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                        status.startResolutionForResult(this, REQUEST_CHECK_LOCATION_SERVICE)
                    } catch (e: IntentSender.SendIntentException) {
                        Log.i(tag, "PendingIntent unable to execute request.")
                    }
                }
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE ->
                    Log.i(tag, "Location settings are inadequate, and cannot be fixed here. Dialog not created."
                )
            }
        }
    }

    private fun showErrorLayout(@StringRes title: Int, @StringRes msg: Int) {
        binding.layoutError.visibility = View.VISIBLE
        binding.tvTitle.text = resources.getString(title)
        binding.tvMessage.text = resources.getString(msg)
    }

    private fun errorLayoutListener() {
        binding.btnMessage.setOnClickListener {
            when (binding.tvTitle.text) {
                resources.getString(R.string.location_permission_title) -> {
                    startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null)
                        )
                    )
                }
                resources.getString(R.string.nearby_device_permission_title) -> {
                    startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null)
                        )
                    )
                }
                resources.getString(R.string.location_service_title) -> {
                    val viewIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(viewIntent)
                }
                resources.getString(R.string.bluetooth_device_title) -> {
                    val intentOpenBluetoothSettings = Intent()
                    intentOpenBluetoothSettings.action = Settings.ACTION_BLUETOOTH_SETTINGS
                    startActivity(intentOpenBluetoothSettings)
                }
            }
        }
    }

    private fun startDiscoveryDevice() {
        binding.layoutError.visibility = View.GONE
        cmAPI!!.cmResetDiscovery(CmType.capsule)
        commonAO!!.aoRunScheduler()
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(tag, "Discover Nearby Device!!!")
            removeTimeout(refreshTimeout)
            mDeviceAdapter.clearAllDevice()
            cmAPI!!.cmDiscovery(CmType.capsule)
            commonAO!!.aoRunScheduler()
        }, 1000)
    }

    private fun refreshDiscoverDevice(){
        if(refreshTimeout) return
        mHandler.postDelayed({
            Log.d(tag, "refreshDiscoverDevice")
            refreshTimeout = false
            binding.swipeRefresh.isRefreshing = true
            startDiscoveryDevice()
        }, 30000)
    }

    private fun initRecyclerView() {
        mDeviceAdapter = DeviceAdapter(this, deviceItemSelected)
        binding.rcvDevice.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@DiscoverActivity)
            adapter = mDeviceAdapter
        }
    }

    private val deviceItemSelected = object : (LinkDescriptor, Int) -> Unit {
        override fun invoke(device: LinkDescriptor, position: Int) {
            if(deviceItemClick) return
            showLoading(resources.getString(R.string.loading_connecting) + " to ${device.deviceName}...")
            cmAPI!!.cmSelect(CmType.capsule, device)
            commonAO!!.aoRunScheduler()

            connectionTimeout = true
            mHandler.postDelayed({
                if (connectionTimeout && !appInBackground) {
                    connectionTimeout = false
                    hideLoading()
                    showDialogTimeout(R.string.connection_timeout_title, R.string.connection_timeout_msg) {
                        if(it){
                            mDeviceAdapter.clearAllDevice()
                            startDiscoveryDevice()
                        } else {
                            finish()
                            exitProcess(0)
                        }
                    }
                }
            }, 15000)
        }
    }

    private fun removeTimeout(timeout: Boolean){
        if(!timeout) return
        mHandler.removeCallbacksAndMessages(null)
    }

    /**
     * BroadCast Receiver *
     **/
    private fun registerAllBroadcast() {
        // Register App receiver
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(appsReceiver, intentFilterAction)
        // Register Bluetooth receiver
        registerBluetoothBroadcast(bluetoothReceiver)
        // Register Location receiver
        registerLocationBroadcast(locationReceiver)
    }

    private val bluetoothReceiver = object : (Int) -> Unit {
        override fun invoke(bleState: Int) {
            if(bleState == BluetoothAdapter.STATE_TURNING_OFF ||
                bleState == BluetoothAdapter.STATE_TURNING_ON) return
            Log.d(tag, "ble state: $bleState")
            binding.swipeRefresh.isRefreshing = false
            mDeviceAdapter.clearAllDevice()
            bluetoothSettingInitCheck = false
            binding.layoutError.visibility = View.GONE
            checkAppPermissionAndSetting()
            if (alertDialog != null) alertDialog!!.dismiss()
        }
    }

    private val locationReceiver = object : (Boolean) -> Unit {
        override fun invoke(status: Boolean) {
            Log.d(tag, "location receiver: $status")
            binding.swipeRefresh.isRefreshing = false
            locationServiceInitCheck = false
            binding.layoutError.visibility = View.GONE
            checkAppPermissionAndSetting()
        }
    }

    private val intentFilterAction: IntentFilter
        get() {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BROADCAST_INTERRUPT)
            intentFilter.addAction(CmBRAction.ACT_TP_ADV_PKT)
            intentFilter.addAction(CmBRAction.ACT_TP_CON_REQUEST)
            intentFilter.addAction(CmBRAction.ACT_TP_CON_TIMEOUT)
            intentFilter.addAction(CmBRAction.ACT_TP_CON_READY)
            return intentFilter
        }

    private val appsReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, dataIntent: Intent?) {
            when (dataIntent?.action) {
                BROADCAST_INTERRUPT -> commonAO!!.aoRunScheduler()
                // advertising
                CmBRAction.ACT_TP_ADV_PKT -> {
                    binding.swipeRefresh.isRefreshing = false
                    val ll = dataIntent.getSerializableExtra(DEVICE_ADVERTISE) as LinkDescriptor
                    Log.d(tag, "ACT_TP_ADV_PKT $ll")
                    if(!Utils.isBluetoothEnable) return
                    mDeviceAdapter.addDevice(ll)

                    refreshDiscoverDevice()
                    refreshTimeout = true
                }
                CmBRAction.ACT_TP_CON_TIMEOUT -> {
                    hideLoading()
                    Log.d(tag, "ACT_TCP_CON_TIMEOUT")
                    removeTimeout(connectionTimeout)
                    connectionTimeout = false
                    if(!appInBackground) return
                    showDialogTimeout(R.string.connection_timeout_title, R.string.connection_timeout_msg) {
                        if(it){
                            mDeviceAdapter.clearAllDevice()
                            startDiscoveryDevice()
                        } else {
                            finish()
                            exitProcess(0)
                        }
                    }
                }

                CmBRAction.ACT_TP_CON_READY -> {
                    Log.d(tag, "ACT_TP_CON_READY ")
                    hideLoading()
                    removeTimeout(connectionTimeout)
                    val llReady = dataIntent.getSerializableExtra(DEVICE_READY) as LinkDescriptor
                    Log.d(tag, "$llReady")
                    val bundle = Bundle()
                    bundle.putString(DEVICE_NAME, llReady.deviceName)
                    bundle.putString(CAPSULE_VERSION, llReady.version)
                    startNextActivity(MaintenanceActivity::class.java, bundle, true)
                }
            }
        }
    }


}