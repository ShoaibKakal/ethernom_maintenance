package com.ethernom.maintenance.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ethernom.maintenance.R
import com.ethernom.maintenance.ao.BROADCAST_INTERRUPT
import com.ethernom.maintenance.ao.login.LoginAPI
import com.ethernom.maintenance.ao.login.LoginBRAction
import com.ethernom.maintenance.base.BaseActivity
import com.ethernom.maintenance.databinding.ActivityAuthenticationBinding
import com.ethernom.maintenance.model.AppRequestState
import com.ethernom.maintenance.model.DialogEnum
import com.ethernom.maintenance.model.RequestFailureModel
import com.ethernom.maintenance.utils.AppConstant
import ig.core.android.utils.PasswordTransformationMethod

class AuthenticationActivity: BaseActivity<ActivityAuthenticationBinding>() {
    private lateinit var loginAPI : LoginAPI

    override fun getViewBidingClass(): ActivityAuthenticationBinding {
        return ActivityAuthenticationBinding.inflate(layoutInflater)
    }

    override fun initView() {
        handleHidePassword()
        handleButtonLogin()
        loginAPI = LoginAPI()

        LocalBroadcastManager.getInstance(this).registerReceiver(loginReceiver, intentFilter)
    }

    override fun onResume() {
        super.onResume()
        handleUiAction()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(loginReceiver)
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
        binding.ln.setOnClickListener {
            hideKeyboard(it)
        }
        binding.btnLogin.setOnClickListener {
            hideKeyboard(it)
            if(binding.edUsername.text!!.trim().isEmpty() || binding.edPassword.text!!.trim().isEmpty()) {
                Toast.makeText(this, "Please enter your username and password!", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if(!isNetworkAvailable()){
                showConfirmDialogFragment(DialogEnum.NETWORK.type){}
                return@setOnClickListener
            }

            showLoading("Loading: Login Request...")
            loginAPI.loginRequest(username = binding.edUsername.text!!.trim().toString(), password = binding.edPassword.text!!.trim().toString())
        }

    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private val intentFilter: IntentFilter
        get() {
            val intent = IntentFilter()
            intent.addAction(BROADCAST_INTERRUPT)
            intent.addAction(LoginBRAction.ACT_LOGIN_FAILURE)
            intent.addAction(LoginBRAction.ACT_LOGIN_COMPLETE)
            return intent
        }

    private val loginReceiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            hideLoading()
            when(intent!!.action){
                BROADCAST_INTERRUPT -> {
                    commonAO!!.aoRunScheduler()
                }
                LoginBRAction.ACT_LOGIN_FAILURE -> {
                    val data = intent.getSerializableExtra(AppConstant.LOGIN_FAILED) as RequestFailureModel
                    val bundle = Bundle()
                    bundle.putString(AppConstant.ERROR_CODE, getString(data.errorMessage))
                    if(data.errorCode == 2){
                        setAppStateRequest(AppRequestState.ACT_CONNECTION_FAILED.type, null)
                    } else {
                        binding.edUsername.setText("")
                        binding.edUsername.requestFocus()
                        binding.edPassword.setText("")
                        setAppStateRequest(AppRequestState.ACT_LOGIN_FAILURE.type, bundle)
                    }
                    handleUiAction()
                }
                LoginBRAction.ACT_LOGIN_COMPLETE -> {
                    setAppStateRequest(AppRequestState.ACT_LOGIN_COMPLETE.type, null)
                    handleUiAction()
                }
            }
        }
    }
}