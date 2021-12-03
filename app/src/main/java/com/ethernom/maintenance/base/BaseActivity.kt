package com.ethernom.maintenance.base

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.ethernom.maintenance.R
import com.ethernom.maintenance.utils.AppConstant.START_ACTIVITY_ANIM_LEFT
import com.ethernom.maintenance.utils.AppConstant.START_ACTIVITY_ANIM_RIGHT
import com.ethernom.maintenance.utils.AppConstant.START_ACTIVITY_ANIM_TOP
import com.ethernom.maintenance.utils.Utils
import com.ethernom.maintenance.utils.customView.LoadingView
import kotlinx.android.synthetic.main.toolbar_back_press.*
import kotlinx.android.synthetic.main.toolbar_center_title.center_toolbar
import kotlinx.android.synthetic.main.toolbar_center_title.toolbar_title
import android.net.NetworkInfo

import android.net.ConnectivityManager




abstract class BaseActivity<VB: ViewBinding>: AppCompatActivity() {
    lateinit var binding: VB
    var alertDialog: AlertDialog? = null

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

    open fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    open fun checkAppPermission(): ArrayList<String> {
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

    open fun showSuggestionDialog(@StringRes title: Int, @StringRes contentText: Int, @StringRes confirmText: Int, confirmButton: () -> Unit){
        if(alertDialog != null) alertDialog!!.dismiss()
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_suggestion, null)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(false)
        dialogView.findViewById<TextView>(R.id.title).text = getString(title)
        dialogView.findViewById<TextView>(R.id.content).text = getString(contentText)
        dialogView.findViewById<Button>(R.id.btn_confirm).apply {
            text = getString(confirmText)
            setOnClickListener {
                confirmButton.invoke()
                alertDialog!!.dismiss()
            }
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

    // Broad cast function //
    private var locationReceiverCallback : ((Boolean) -> Unit)? = null
    open fun registerLocationBroadcast(locationCallback: (Boolean)-> Unit) {
        // Register for broadcasts on Location state change
        locationReceiverCallback = locationCallback
        val filterLocation = IntentFilter(LocationManager.MODE_CHANGED_ACTION)
        registerReceiver(locationReceiver, filterLocation)
    }

    open fun unRegisterLocationBroadcast() {
        locationReceiverCallback = null
        unregisterReceiver(locationReceiver)
    }

    private val locationReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            locationReceiverCallback!!.invoke(Utils.isLocationEnabled(context!!))
        }
    }

    private var bluetoothReceiverCallback : ((Int) -> Unit)? = null
    open fun registerBluetoothBroadcast(bluetoothCallback: (Int) -> Unit){
        val filterBLe = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        bluetoothReceiverCallback = bluetoothCallback
        registerReceiver(bluetoothReceiver, filterBLe)
    }

    open fun unRegisterBluetoothReceiver(){
        bluetoothReceiverCallback = null
        unregisterReceiver(bluetoothReceiver)
    }

    private val bluetoothReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent!!.action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                bluetoothReceiverCallback!!.invoke(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR))
            }
        }

    }


}