package com.ethernom.maintenance.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.ethernom.maintenance.R
import com.ethernom.maintenance.adapter.MainMenuAdapter
import com.ethernom.maintenance.ao.BROADCAST_INTERRUPT
import com.ethernom.maintenance.ao.capsuleFactoryReset.CapsuleFactoryResetAPI
import com.ethernom.maintenance.ao.capsuleFactoryReset.CapsuleFactoryResetBRAction
import com.ethernom.maintenance.ao.debugProcess.DebugProcessAPI
import com.ethernom.maintenance.ao.debugProcess.DebugProcessBRAction
import com.ethernom.maintenance.ao.readQRCode.ReadQRCodeAPI
import com.ethernom.maintenance.ao.readQRCode.ReadQRCodeBRAction
import com.ethernom.maintenance.base.BaseActivity
import com.ethernom.maintenance.databinding.ActivityMaintenanceBinding
import com.ethernom.maintenance.model.AppRequestState
import com.ethernom.maintenance.model.DebugProcessModel
import com.ethernom.maintenance.model.DialogEnum
import com.ethernom.maintenance.model.RequestFailureModel
import com.ethernom.maintenance.utils.AppConstant
import com.ethernom.maintenance.utils.AppConstant.CAPSULE_VERSION
import com.ethernom.maintenance.utils.AppConstant.DEVICE_NAME
import kotlin.system.exitProcess

class MaintenanceActivity : BaseActivity<ActivityMaintenanceBinding>() {
    private val tag = javaClass.simpleName
    private var deviceName : String = ""
    private var capsuleVersion: String = ""
    private var isMenuItemClick: Boolean = false

    private lateinit var mCapsuleFactoryResetAPI: CapsuleFactoryResetAPI
    private lateinit var mDebugProcessAPI: DebugProcessAPI
    private lateinit var mReadQrCodeAPI: ReadQRCodeAPI

    override fun getViewBidingClass(): ActivityMaintenanceBinding {
        return ActivityMaintenanceBinding.inflate(layoutInflater)
    }

