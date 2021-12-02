package com.ethernom.maintenance.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ethernom.maintenance.R
import com.ethernom.maintenance.adapter.DeviceAdapter
import com.ethernom.maintenance.ao.BROADCAST_INTERRUPT
import com.ethernom.maintenance.ao.cm.CmBRAction
import com.ethernom.maintenance.ao.cm.CmType
import com.ethernom.maintenance.ao.link.LinkDescriptor
import com.ethernom.maintenance.base.BaseActivity
import com.ethernom.maintenance.broadcast.LocationBC
import com.ethernom.maintenance.broadcast.LocationBroadcast
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

class DiscoverActivity : BaseActivity<ActivityDiscoverBinding>() {

    private val tag = "DiscoverTAG"
    private lateinit var mDeviceAdapter: DeviceAdapter
    private lateinit var mAppSession: ApplicationSession
    private var dataLinkDescriptor = ArrayList<LinkDescriptor>()
    private var deviceSelected: DeviceModel? = null
    private val REQUEST_CHECK_SETTINGS = 0x1
    private var bleAlertDialog: AlertDialog? = null

    override fun getViewBidingClass(): ActivityDiscoverBinding {
        return ActivityDiscoverBinding.inflate(layoutInflater)
    }

    override fun initView() {
        mAppSession = ApplicationSession.getInstance(this)

        showToolbar(R.string.discover_toolbar_text)
        initRecyclerView()
        handleSettingService()

        // Register interrupt receiver
        val filter = IntentFilter(BROADCAST_INTERRUPT)
        LocalBroadcastManager.getInstance(this).registerReceiver(interruptBroadcastReceiver, filter)
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilterAction())
        val filterBLe = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateChange, filterBLe)


    }

    /**
     * Activity lifecycle *
     */
    override fun onResume() {
        super.onResume()
        handleButtonMessage()
        handleErrorMessage()
        mDeviceAdapter.clearAllDevice()
        dataLinkDescriptor.clear()
//        cmAPI!!.cmDiscovery(CmType.capsule)
//        commonAO!!.aoRunScheduler()
    }

    override fun onDestroy() {
        super.onDestroy()
    }



    /**
     * UI Functions
     */
    private fun initRecyclerView(){
        mDeviceAdapter = DeviceAdapter(this, mutableListOf(), deviceItemSelected)
        binding.rcvDevice.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@DiscoverActivity)
            adapter = mDeviceAdapter
        }
    }

    private val deviceItemSelected = object : (DeviceModel, Int) -> Unit {
        override fun invoke(device: DeviceModel, position: Int) {
            showLoading(resources.getString(R.string.loading_connecting) + " to ${device.deviceName}...")
            /* Call connect to Link Layer */
            cmAPI!!.cmSelect(CmType.capsule, dataLinkDescriptor[position])
            commonAO!!.aoRunScheduler()
            deviceSelected = device
        }
    }

    private fun handleButtonMessage(){
        binding.btnOpenSetting.setOnClickListener {
            when(binding.tvTitle.text){
                resources.getString(R.string.location_permission_title) -> {
                    startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)))
                }
                resources.getString(R.string.bluetooth_permission_title) -> {
                    startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)))
                }
                resources.getString(R.string.location_service_title) -> {}
                resources.getString(R.string.bluetooth_setting_title) -> {}
            }
        }
    }

    private fun handleErrorMessage() {
        val permission = checkAppPermission()
        Log.d(tag, "permission: $permission")
        if (permission.isNotEmpty()) {
            when (permission[0]) {
                Manifest.permission.ACCESS_FINE_LOCATION -> {
                    showErrorLayout(R.string.location_permission_title, R.string.location_permission_message)
                }
                Manifest.permission.BLUETOOTH_CONNECT -> {
                    showErrorLayout(R.string.bluetooth_permission_title, R.string.bluetooth_permission_message)
                }
            }
        } else {
            binding.btnOpenSetting.visibility = View.GONE
        }
    }

    private fun showErrorLayout(@StringRes title: Int, @StringRes msg: Int) {
        binding.btnOpenSetting.visibility = View.VISIBLE
        binding.tvTitle.text = resources.getString(title)
        binding.tvMessage.text = resources.getString(msg)
    }

    private fun checkAppPermission(): ArrayList<String> {
        val appPermissions : ArrayList<String> = ArrayList()
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            appPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            appPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            appPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        return appPermissions
    }

    private fun handleSettingService() {
        if(checkAppPermission().isNotEmpty()) return
        if(!Utils.isBluetoothEnable)
            displayBluetoothSettingRequest()
        else
            displayLocationSettingsRequest()
    }

    private fun displayBluetoothSettingRequest() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.warning))
        builder.setMessage(resources.getString(R.string.turn_on_bluetooth_message_cred))
        builder.setCancelable(true)
        builder.setPositiveButton(resources.getString(R.string.enable)) { dialog, _ ->
            dialog.cancel()
            turnOnBluetooth()
        }
        bleAlertDialog = builder.create()
        bleAlertDialog!!.show()
    }

    private fun turnOnBluetooth(): Boolean {
        val bluetoothAdapter = BluetoothAdapter
            .getDefaultAdapter()
        return bluetoothAdapter?.enable() ?: false
    }

    private fun displayLocationSettingsRequest() {
        val googleApiClient = GoogleApiClient.Builder(this).addApi(LocationServices.API).build()
        googleApiClient.connect()
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 10000
        locationRequest.fastestInterval = (10000 / 2).toLong()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result =
            LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())

        result.setResultCallback { result ->
            val status = result.status
            when (status.statusCode) {
                LocationSettingsStatusCodes.SUCCESS -> Log.i(tag, "All location settings are satisfied.")
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                    Log.i(tag, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ")
                    try {
                        // Show the dialog by calling startResolutionForResult(), and check the result
                        // in onActivityResult().
                        status.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                    } catch (e: IntentSender.SendIntentException) {
                        Log.i(tag, "PendingIntent unable to execute request.")
                    }
                }
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> Log.i(
                    tag, "Location settings are inadequate, and cannot be fixed here. Dialog not created."
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> when (resultCode) {
                RESULT_OK -> binding.btnOpenSetting.visibility = View.GONE
                RESULT_CANCELED -> showErrorLayout(R.string.location_service_title, R.string.location_service_message)
            }
        }
    }


    /**
     * BroadCast Receiver *
     **/
    private val interruptBroadcastReceiver =
        object : BroadcastReceiver() { // Broadcast receiver callback Bluetooth state change
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(tag, "interruptBroadcastReceiver call")
                commonAO!!.aoRunScheduler()
            }
        }

    private val bluetoothStateChange = object : BroadcastReceiver() { // Broadcast receiver callback Bluetooth state change
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent!!.action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_ON -> {
                        binding.btnOpenSetting.visibility = View.GONE
                        bleAlertDialog!!.dismiss()
                        displayLocationSettingsRequest()
                    }

                    BluetoothAdapter.STATE_OFF -> {
                        showErrorLayout(R.string.bluetooth_setting_title, R.string.bluetooth_setting_message)
                    }
                }
            }
        }
    }

    private fun intentFilterAction(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(CmBRAction.ACT_TP_ADV_PKT)
        intentFilter.addAction(CmBRAction.ACT_TP_CON_REQUEST)
        intentFilter.addAction(CmBRAction.ACT_TP_CON_TIMEOUT)
        intentFilter.addAction(CmBRAction.ACT_TP_CON_READY)
        intentFilter.addAction(LocationBC.LOCATION_DISABLE)
        intentFilter.addAction(LocationBC.LOCATION_ENABLE)
        return intentFilter
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.S)
        @SuppressLint("NotifyDataSetChanged")
        override fun onReceive(p0: Context?, dataIntent: Intent?) {
            Log.d(tag, "BR COMING ${dataIntent?.action.toString()}")
            when (dataIntent?.action) {
                // advertising
                CmBRAction.ACT_TP_ADV_PKT -> {
                    val name = dataIntent.getStringExtra(DEVICE_NAME)
                    val mFSN = dataIntent.getStringExtra(MANUFAC_SERIAL_NUMBER)
                    val uuid = dataIntent.getStringExtra(UUID)
                    val type = dataIntent.getByteExtra(TYPE, 0)
                    val mtu = dataIntent.getIntExtra(MTU, 0)
                    val ble = dataIntent.getParcelableExtra<BluetoothDevice>(BLUETOOTH_DEVICE)

                    Log.d("TAG", "data discover")

                    val deviceModel = DeviceModel(0, name!!, mFSN!!, uuid!!)
                    val llId = LinkDescriptor(name, mFSN, uuid, type, mtu, ble)
                    dataLinkDescriptor.add(llId)
                    mDeviceAdapter.addDevice(deviceModel)
                }

                // Connecting

                CmBRAction.ACT_TP_CON_REQUEST -> {
                    Log.d(tag, "ACT_TP_CON_REQUEST")
                }

                CmBRAction.ACT_TP_CON_TIMEOUT -> {
                    Log.d(tag, "ACT_TP_CON_TIMEOUT ")

                }

                CmBRAction.ACT_TP_CON_READY -> {
                    Log.d(tag, "ACT_TP_CON_READY ")
                    hideLoading()
                    ApplicationSession.getInstance(this@DiscoverActivity).setDeviceName(deviceSelected!!.deviceName)
                    startNextActivity(MaintenanceActivity::class.java, true)
                }

                LocationBC.LOCATION_DISABLE -> {
                    Log.d(tag, "LOCATION_DISABLE")
                }
                LocationBC.LOCATION_ENABLE -> {
                    Log.d(tag, "LOCATION_ENABLE")

                }
            }
        }
    }



}