package com.ethernom.maintenance.ui

import androidx.recyclerview.widget.LinearLayoutManager
import com.ethernom.maintenance.R
import com.ethernom.maintenance.adapter.DebugProcessAdapter
import com.ethernom.maintenance.base.BaseActivity
import com.ethernom.maintenance.databinding.ActivityDebugProcessBinding
import com.ethernom.maintenance.model.CapsuleOAModel

class DebugProcessActivity : BaseActivity<ActivityDebugProcessBinding>(){

    private lateinit var mDebugProcessAdapter: DebugProcessAdapter

    override fun getViewBidingClass(): ActivityDebugProcessBinding {
        return ActivityDebugProcessBinding.inflate(layoutInflater)
    }

    override fun initView() {
        showToolbarBackPress(R.string.debug_toolbar)
        initRecyclerView(mutableListOf())
    }

    private fun initRecyclerView(capsuleOAs: MutableList<CapsuleOAModel>) {
        mDebugProcessAdapter = DebugProcessAdapter(this, capsuleOAs)
        binding.rcvDebugProcess.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@DebugProcessActivity)
            adapter = mDebugProcessAdapter
        }
    }

}