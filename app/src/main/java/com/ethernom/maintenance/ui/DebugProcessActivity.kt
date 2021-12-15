package com.ethernom.maintenance.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ethernom.maintenance.R
import com.ethernom.maintenance.adapter.DebugProcessAdapter
import com.ethernom.maintenance.ao.BROADCAST_INTERRUPT
import com.ethernom.maintenance.ao.debugProcess.DebugProcessAPI
import com.ethernom.maintenance.ao.debugProcess.DebugProcessBRAction
import com.ethernom.maintenance.base.BaseActivity
import com.ethernom.maintenance.databinding.ActivityDebugProcessBinding
import com.ethernom.maintenance.model.*
import com.ethernom.maintenance.utils.AppConstant
import java.lang.Long
import kotlin.system.exitProcess

class DebugProcessActivity : BaseActivity<ActivityDebugProcessBinding>() {

    private lateinit var debugProcessAPI: DebugProcessAPI
    private var isUpdatedCT: Boolean = false
    private var mCTStatus: Boolean =false

    override fun getViewBidingClass(): ActivityDebugProcessBinding {
        return ActivityDebugProcessBinding.inflate(layoutInflater)
    }

    override fun initView() {
        debugProcessAPI = DebugProcessAPI()
        showToolbarBackPress(R.string.debug_toolbar)
        initRecyclerView()
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    override fun onBackPressed() {
        showLoading("Loading...")
        debugProcessAPI.closeUpdateCT()
    }


    private fun initRecyclerView() {
        val debugProcessAdapter = DebugProcessAdapter(this)
        binding.rcvDebugProcess.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@DebugProcessActivity)
            adapter = debugProcessAdapter
        }
        loadData(debugProcessAdapter)
    }

    private fun loadData(debugProcessAdapter: DebugProcessAdapter) {
        if (intent.extras!!.containsKey(AppConstant.DEBUG_DATA_RES_KEY)) {
            val debugDataRes: DebugProcessModel =
                intent.getSerializableExtra(AppConstant.DEBUG_DATA_RES_KEY) as DebugProcessModel
            val version = intent.getStringExtra(AppConstant.CAPSULE_VERSION)
            binding.tvUsername.text =
                "Device Name: ${intent.getStringExtra(AppConstant.DEVICE_NAME)}"
            val capsuleStatus = mutableListOf(
                CapsuleStatusModel(ctStatus = "Version", ctValue = getVersion(version!!)),
                CapsuleStatusModel(ctStatus = "Battery Level", ctValue = "${String.format("%.3f",debugDataRes.bl)}" + " V"),
                CapsuleStatusModel(ctStatus = "Contact Tracing", ctValue = debugDataRes.cts),
                CapsuleStatusModel(ctStatus = "Proximity Alarm Function", ctValue = debugDataRes.pa),
                CapsuleStatusModel(ctStatus = "User Onbording", ctValue = debugDataRes.uob),
                CapsuleStatusModel(ctStatus = "Timestamp", ctValue = debugDataRes.ts)
            )
            debugProcessAdapter.addDataList(DebugProcessSealed.CapsuleStatus(capsuleStatusList = capsuleStatus))
            debugProcessAdapter.addDataList(DebugProcessSealed.CapsuleAOs(capsuleAOList = debugDataRes.capsuleOAs))

            handleButton(debugDataRes.ct)
        }
    }

    private fun getVersion (capsuleVersion: String): String {
        val first = Long.parseLong(capsuleVersion.substring(0, 2), 16)
        val second = Long.parseLong(capsuleVersion.substring(2, 4), 16)
        val third = Long.parseLong(capsuleVersion.substring(4, 6), 16)
        return "$first.$second.$third"
    }

    private fun handleButton(ctStatus: Boolean) {
        binding.btnUpdateCt.background =
            if (ctStatus) ContextCompat.getDrawable(this, R.drawable.selector_disable_ct)
            else ContextCompat.getDrawable(this, R.drawable.selector_save_qr)
        binding.btnUpdateCt.text =
            if (ctStatus) resources.getString(R.string.disable_ct)
            else resources.getString(R.string.enable_ct)
        mCTStatus = ctStatus

        binding.btnUpdateCt.setOnClickListener {
            showLoading("Loading: Update CT...")
            isUpdatedCT = true
            mCTStatus = !mCTStatus
            debugProcessAPI.updateCTRequest(mCTStatus)
        }

        binding.btnClose.setOnClickListener {
            showLoading("Loading: Update CT...")
            debugProcessAPI.closeUpdateCT()
        }
    }

    private val intentFilter: IntentFilter
        get() {
            val intentAction = IntentFilter()
            intentAction.addAction(BROADCAST_INTERRUPT)

            intentAction.addAction(DebugProcessBRAction.ACT_TIMEOUT_UPDATE_CT)
            intentAction.addAction(DebugProcessBRAction.ACT_UPDATE_CT_RES)
            intentAction.addAction(DebugProcessBRAction.ACT_DEBUG_PROCESS_COMPLETED)
            return intentAction
        }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                BROADCAST_INTERRUPT -> commonAO!!.aoRunScheduler()

                DebugProcessBRAction.ACT_TIMEOUT_UPDATE_CT -> {
                    hideLoading()
                    val requestFailureModel = intent.getSerializableExtra(AppConstant.CAPSULE_FAILURE_KEY) as RequestFailureModel
                    showFailedDialogFragment(DialogEnum.UPDATE_CT_FAILED.type, requestFailureModel.errorCode){
                        finish()
                        exitProcess(0)
                    }
                }
                DebugProcessBRAction.ACT_UPDATE_CT_RES -> {
                    hideLoading()
                    binding.btnUpdateCt.background = if (mCTStatus) ContextCompat.getDrawable(this@DebugProcessActivity, R.drawable.selector_disable_ct)
                    else ContextCompat.getDrawable(this@DebugProcessActivity, R.drawable.selector_save_qr)
                    binding.btnUpdateCt.text = if (mCTStatus) resources.getString(R.string.disable_ct)
                    else resources.getString(R.string.enable_ct)
                }
                DebugProcessBRAction.ACT_DEBUG_PROCESS_COMPLETED -> {
                    hideLoading()
                    if(intent.extras!!.containsKey(AppConstant.COMPLETE_TYPE_KEY)){
                        if(intent.getIntExtra(AppConstant.COMPLETE_TYPE_KEY, 0) == 1) {
                            startPreviousActivity(DiscoverActivity::class.java, true)
                        }
                    }
                }

            }
        }
    }
}