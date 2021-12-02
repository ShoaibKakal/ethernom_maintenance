package com.ethernom.maintenance.base

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewbinding.ViewBinding
import cn.pedant.SweetAlert.SweetAlertDialog
import com.ethernom.maintenance.R
import com.ethernom.maintenance.broadcast.LocationBroadcast
import com.ethernom.maintenance.utils.AppConstant.START_ACTIVITY_ANIM_LEFT
import com.ethernom.maintenance.utils.AppConstant.START_ACTIVITY_ANIM_RIGHT
import com.ethernom.maintenance.utils.AppConstant.START_ACTIVITY_ANIM_TOP
import com.ethernom.maintenance.utils.customView.LoadingView
import kotlinx.android.synthetic.main.toolbar_back_press.*
import kotlinx.android.synthetic.main.toolbar_center_title.*
import kotlinx.android.synthetic.main.toolbar_center_title.center_toolbar
import kotlinx.android.synthetic.main.toolbar_center_title.toolbar_title

abstract class BaseActivity<VB: ViewBinding>: AppCompatActivity() {
    lateinit var binding: VB
    var alertDialog: AlertDialog? = null
    private val locationBr: BroadcastReceiver = LocationBroadcast()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = getViewBidingClass()
        setContentView(binding.root)
        initView()

        val filter1 = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
       registerReceiver(locationBr, filter1)
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

    private val mLoadingViewParent: ViewGroup? = null
    private var loadingView: LoadingView? = null

    open fun showLoading(title: String) {
        loadingView = loadingView ?: LoadingView(this).show(this, mLoadingViewParent)
        loadingView!!.setLoadingDescription(title)
    }

    open fun hideLoading() {
        loadingView?.let {
            it.hide()
            loadingView = null
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

    open fun showDialogInProgress(@StringRes title: Int, @StringRes contentText: Int){
        if(alertDialog != null) alertDialog!!.dismiss()
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_in_progress, null)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(false)
        dialogView.findViewById<TextView>(R.id.title).text = getString(title)
            dialogView.findViewById<TextView>(R.id.content).text = getString(contentText)

        alertDialog = dialogBuilder.create()
        alertDialog!!.show()
    }

    open fun showDialogSuccess(@StringRes title: Int, @StringRes contentText: Int, confirmButton: () -> Unit){
        if(alertDialog != null) alertDialog!!.dismiss()
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_sucess, null)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(false)
        dialogView.findViewById<TextView>(R.id.title).text = getString(title)
        dialogView.findViewById<TextView>(R.id.content).text = getString(contentText)
        dialogView.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            confirmButton.invoke()
            alertDialog!!.dismiss()
        }

        alertDialog = dialogBuilder.create()
        alertDialog!!.show()
    }

    open fun showDialogFailed(@StringRes title: Int, @StringRes contentText: Int, confirmButton: () -> Unit){
        if(alertDialog != null) alertDialog!!.dismiss()
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_failed, null)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(false)
        dialogView.findViewById<TextView>(R.id.title).text = getString(title)
        dialogView.findViewById<TextView>(R.id.content).text = getString(contentText)
        dialogView.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            confirmButton.invoke()
            alertDialog!!.dismiss()
        }

        alertDialog = dialogBuilder.create()
        alertDialog!!.show()
    }

    open fun showDialogTimeout(@StringRes title: Int, @StringRes contentText: Int, confirmButton: (Boolean) -> Unit){
        if(alertDialog != null) alertDialog!!.dismiss()
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_timout, null)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(false)
        dialogView.findViewById<TextView>(R.id.title).text = getString(title)
        dialogView.findViewById<TextView>(R.id.content).text = getString(contentText)
        dialogView.findViewById<Button>(R.id.btn_exit).setOnClickListener {
            confirmButton.invoke(false)
            alertDialog!!.dismiss()
        }
        dialogView.findViewById<Button>(R.id.btn_retry).setOnClickListener {
            confirmButton.invoke(true)
        }
        alertDialog = dialogBuilder.create()
        alertDialog!!.show()
    }


}