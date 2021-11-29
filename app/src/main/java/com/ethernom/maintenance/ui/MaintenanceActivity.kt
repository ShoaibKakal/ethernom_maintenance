package com.ethernom.maintenance.ui

import androidx.recyclerview.widget.GridLayoutManager
import com.ethernom.maintenance.R
import com.ethernom.maintenance.adapter.MainMenuAdapter
import com.ethernom.maintenance.base.BaseActivity
import com.ethernom.maintenance.databinding.ActivityMaintenanceBinding

class MaintenanceActivity : BaseActivity<ActivityMaintenanceBinding>() {

    override fun getViewBidingClass(): ActivityMaintenanceBinding {
        return ActivityMaintenanceBinding.inflate(layoutInflater)
    }

    override fun initView() {
        showToolbar(R.string.main_toolbar)
        binding.tvUsername.text = "Ethernom Test"
        initRecyclerView()
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
                0 -> {}
                1 -> startNextActivity(DebugProcessActivity::class.java, false)
                2 -> startNextActivity(QRCodeActivity::class.java, false)
                3 -> startNextActivity(AboutActivity::class.java, false)
            }
        }
    }

}