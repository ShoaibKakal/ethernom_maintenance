package com.ethernom.maintenance.ui

import android.text.method.HideReturnsTransformationMethod
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
                return@setOnClickListener
            }
        }

    }
}