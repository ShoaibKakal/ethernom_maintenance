package com.ethernom.maintenance.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ethernom.maintenance.R
import com.ethernom.maintenance.ao.CommonAO
import com.ethernom.maintenance.ao.cm.CmAPI
import com.ethernom.maintenance.databinding.ActivitySplashBinding


var commonAO: CommonAO? = null
var cmAPI: CmAPI? = null
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var mBinding : ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        mBinding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initObject()
        val checkPermission = checkAppPermission()
        if(checkPermission.isEmpty()){
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToNextPage()
            }, 3000)
        } else {
            requestPermission(checkPermission.toTypedArray())
        }

        // get app version name
        val versionName: String = this.packageManager.getPackageInfo(this.packageName, 0).versionName
        mBinding.appVersion.text = resources.getString(R.string.app_version) + " " + versionName
    }

    private fun initObject() {
        commonAO = CommonAO(this)
        cmAPI = CmAPI(this)
        commonAO!!.aoRunScheduler()
    }

    private fun navigateToNextPage() {
        val intent = Intent(this, AuthenticationActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Permission *
     */
    private fun checkAppPermission(): ArrayList<String> {
        val appPermissions : ArrayList<String> = ArrayList()
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            appPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            appPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            appPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        return appPermissions
    }

    private fun requestPermission(appPermissions: Array<String>) {
        requestMultiplePermissions.launch(appPermissions)
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    it.key == Manifest.permission.BLUETOOTH_CONNECT) {
                    navigateToNextPage()
                } else if(it.key == Manifest.permission.ACCESS_FINE_LOCATION){
                    navigateToNextPage()
                }
            }
        }
}