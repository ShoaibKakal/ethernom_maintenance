package com.ethernom.maintenance.ao.link.ble

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ethernom.maintenance.R
import com.ethernom.maintenance.ao.BROADCAST_INTERRUPT
import com.ethernom.maintenance.ao.EventBuffer
import com.ethernom.maintenance.ao.link.LinkEvent
import com.ethernom.maintenance.dialog.ConfirmDialog
import com.ethernom.maintenance.model.DialogEnum
import com.ethernom.maintenance.ui.commonAO
import com.ethernom.maintenance.utils.AppConstant
import kotlin.system.exitProcess

@RequiresApi(Build.VERSION_CODES.Q)
class BLEDialogActivity : AppCompatActivity() {
    val tag = javaClass.simpleName
    lateinit var bleAlertDialog: AlertDialog
    var status = false
    var aoId = 0
    private val intentIsr = Intent(BROADCAST_INTERRUPT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = intent.getBooleanExtra("BLE_STATUS", false)
        aoId = intent.getIntExtra("AO_ID", 0)

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateChange, filter)

        showBleDialogFragment(DialogEnum.BLUETOOTH.type){
            turnOnBluetooth()
        }
    }

    private fun showBleDialogFragment(dialogType: Byte, confirmCallback: () -> Unit){
        val bundle = Bundle()
        bundle.putByte(AppConstant.DIALOG_TYPE, dialogType)
        val dialogFragment = ConfirmDialog(confirmCallback)
        dialogFragment.arguments = bundle
        dialogFragment.show(supportFragmentManager.beginTransaction(), null)
    }

    private fun turnOnBluetooth(): Boolean {
        val bluetoothAdapter = BluetoothAdapter
            .getDefaultAdapter()
        return bluetoothAdapter?.enable() ?: false
    }

    private val bluetoothStateChange = object : BroadcastReceiver() { // Broadcast receiver callback Bluetooth state change
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(tag, "bluetoothStateChange call")
            if (intent!!.action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

                when (intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )) {
                    BluetoothAdapter.STATE_ON -> {
                        val buff = EventBuffer(eventId = LinkEvent.BLE_ON, buffer = byteArrayOf(0x01), srvDesc = null)
                        commonAO!!.sendEvent(aoId = aoId, buff)

                        // send broadcast interrupt
                        LocalBroadcastManager.getInstance(this@BLEDialogActivity).sendBroadcast(intentIsr)

                        unregisterBroadcastReceiver(context!!)
                        if (status) {
                            finish()
                        } else {
                            finishAffinity()
                            exitProcess(0)
                        }
                    }
                }
            }
        }
    }

    private fun unregisterBroadcastReceiver(ctx: Context) {
        unregisterReceiver(bluetoothStateChange)
    }
}