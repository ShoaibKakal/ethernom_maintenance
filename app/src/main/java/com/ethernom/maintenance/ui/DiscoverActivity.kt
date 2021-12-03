package com.ethernom.maintenance.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.annotation.StringRes
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
import com.ethernom.maintenance.model.DeviceModel
import com.ethernom.maintenance.utils.AppConstant.BLUETOOTH_DEVICE
import com.ethernom.maintenance.utils.AppConstant.DEVICE_NAME
import com.ethernom.maintenance.utils.AppConstant.MANUFAC_SERIAL_NUMBER
import com.ethernom.maintenance.utils.AppConstant.MTU
import com.ethernom.maintenance.utils.AppConstant.TYPE
import com.ethernom.maintenance.utils.AppConstant.UUID
import com.ethernom.maintenance.utils.Utils
import com.ethernom.maintenance.utils.session.ApplicationSession
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.ethernom.maintenance.utils.AppConstant
import com.ethernom.maintenance.utils.AppConstant.CAPSULE_VERSION
import com.ethernom.maintenance.utils.AppConstant.DEVICE_ADVERTISE
import com.ethernom.maintenance.utils.AppConstant.DEVICE_READY
import com.ethernom.maintenance.utils.AppConstant.TIMER
import kotlin.system.exitProcess


class DiscoverActivity : BaseActivity<ActivityDiscoverBinding>() {

    private val tag = "DiscoverTAG"
    private lateinit var mDeviceAdapter: DeviceAdapter
    private lateinit var mAppSession: ApplicationSession
    private var dataLinkDescriptor = ArrayList<LinkDescriptor>()
    private var deviceSelected: LinkDescriptor? = null
    private val REQUEST_CHECK_LOCATION_SERVICE = 0x1

    private var locationService: Boolean = true
    private var bluetoothDevice: Boolean = true

    override fun getViewBidingClass(): ActivityDiscoverBinding {
        return ActivityDiscoverBinding.inflate(layoutInflater)
    }

    override fun initView() {
        mAppSession = ApplicationSession.getInstance(this)
        showToolbar(R.string.discover_toolbar_text)
        initRecyclerView()
        handleSettingService()
        registerAllBroadcast()

        binding.btnQuestion.setOnClickListener {
            showSuggestionDialog(R.string.advertise_device_title, R.string.advertise_device_msg, R.string.dialog_ok){}
        }
    }

    override fun onResume() {
        super.onResume()
        handleDiscover()
    }

