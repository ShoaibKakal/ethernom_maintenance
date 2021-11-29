package com.ethernom.maintenance.ui

import com.ethernom.maintenance.R
import com.ethernom.maintenance.base.BaseActivity
import com.ethernom.maintenance.databinding.ActivityAboutBinding

class AboutActivity : BaseActivity<ActivityAboutBinding>(){
    override fun getViewBidingClass(): ActivityAboutBinding {
        return ActivityAboutBinding.inflate(layoutInflater)
    }

    override fun initView() {
        showToolbarBackPress(R.string.about_toolbar)
    }

}