package com.ethernom.maintenance.base

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.ethernom.maintenance.R
import com.ethernom.maintenance.dialog.ConfirmDialog
import com.ethernom.maintenance.dialog.InProgressDialog
import com.ethernom.maintenance.dialog.TimeoutDialog
import com.ethernom.maintenance.model.AppRequestState
import com.ethernom.maintenance.model.DialogEnum
import com.ethernom.maintenance.ui.DebugProcessActivity
import com.ethernom.maintenance.ui.DiscoverActivity
import com.ethernom.maintenance.ui.QRCodeActivity
import com.ethernom.maintenance.utils.AppConstant
import com.ethernom.maintenance.utils.AppConstant.START_ACTIVITY_ANIM_LEFT
import com.ethernom.maintenance.utils.AppConstant.START_ACTIVITY_ANIM_RIGHT
import com.ethernom.maintenance.utils.AppConstant.START_ACTIVITY_ANIM_TOP
import com.ethernom.maintenance.utils.COLOR
import com.ethernom.maintenance.utils.Utils
import com.ethernom.maintenance.utils.customView.CustomToast
import com.ethernom.maintenance.utils.customView.LoadingView
import kotlinx.android.synthetic.main.toolbar_back_press.*
import kotlinx.android.synthetic.main.toolbar_center_title.center_toolbar
import kotlinx.android.synthetic.main.toolbar_center_title.toolbar_title
import java.lang.IllegalArgumentException
import kotlin.system.exitProcess


abstract class BaseActivity<VB: ViewBinding>: AppCompatActivity() {
    lateinit var binding: VB
    var dialogFragment: DialogFragment? = null
    var isBackground: Boolean = false

    private val nextActivityTimeout : Long = 2500
    private var requestStateType: Pair<Byte, Bundle?> = Pair(0x00, null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = getViewBidingClass()
        setContentView(binding.root)
        initView()
    }

    override fun onResume() {
        super.onResume()
        isBackground = false
    }

