package com.ethernom.maintenance.ui

import android.util.Log
import android.view.View
import com.ethernom.maintenance.R
import com.ethernom.maintenance.base.BaseActivity
import com.ethernom.maintenance.databinding.ActivityAboutBinding
import com.ethernom.maintenance.utils.AppConstant
import com.ethernom.maintenance.utils.hexa
import com.ethernom.maintenance.utils.session.ApplicationSession
import java.lang.Long.parseLong
import java.util.*

class AboutActivity : BaseActivity<ActivityAboutBinding>(){
    private val tag = "AboutTAG"
    override fun getViewBidingClass(): ActivityAboutBinding {
        return ActivityAboutBinding.inflate(layoutInflater)
    }

    override fun initView() {
        showToolbarBackPress(R.string.about_toolbar)
        val versionName: String = this.packageManager.getPackageInfo(this.packageName, 0).versionName
        binding.tvVersion.text = resources.getString(R.string.app_version) + " " + versionName

        if(intent.extras!!.containsKey(AppConstant.CAPSULE_VERSION)){
            val capsuleVersion = intent.getStringExtra(AppConstant.CAPSULE_VERSION)
            if(capsuleVersion!!.length == 6){
                val first = parseLong(capsuleVersion.substring(0, 2), 16)
                val second = parseLong(capsuleVersion.substring(2, 4), 16)
                val third = parseLong(capsuleVersion.substring(4, 6), 16)
                binding.capsuleVersion.text = resources.getString(R.string.capsule_version) + " $first.$second.$third"
                binding.capsuleVersion.visibility = View.VISIBLE

            }

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