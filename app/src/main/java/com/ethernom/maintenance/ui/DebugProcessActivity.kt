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
import com.ethernom.maintenance.ao.debugProcess.DebugProcessAPI
import com.ethernom.maintenance.ao.debugProcess.DebugProcessBRAction
import com.ethernom.maintenance.base.BaseActivity
import com.ethernom.maintenance.databinding.ActivityDebugProcessBinding
import com.ethernom.maintenance.model.CapsuleOAModel
import com.ethernom.maintenance.model.DebugProcessModel
import com.ethernom.maintenance.utils.AppConstant
import com.ethernom.maintenance.utils.session.ApplicationSession
import kotlin.system.exitProcess

class DebugProcessActivity : BaseActivity<ActivityDebugProcessBinding>() {

    private lateinit var mDebugProcessAdapter: DebugProcessAdapter
    private lateinit var debugProcessAPI: DebugProcessAPI
    private var isUpdatedCT: Boolean = false
    private var mCTStatus: Boolean =false

    override fun getViewBidingClass(): ActivityDebugProcessBinding {
        return ActivityDebugProcessBinding.inflate(layoutInflater)
    }

    override fun initView() {
        debugProcessAPI = DebugProcessAPI()
        showToolbarBackPress(R.string.debug_toolbar)
        initRecyclerView(mutableListOf())
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    override fun onBackPressed() {
        startPreviousActivity(DiscoverActivity::class.java, true)
    }


    private fun initRecyclerView(capsuleOAs: MutableList<CapsuleOAModel>) {
        mDebugProcessAdapter = DebugProcessAdapter(this, capsuleOAs)
        binding.rcvDebugProcess.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@DebugProcessActivity)
            adapter = mDebugProcessAdapter
        }

        if (intent.extras!!.containsKey(AppConstant.DEBUG_DATA_RES_KEY)) {
            val debugDataRes: DebugProcessModel =
                intent.getSerializableExtra(AppConstant.DEBUG_DATA_RES_KEY) as DebugProcessModel
            mDebugProcessAdapter.addAOs(debugDataRes.capsuleOAs)
            binding.tvUsername.text =
                "Device Name: ${ApplicationSession.getInstance(this).getDeviceName()}"
            binding.txtBatterLevel.text =
                resources.getString(R.string.battery_level) + " ${debugDataRes.bl}" + "V"
            binding.btnUpdateCt.background = if (debugDataRes.ctStatus) ContextCompat.getDrawable(this, R.drawable.selector_disable_ct)
            else ContextCompat.getDrawable(this, R.drawable.selector_save_qr)
            binding.btnUpdateCt.text = if (debugDataRes.ctStatus) resources.getString(R.string.disable_ct)
            else resources.getString(R.string.enable_ct)
            mCTStatus = debugDataRes.ctStatus

            handleButton(debugDataRes.ctStatus)
        }
    }

    private fun handleButton(ctStatus: Boolean) {
        binding.btnUpdateCt.setOnClickListener {
            showLoading("Loading: Update CT...")
            isUpdatedCT = true
            mCTStatus = !mCTStatus
            debugProcessAPI.updateCTRequest(mCTStatus)
        }

        binding.btnClose.setOnClickListener {
            if(isUpdatedCT) {
                startPreviousActivity(DiscoverActivity::class.java, true)
            }
            showLoading("Loading: Update CT...")
            debugProcessAPI.closeUpdateCT()
        }
    }

    private val intentFilter: IntentFilter
        get() {
            val intentAction = IntentFilter()
            intentAction.addAction(DebugProcessBRAction.ACT_TIMEOUT_UPDATE_CT)
            intentAction.addAction(DebugProcessBRAction.ACT_UPDATE_CT_RES)
            intentAction.addAction(DebugProcessBRAction.ACT_DEBUG_PROCESS_COMPLETED)
            return intentAction
        }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                DebugProcessBRAction.ACT_TIMEOUT_UPDATE_CT -> {
                    hideLoading()
                    showDialogFailed(R.string.debug_title,R.string.debug_update_ct){
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