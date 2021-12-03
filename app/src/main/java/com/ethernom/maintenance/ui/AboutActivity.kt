package com.ethernom.maintenance.ui

import android.view.View
import com.ethernom.maintenance.R
import com.ethernom.maintenance.base.BaseActivity
import com.ethernom.maintenance.databinding.ActivityAboutBinding
import com.ethernom.maintenance.utils.session.ApplicationSession
import java.util.*

class AboutActivity : BaseActivity<ActivityAboutBinding>(){
    override fun getViewBidingClass(): ActivityAboutBinding {
        return ActivityAboutBinding.inflate(layoutInflater)
    }

    override fun initView() {
        showToolbarBackPress(R.string.about_toolbar)
        val versionName: String = this.packageManager.getPackageInfo(this.packageName, 0).versionName
        binding.tvVersion.text = resources.getString(R.string.app_version) + " " + versionName

        val version = ApplicationSession.getInstance(this).getCapsuleVersion()
        if(version.isNotEmpty()) {
            binding.capsuleVersion.text = resources.getString(R.string.capsule_version) + " " + version
            binding.capsuleVersion.visibility = View.VISIBLE
        }

        val calendar = Calendar.getInstance()
        val year = calendar[Calendar.YEAR]
        binding.copyRight.text = resources.getString(R.string.copyright) + year + " " + resources.getString(R.string.company_name)
        binding.tvVersion.text = resources.getString(R.string.app_version) + " " + versionName
        binding.btnTermsOfService.setOnClickListener{

        }
        binding.btnPrivacy.setOnClickListener {

        }
    }

}