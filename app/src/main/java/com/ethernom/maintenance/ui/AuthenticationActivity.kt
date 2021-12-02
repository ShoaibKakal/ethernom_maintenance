package com.ethernom.maintenance.ui

import android.content.DialogInterface
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import android.text.method.HideReturnsTransformationMethod
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.ethernom.maintenance.R
import com.ethernom.maintenance.base.BaseActivity
import com.ethernom.maintenance.databinding.ActivityAuthenticationBinding
import ig.core.android.utils.PasswordTransformationMethod

class AuthenticationActivity: BaseActivity<ActivityAuthenticationBinding>() {
    override fun getViewBidingClass(): ActivityAuthenticationBinding {
        return ActivityAuthenticationBinding.inflate(layoutInflater)
    }

    override fun initView() {
        handleHidePassword()
        handleButtonLogin()
        binding.edUsername.setText("admin")
        binding.edPassword.setText("12345")
    }

    private fun handleHidePassword() {
        var isHide = true
        binding.edPassword.transformationMethod = PasswordTransformationMethod()
        binding.btnHidePassword.setOnClickListener {
            if(isHide) {
                binding.imgHidePassword.setImageResource(R.drawable.ic_eye)
                binding.edPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
            } else {
                binding.imgHidePassword.setImageResource(R.drawable.ic_hide_eye)
                binding.edPassword.transformationMethod = PasswordTransformationMethod()
            }
            binding.edPassword.setSelection(binding.edPassword.text!!.length)
            isHide = !isHide
        }
    }

    private fun handleButtonLogin(){
        binding.btnLogin.setOnClickListener {
            if(binding.edUsername.text!!.trim().isEmpty() || binding.edPassword.text!!.trim().isEmpty()) {
                Toast.makeText(this, "Please enter your username and password!", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            Log.d("fxx", "${binding.edUsername.text!!.trim()}")
            Log.d("fxx", "${binding.edPassword.text!!.trim()}")

//            if(binding.edUsername.text!!.trim() != "admin" || binding.edPassword.text!!.trim() != "12345") {
//                Toast.makeText(this, "Username and Password were incorrect!", Toast.LENGTH_SHORT)
//                    .show()
//                return@setOnClickListener
//            }

            startNextActivity(DiscoverActivity::class.java, true)
        }

    }
}