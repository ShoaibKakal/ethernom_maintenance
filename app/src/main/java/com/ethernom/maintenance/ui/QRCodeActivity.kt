package com.ethernom.maintenance.ui

import com.ethernom.maintenance.R
import com.ethernom.maintenance.base.BaseActivity
import com.ethernom.maintenance.databinding.ActivityQrcodeBinding

class QRCodeActivity : BaseActivity<ActivityQrcodeBinding>(){
    override fun getViewBidingClass(): ActivityQrcodeBinding {
        return ActivityQrcodeBinding.inflate(layoutInflater)
    }

    override fun initView() {
        showToolbarBackPress(R.string.qr_code_toolbar)
    }

}