    override fun onPause() {
        super.onPause()
        isBackground = true
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

    open fun setAppStateRequest(stateType: Byte, bundle: Bundle?){
        requestStateType = Pair(stateType, bundle)
    }

    open fun handleUiAction() {
        if (isBackground) return
        when (requestStateType.first) {
            AppRequestState.ACT_RESET_FAILURE.type -> {
                val errorCode = requestStateType.second!!.getInt(AppConstant.ERROR_CODE)
                showFailedDialogFragment(DialogEnum.RESET_FAILED.type, errorCode) {
                    exitApplication()
                }
            }
            AppRequestState.ACT_RESET_COMPLETED.type -> {
                requestComplete(DialogEnum.RESET_SUCCESS.type ,DiscoverActivity::class.java, false)
            }
            AppRequestState.ACT_DEBUG_PROCESS_FAILURE.type -> {
                val errorCode = requestStateType.second!!.getInt(AppConstant.ERROR_CODE)
                showFailedDialogFragment(DialogEnum.DEBUG_FAILED.type, errorCode) {
                    exitApplication()
                }
            }
            AppRequestState.ACT_DEBUG_PROCESS_DATA_RSP.type -> {
                requestComplete(DialogEnum.DEBUG_SUCCESS.type ,DebugProcessActivity::class.java, true, requestStateType.second)
            }
            AppRequestState.READ_QR_CODE_FAILURE.type -> {
                val errorCode = requestStateType.second!!.getInt(AppConstant.ERROR_CODE)
                showFailedDialogFragment(DialogEnum.QR_FAILED.type, errorCode) {
                    exitApplication()
                }
            }
            AppRequestState.READ_QR_CODE_COMPLETED.type -> {
                requestComplete(DialogEnum.QR_SUCCESS.type, QRCodeActivity::class.java, true, requestStateType.second)
            }
            AppRequestState.ACT_TIMEOUT_UPDATE_CT.type -> {
                val errorCode = requestStateType.second!!.getInt(AppConstant.ERROR_CODE)
                showFailedDialogFragment(DialogEnum.UPDATE_CT_FAILED.type, errorCode){
                    exitApplication()
                }
            }
            AppRequestState.ACT_UPDATE_CT_RES.type -> {

            }
            AppRequestState.ACT_DEBUG_PROCESS_COMPLETED.type -> {
                startPreviousActivity(DiscoverActivity::class.java, true)
            }
            AppRequestState.ACT_LOGIN_FAILURE.type -> {
                val errMsg = requestStateType.second!!.getString(AppConstant.ERROR_CODE)
                CustomToast().infoToast(this, COLOR.DANGER, errMsg!!)

            }
            AppRequestState.ACT_LOGIN_COMPLETE.type -> {
                startNextActivity(DiscoverActivity::class.java, true)
            }

            else -> {
                Log.d("tag", "Invalid App State ${requestStateType.first}")
            }
        }
        requestStateType = Pair(0x00, null)
    }

    private fun requestComplete(dialogType: Byte ,activityClass: Class<out AppCompatActivity?>, isNextActivity: Boolean,bundle: Bundle? = null){
        var isClick = false
        val handler = Handler(Looper.getMainLooper())
        showConfirmDialogFragment(dialogType){
            isClick = true
            removeTimeout(handler,isClick)
            if(isNextActivity) startNextActivity(activityClass, bundle, true)
            if(!isNextActivity) startPreviousActivity(activityClass, bundle, true)
        }

        handler.postDelayed({
            if(!isClick && !isBackground){
                dismissDialogFragment()
                if(isNextActivity) startNextActivity(activityClass, bundle, true)
                if(!isNextActivity) startPreviousActivity(activityClass, bundle, true)
            } else {
                Log.d("tag", "App in background!!!")
            }
        }, nextActivityTimeout)
    }

    private fun removeTimeout(handler: Handler,timeout: Boolean) {
        if(!timeout) return
        handler.removeCallbacksAndMessages(null)
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

    open fun showInProgressDialogFragment(dialogType: Byte){
        if(isBackground) return
        if(dialogFragment != null) dialogFragment!!.dismiss()
        val bundle = Bundle()
        bundle.putByte(AppConstant.DIALOG_TYPE, dialogType)
        dialogFragment = InProgressDialog()
        dialogFragment!!.arguments = bundle
        dialogFragment!!.show(supportFragmentManager.beginTransaction(), null)
    }

    open fun showConfirmDialogFragment(dialogType: Byte, confirmCallback: () -> Unit){
        if(isBackground) return
        if(dialogFragment != null) dialogFragment!!.dismiss()
        val bundle = Bundle()
        bundle.putByte(AppConstant.DIALOG_TYPE, dialogType)
        dialogFragment = ConfirmDialog(confirmCallback)
        dialogFragment!!.arguments = bundle
        dialogFragment!!.show(supportFragmentManager.beginTransaction(), null)
    }

    open fun showFailedDialogFragment(dialogType: Byte, errorCode: Int, confirmCallback: () -> Unit){
        if(isBackground) return
        if(dialogFragment != null) dialogFragment!!.dismiss()
        val bundle = Bundle()
        bundle.putByte(AppConstant.DIALOG_TYPE, dialogType)
        bundle.putInt(AppConstant.ERROR_CODE, errorCode)
        dialogFragment = ConfirmDialog(confirmCallback)
        dialogFragment!!.arguments = bundle
        dialogFragment!!.show(supportFragmentManager.beginTransaction(), null)
    }

    open fun showTimeoutDialogFragment(dialogType: Byte, confirmCallback: (Boolean) -> Unit) {
        if(isBackground) return
        if(dialogFragment != null) dialogFragment!!.dismiss()
        dialogFragment = TimeoutDialog(confirmCallback)
        val bundle = Bundle()
        bundle.putByte(AppConstant.DIALOG_TYPE, dialogType)
        dialogFragment!!.arguments = bundle
        dialogFragment!!.show(supportFragmentManager.beginTransaction(), null)
    }

    open fun dismissDialogFragment(){
        if(isBackground) return
        if(dialogFragment != null) dialogFragment!!.dismiss()
    }

    open fun exitApplication() {
        finish()
        exitProcess(0)
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