    override fun initView() {
        showToolbar(R.string.main_toolbar)
        initRecyclerView()
        if(intent.extras!!.containsKey(DEVICE_NAME)){
            deviceName = intent.getStringExtra(DEVICE_NAME)!!
            capsuleVersion = intent.getStringExtra(CAPSULE_VERSION)!!
            binding.tvUsername.text = "Device Name: $deviceName"
            Log.d(tag, "capsuleVersion: $capsuleVersion")

            initializeObj()
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onResume() {
        super.onResume()
        handleUiAction()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    override fun onBackPressed() {
//        super.onBackPressed()
//        cmAPI!!.cmReset(CmType.capsule)
//        startPreviousActivity(DiscoverActivity::class.java, true)
    }

    private fun initializeObj(){
        mCapsuleFactoryResetAPI = CapsuleFactoryResetAPI()
        mDebugProcessAPI = DebugProcessAPI()
        mReadQrCodeAPI = ReadQRCodeAPI()
    }

    private fun initRecyclerView(){
        binding.rcvMainMenu.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@MaintenanceActivity, 2)
            adapter = MainMenuAdapter(this@MaintenanceActivity, menuItemSelected)
        }
    }

    private val menuItemSelected = object : (Int) -> Unit {
        override fun invoke(p1: Int) {
            if(isMenuItemClick) return
            isMenuItemClick = true
            Log.d(tag, "Menu Item Selected!!!")
            when(p1){
                0 -> {
                    if(!isNetworkAvailable()){
                        showConfirmDialogFragment(DialogEnum.NETWORK.type){
                            isMenuItemClick = false
                        }
                        return
                    }
                    mCapsuleFactoryResetAPI.capsuleFactoryResetRequest()
                    showInProgressDialogFragment(DialogEnum.RESET_PROGRESS.type)
                }
                1 -> {
                    mDebugProcessAPI.debugProcessRequest()
                    showInProgressDialogFragment(DialogEnum.DEBUG_PROGRESS.type)
                }
                2 -> {
                    mReadQrCodeAPI.readQRCodeRequest()
                    showInProgressDialogFragment(DialogEnum.QR_PROGRESS.type)
                }
                3 -> {
                    isMenuItemClick = false
                    val bundle = Bundle()
                    bundle.putString(CAPSULE_VERSION, capsuleVersion)
                    startNextActivity(AboutActivity::class.java, bundle, false)
                }
            }
        }
    }

    private val intentFilter: IntentFilter
        get()  {
            val intentAction = IntentFilter()
            intentAction.addAction(BROADCAST_INTERRUPT)

            //Capsule Factory Reset
            intentAction.addAction(CapsuleFactoryResetBRAction.ACT_RESET_RSP)
            intentAction.addAction(CapsuleFactoryResetBRAction.ACT_RESET_FAILURE)
            intentAction.addAction(CapsuleFactoryResetBRAction.ACT_RESET_COMPLETED)

            //Debug Process
            intentAction.addAction(DebugProcessBRAction.ACT_DEBUG_PROCESS_RSP)
            intentAction.addAction(DebugProcessBRAction.ACT_DEBUG_PROCESS_FAILURE)
            intentAction.addAction(DebugProcessBRAction.ACT_DEBUG_PROCESS_DATA_RSP)
            intentAction.addAction(DebugProcessBRAction.ACT_TIMEOUT_UPDATE_CT)
            intentAction.addAction(DebugProcessBRAction.ACT_DEBUG_PROCESS_COMPLETED)

            //Read QR Code
            intentAction.addAction(ReadQRCodeBRAction.READ_QR_CODE_RESPONSE)
            intentAction.addAction(ReadQRCodeBRAction.READ_QR_CODE_FAILURE)
            intentAction.addAction(ReadQRCodeBRAction.READ_QR_CODE_COMPLETED)

            return intentAction
        }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            hideLoading()
            when(intent!!.action){
                BROADCAST_INTERRUPT -> commonAO!!.aoRunScheduler()

                CapsuleFactoryResetBRAction.ACT_RESET_RSP -> {}
                CapsuleFactoryResetBRAction.ACT_RESET_FAILURE -> {
                    val requestFailureModel = intent.getSerializableExtra(AppConstant.CAPSULE_FAILURE_KEY) as RequestFailureModel
                    Log.d(tag, "requestFailureModel ${requestFailureModel.errorCode}")
                    val bundle = Bundle()
                    bundle.putInt(AppConstant.ERROR_CODE, requestFailureModel.errorCode)
                    setAppStateRequest(AppRequestState.ACT_RESET_FAILURE.type, bundle)
                    handleUiAction()
                }
                CapsuleFactoryResetBRAction.ACT_RESET_COMPLETED -> {
                    setAppStateRequest(AppRequestState.ACT_RESET_COMPLETED.type, null)
                    handleUiAction()
                }

                DebugProcessBRAction.ACT_DEBUG_PROCESS_RSP -> {}
                DebugProcessBRAction.ACT_DEBUG_PROCESS_FAILURE -> {
                    val requestFailureModel = intent.getSerializableExtra(AppConstant.CAPSULE_FAILURE_KEY) as RequestFailureModel
                    val bundle = Bundle()
                    bundle.putInt(AppConstant.ERROR_CODE, requestFailureModel.errorCode)
                    setAppStateRequest(AppRequestState.ACT_DEBUG_PROCESS_FAILURE.type, bundle)
                    handleUiAction()
                }
                DebugProcessBRAction.ACT_DEBUG_PROCESS_DATA_RSP -> {
                    if(intent.extras!!.containsKey(AppConstant.DEBUG_DATA_RES_KEY)){
                        val debugDataRes = intent.getSerializableExtra(AppConstant.DEBUG_DATA_RES_KEY) as DebugProcessModel
                        Log.d("debugProcess", "debugProcess: $debugDataRes")
                        val bundle = Bundle()
                        bundle.putSerializable(AppConstant.DEBUG_DATA_RES_KEY, debugDataRes)
                        bundle.putString(CAPSULE_VERSION, capsuleVersion)
                        bundle.putString(DEVICE_NAME, deviceName)
                        setAppStateRequest(AppRequestState.ACT_DEBUG_PROCESS_DATA_RSP.type, bundle)
                        handleUiAction()
                    }
                }
                DebugProcessBRAction.ACT_TIMEOUT_UPDATE_CT -> {}
                DebugProcessBRAction.ACT_DEBUG_PROCESS_COMPLETED -> {}

                ReadQRCodeBRAction.READ_QR_CODE_RESPONSE -> {}
                ReadQRCodeBRAction.READ_QR_CODE_FAILURE -> {
                    val requestFailureModel = intent.getSerializableExtra(AppConstant.CAPSULE_FAILURE_KEY) as RequestFailureModel
                    val bundle = Bundle()
                    bundle.putInt(AppConstant.ERROR_CODE, requestFailureModel.errorCode)
                    setAppStateRequest(AppRequestState.READ_QR_CODE_FAILURE.type, bundle)
                    handleUiAction()
                }
                ReadQRCodeBRAction.READ_QR_CODE_COMPLETED -> {
                    Log.d("ReadQRCodeAO", "onReceive: READ_QR_CODE_COMPLETED")
                    val deviceName = intent.getStringExtra(AppConstant.DEVICE_KEY)
                    val sn = intent.getStringExtra(AppConstant.SERIAL_NUMBER_KEY)
                    val bundle = Bundle()
                    bundle.putString(AppConstant.DEVICE_KEY, deviceName)
                    bundle.putString(AppConstant.SERIAL_NUMBER_KEY, sn)
                    setAppStateRequest(AppRequestState.READ_QR_CODE_COMPLETED.type, bundle)
                    handleUiAction()
                }
            }
        }
    }

}