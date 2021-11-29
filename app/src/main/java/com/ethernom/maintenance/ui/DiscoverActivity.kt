package com.ethernom.maintenance.ui

import androidx.recyclerview.widget.LinearLayoutManager
import com.ethernom.maintenance.R
import com.ethernom.maintenance.adapter.DeviceAdapter
import com.ethernom.maintenance.base.BaseActivity
import com.ethernom.maintenance.databinding.ActivityDiscoverBinding
import com.ethernom.maintenance.model.DeviceModel

class DiscoverActivity : BaseActivity<ActivityDiscoverBinding>() {

    private lateinit var mDeviceAdapter: DeviceAdapter
    private var mDevices: MutableList<DeviceModel> = mutableListOf(
        DeviceModel(name = "T1", sn = "000100010000001a"),
        DeviceModel(name = "T2", sn = "000100010000001b"),
        DeviceModel(name = "T3", sn = "000100010000001c"),
        DeviceModel(name = "T4", sn = "000100010000001d"),
    )

    override fun getViewBidingClass(): ActivityDiscoverBinding {
        return ActivityDiscoverBinding.inflate(layoutInflater)
    }

    override fun initView() {
        showToolbar(R.string.discover_toolbar_text)
        initRecyclerView()
    }

    private fun initRecyclerView(){
        mDeviceAdapter = DeviceAdapter(this, mDevices, deviceItemSelected)
        binding.rcvDevice.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@DiscoverActivity)
            adapter = mDeviceAdapter
        }
    }

    private val deviceItemSelected = object : (DeviceModel) -> Unit {
        override fun invoke(device: DeviceModel) {
            startNextActivity(MaintenanceActivity::class.java, true)
        }
    }
}