package com.ethernom.maintenance.base

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import cn.pedant.SweetAlert.SweetAlertDialog
import com.ethernom.maintenance.R
import com.ethernom.maintenance.utils.AppConstant.START_ACTIVITY_ANIM_LEFT
import com.ethernom.maintenance.utils.AppConstant.START_ACTIVITY_ANIM_RIGHT
import com.ethernom.maintenance.utils.AppConstant.START_ACTIVITY_ANIM_TOP
import kotlinx.android.synthetic.main.toolbar_back_press.*
import kotlinx.android.synthetic.main.toolbar_center_title.*
import kotlinx.android.synthetic.main.toolbar_center_title.center_toolbar
import kotlinx.android.synthetic.main.toolbar_center_title.toolbar_title

abstract class BaseActivity<VB: ViewBinding>: AppCompatActivity() {
    lateinit var binding: VB
    private var sweetAlertDialog: SweetAlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = getViewBidingClass()
        setContentView(binding.root)
        initView()
    }

    abstract fun getViewBidingClass(): VB
    abstract fun initView()


    open fun showToolbar(@StringRes title: Int){
        setSupportActionBar(center_toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        toolbar_title.text = resources.getString(title)
    }

    open fun showToolbarBackPress(@StringRes title: Int){
        setSupportActionBar(center_toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        toolbar_title.text = resources.getString(title)
        btn_toolbar_back.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if(item.itemId == android.R.id.home) {
            onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    open fun startNextActivity(clz: Class<out AppCompatActivity?>, isFinish: Boolean) {
        startNewActivity(clz, START_ACTIVITY_ANIM_RIGHT, isFinish, null)
    }

    open fun startNextActivity(clz: Class<out AppCompatActivity?>, bundle: Bundle?, isFinish: Boolean) {
        startNewActivity(clz, START_ACTIVITY_ANIM_RIGHT, isFinish, bundle)
    }

    open fun startPreviousActivity(clz: Class<out AppCompatActivity?>, isFinish: Boolean) {
        startNewActivity(clz, START_ACTIVITY_ANIM_LEFT, isFinish, null)
    }

    open fun startPreviousActivity(clz: Class<out AppCompatActivity?>, bundle: Bundle?, isFinish: Boolean) {
        startNewActivity(clz, START_ACTIVITY_ANIM_LEFT, isFinish, bundle)
    }

    private fun startNewActivity(clz: Class<out AppCompatActivity?>, amin: Int, isFinish: Boolean, bundle: Bundle?) {
        if (this.javaClass.simpleName != clz.simpleName) {
            val intent = Intent(this, clz)
            if (bundle != null) {
                intent.putExtras(bundle)
            }
            startActivity(intent)
            when (amin) {
                START_ACTIVITY_ANIM_LEFT -> overridePendingTransition(R.anim.activity_left_in, R.anim.activity_left_out)
                START_ACTIVITY_ANIM_RIGHT -> overridePendingTransition(R.anim.activity_right_in, R.anim.activity_right_out)
                START_ACTIVITY_ANIM_TOP -> overridePendingTransition(R.anim.activity_top_in, R.anim.activity_top_out)
            }
            if (isFinish) {
                finish()
            }
        }
    }

    open fun showDialogInProgress(@StringRes title: Int, @StringRes context: Int){
        if(sweetAlertDialog != null) sweetAlertDialog!!.dismissWithAnimation()
        sweetAlertDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
            titleText = getString(title)
            contentText = getString(context)
            contentTextSize = 15
            setCancelable(false)
            show()
        }
    }

    open fun showDialogSuccess(@StringRes title: Int, @StringRes context: Int, confirmButton: () -> Unit){
        if(sweetAlertDialog != null) sweetAlertDialog!!.dismissWithAnimation()
        sweetAlertDialog = SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE).apply {
            titleText = getString(title)
            contentText = getString(context)
            contentTextSize = 15
            confirmText = getString(R.string.okay)
            setCancelable(false)
            setConfirmClickListener {
                dismissWithAnimation()
                confirmButton.invoke()
                sweetAlertDialog = null
            }
            show()
        }
    }

    open fun showDialogFailed(@StringRes title: Int, @StringRes context: Int, confirmButton: () -> Unit){
        if(sweetAlertDialog != null) sweetAlertDialog!!.dismissWithAnimation()
        sweetAlertDialog = SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE).apply {
            titleText = getString(title)
            contentText = getString(context)
            contentTextSize = 15
            confirmText = getString(R.string.exit)
            setCancelable(false)
            setConfirmClickListener {
                dismissWithAnimation()
                confirmButton.invoke()
                sweetAlertDialog = null
            }
            show()
        }
    }


}