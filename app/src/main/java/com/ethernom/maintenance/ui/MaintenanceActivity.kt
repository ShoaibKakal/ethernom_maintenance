package com.ethernom.maintenance.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.ethernom.maintenance.R
import com.ethernom.maintenance.adapter.MainMenuAdapter
import com.ethernom.maintenance.ao.capsuleFactoryReset.CapsuleFactoryResetAPI
import com.ethernom.maintenance.ao.capsuleFactoryReset.CapsuleFactoryResetBRAction
import com.ethernom.maintenance.ao.cm.CmType
import com.ethernom.maintenance.ao.debugProcess.DebugProcessAPI
import com.ethernom.maintenance.ao.debugProcess.DebugProcessBRAction
import com.ethernom.maintenance.ao.readQRCode.ReadQRCodeAPI
import com.ethernom.maintenance.ao.readQRCode.ReadQRCodeBRAction
import com.ethernom.maintenance.base.BaseActivity
import com.ethernom.maintenance.databinding.ActivityMaintenanceBinding
import com.ethernom.maintenance.model.DebugProcessModel
import com.ethernom.maintenance.model.RequestFailureModel
import com.ethernom.maintenance.utils.AppConstant
import com.ethernom.maintenance.utils.AppConstant.TIMER
import com.ethernom.maintenance.utils.session.ApplicationSession
import kotlin.system.exitProcess

class MaintenanceActivity : BaseActivity<ActivityMaintenanceBinding>() {

    override fun getViewBidingClass(): ActivityMaintenanceBinding {
        return ActivityMaintenanceBinding.inflate(layoutInflater)
    }

    override fun initView() {
        showToolbar(R.string.main_toolbar)
        binding.tvUsername.text = "Device Name: ${ApplicationSession.getInstance(this).getDeviceName()}"
        initRecyclerView()
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        cmAPI!!.cmReset(CmType.capsule)
        startPreviousActivity(DiscoverActivity::class.java, true)
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
            when(p1){
                0 -> {
                    showDialogInProgress(R.string.capsule_reset_title, R.string.capsule_reset_in_progress)
                    CapsuleFactoryResetAPI.capsuleFactoryResetRequest()
                }
                1 -> {
                    showDialogInProgress(R.string.debug_title, R.string.debug_in_progress)
                    DebugProcessAPI.debugProcessRequest()
                }
                2 -> {
                    showDialogInProgress(R.string.qr_code_title, R.string.qr_code_in_progress)
                    ReadQRCodeAPI.readQRCodeRequest()
                }
                3 -> startNextActivity(AboutActivity::class.java, false)
            }
        }
    }

    private val intentFilter: IntentFilter
        get()  {
            val intentAction = IntentFilter()
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
                CapsuleFactoryResetBRAction.ACT_RESET_RSP -> {}
                CapsuleFactoryResetBRAction.ACT_RESET_FAILURE -> {
                    val requestFailureModel = intent.getSerializableExtra(AppConstant.CAPSULE_FAILURE_KEY) as RequestFailureModel
                    showDialogFailed(R.string.capsule_reset_title, requestFailureModel.errorMessage){
                        finish()
                        exitProcess(0)
                    }
                }
                CapsuleFactoryResetBRAction.ACT_RESET_COMPLETED -> {
                    var isClick = false
                    showDialogSuccess(R.string.capsule_reset_title, R.string.capsule_reset_success){
                        startPreviousActivity(DiscoverActivity::class.java, true)
                        isClick = true
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        if(!isClick)
                            startPreviousActivity(DiscoverActivity::class.java, true)
                    }, 5000)
                }

                DebugProcessBRAction.ACT_DEBUG_PROCESS_RSP -> {}
                DebugProcessBRAction.ACT_DEBUG_PROCESS_FAILURE -> {
                    val requestFailureModel = intent.getSerializableExtra(AppConstant.CAPSULE_FAILURE_KEY) as RequestFailureModel
                    showDialogFailed(R.string.debug_title, requestFailureModel.errorMessage){
                        finish()
                        exitProcess(0)
                    }
                }
                DebugProcessBRAction.ACT_DEBUG_PROCESS_DATA_RSP -> {
                    if(intent.extras!!.containsKey(AppConstant.DEBUG_DATA_RES_KEY)){
                        val debugDataRes = intent.getSerializableExtra(AppConstant.DEBUG_DATA_RES_KEY) as DebugProcessModel
                        Log.d("debugProcess", "debugProcess: $debugDataRes")
                        val bundle = Bundle()
                        bundle.putSerializable(AppConstant.DEBUG_DATA_RES_KEY, debugDataRes)
                        showDialogSuccess(R.string.debug_title, R.string.debug_success){}
                        startNextActivity(DebugProcessActivity::class.java, bundle, true)

                    }
                }
                DebugProcessBRAction.ACT_TIMEOUT_UPDATE_CT -> {}
                DebugProcessBRAction.ACT_DEBUG_PROCESS_COMPLETED -> {}

                ReadQRCodeBRAction.READ_QR_CODE_RESPONSE -> {}
                ReadQRCodeBRAction.READ_QR_CODE_FAILURE -> {
                    val requestFailureModel = intent.getSerializableExtra(AppConstant.CAPSULE_FAILURE_KEY) as RequestFailureModel
                    showDialogFailed(R.string.qr_code_title, requestFailureModel.errorMessage){
                        finish()
                        exitProcess(0)
                    }
                }
                ReadQRCodeBRAction.READ_QR_CODE_COMPLETED -> {
                    val deviceName = intent.getStringExtra(AppConstant.DEVICE_KEY)
                    val sn = intent.getStringExtra(AppConstant.SERIAL_NUMBER_KEY)
                    val bundle = Bundle()
                    bundle.putString(AppConstant.DEVICE_KEY, deviceName)
                    bundle.putString(AppConstant.SERIAL_NUMBER_KEY, sn)
                    showDialogSuccess(R.string.qr_code_title, R.string.qr_code_success){}
                    startNextActivity(QRCodeActivity::class.java, bundle, true)
                }
            }
        }
    }

}