    private fun handleDiscover() {
        Log.d(tag, "${handleButtonMessage()}, ${locationService}, $bluetoothDevice")
        if (handleButtonMessage() && locationService && bluetoothDevice) {
            Log.d(tag, "call to discover device!!!")
            mDeviceAdapter.clearAllDevice()
            dataLinkDescriptor.clear()
            cmAPI!!.cmReset(CmType.capsule)
            cmAPI!!.cmDiscovery(CmType.capsule)
            commonAO!!.aoRunScheduler()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.unregisterReceiver(broadcastReceiver)
        unRegisterBluetoothReceiver()
        unRegisterLocationBroadcast()
    }

    private fun initRecyclerView(){
        mDeviceAdapter = DeviceAdapter(this, deviceItemSelected)
        binding.rcvDevice.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@DiscoverActivity)
            adapter = mDeviceAdapter
        }
    }

    private var connectionTimeout = false
    private val deviceItemSelected = object : (LinkDescriptor, Int) -> Unit {
        override fun invoke(device: LinkDescriptor, position: Int) {
            showLoading(resources.getString(R.string.loading_connecting) + " to ${device.deviceName}...")
            cmAPI!!.cmSelect(CmType.capsule, device)
            commonAO!!.aoRunScheduler()
            deviceSelected = device

            connectionTimeout = true
            Handler(Looper.getMainLooper()).postDelayed({
                if(connectionTimeout){
                    connectionTimeout = false
                    hideLoading()
                    showDialogFailed(R.string.connection_timeout_title, R.string.connection_timeout_msg){
                        finish()
                        exitProcess(0)
                    }
                }
            }, TIMER)
        }
    }

    private fun handleButtonMessage():Boolean{
        binding.btnMessage.setOnClickListener {
            when(binding.tvTitle.text){
                resources.getString(R.string.location_permission_title) -> {
                    startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
                }
                resources.getString(R.string.nearby_device_permission_title) -> {
                    startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)))
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
        return handleErrorMessage()
    }

    private fun handleErrorMessage(): Boolean {
        val permission = checkAppPermission()
        return if (permission.isNotEmpty()) {
            when (permission[0]) {
                Manifest.permission.ACCESS_FINE_LOCATION -> {
                    showErrorLayout(R.string.location_permission_title, R.string.location_permission_message)
                }
                Manifest.permission.BLUETOOTH_CONNECT -> {
                    showErrorLayout(R.string.nearby_device_permission_title, R.string.nearby_device_permission_message)
                }
            }
            false
        } else if(!locationService) {
            showErrorLayout(R.string.location_service_title, R.string.location_service_message)
            false
        } else {
            binding.btnOpenSetting.visibility = View.GONE
            true
        }
    }

    private fun showErrorLayout(@StringRes title: Int, @StringRes msg: Int) {
        binding.btnOpenSetting.visibility = View.VISIBLE
        binding.tvTitle.text = resources.getString(title)
        binding.tvMessage.text = resources.getString(msg)
    }

    private fun handleSettingService() {
        if(checkAppPermission().isNotEmpty()) return
        if(!Utils.isBluetoothEnable){
            bluetoothDevice = false
            showSuggestionDialog(R.string.bluetooth_device_title, R.string.bluetooth_device_turn_on, R.string.turn_on){
                BluetoothAdapter.getDefaultAdapter()?.enable()
            }
        }
        else
            displayLocationSettingsRequest()
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
                    try {
                        locationService = false
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_CHECK_LOCATION_SERVICE && resultCode == RESULT_CANCELED){
            locationService = false
        }

    }

    /**
     * BroadCast Receiver *
     **/
    private fun registerAllBroadcast() {
        // Register App receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilterAction)
        // Register Bluetooth receiver
        registerBluetoothBroadcast(bluetoothReceiver)
        // Register Location receiver
        registerLocationBroadcast(locationReceiver)
    }

    private val bluetoothReceiver = object : (Int) -> Unit {
        override fun invoke(bleState: Int) {
            Log.d(tag, "ble state: $bleState")
            if(bleState == BluetoothAdapter.STATE_OFF){
                mDeviceAdapter.clearAllDevice()
                dataLinkDescriptor.clear()
                bluetoothDevice = false
                showErrorLayout(R.string.bluetooth_device_title, R.string.bluetooth_device_message)
            }
            if(bleState == BluetoothAdapter.STATE_ON) {
                bluetoothDevice = true
                binding.btnOpenSetting.visibility = View.GONE
                if(alertDialog != null) alertDialog!!.dismiss()
                displayLocationSettingsRequest()
                handleDiscover()
            }
        }
    }

    private val locationReceiver = object : (Boolean) -> Unit {
        override fun invoke(status: Boolean) {
            Log.d(tag, "location receiver: $status")
            if(status) {
                locationService = true
                handleDiscover()
            }
            if(!status) {
                showErrorLayout(R.string.location_service_title, R.string.location_service_message)
            }
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

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, dataIntent: Intent?) {
//            Log.d(tag, "BR COMING ${dataIntent?.action}")
            when (dataIntent?.action) {
                BROADCAST_INTERRUPT -> commonAO!!.aoRunScheduler()
                // advertising
                CmBRAction.ACT_TP_ADV_PKT -> {
                    val ll = dataIntent.getSerializableExtra(DEVICE_ADVERTISE) as LinkDescriptor
                    Log.d(tag, "ACT_TP_ADV_PKT $ll")
                    mDeviceAdapter.addDevice(ll)
                }
                CmBRAction.ACT_TP_CON_READY -> {
                    Log.d(tag, "ACT_TP_CON_READY ")
                    hideLoading()
                    if(!connectionTimeout) return
                    connectionTimeout = false

                    val llReady = dataIntent.getSerializableExtra(DEVICE_READY) as LinkDescriptor
                    Log.d(tag, "$llReady")
                    val bundle = Bundle()
                    bundle.putString(DEVICE_NAME, llReady.deviceName)
                    bundle.putString(CAPSULE_VERSION, llReady.version)
                    startNextActivity(MaintenanceActivity::class.java, bundle,true)
                }
            }
        }
    }




}