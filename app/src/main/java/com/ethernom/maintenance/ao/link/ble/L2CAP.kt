package com.ethernom.maintenance.ao.link.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ethernom.maintenance.MainApplication
import com.ethernom.maintenance.ao.BROADCAST_INTERRUPT
import com.ethernom.maintenance.ao.EventBuffer
import com.ethernom.maintenance.ao.SrvDesc
import com.ethernom.maintenance.ao.link.LinkEvent
import com.ethernom.maintenance.ao.link.LinkType
import com.ethernom.maintenance.ao.link.WriteResponse
import com.ethernom.maintenance.ui.commonAO
import com.ethernom.maintenance.utils.Conversion
import com.ethernom.maintenance.utils.hexa
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

@RequiresApi(Build.VERSION_CODES.Q)
class L2CAP(ctx: Context) {
    private val intent = Intent(BROADCAST_INTERRUPT)

    private var context: Context = ctx
    private var manager: BluetoothManager = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
    private var adapter: BluetoothAdapter = manager.adapter
    private var scanner: BluetoothLeScanner? = null
    private var ld: SrvDesc? = null
    private var scanCallback: ScanCallback? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var writeResponse: WriteResponse? = null

    fun scan(lD: SrvDesc) {
        manager = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
        adapter = manager.adapter

        ld = lD
        val scanFilters: MutableList<ScanFilter> = ArrayList()
        val settings = ScanSettings.Builder().build()
        val scanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(
            ETH_SERVICE_DISCOVER_UUID
        )).build() // Filter only Ethernom discover UUID
        scanFilters.add(scanFilter)
        // SCAN CALLBACK
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (result.scanRecord?.manufacturerSpecificData == null) return
                if (result.scanRecord?.serviceUuids == null) return
                onAdvertising(result)
            }
        }
        scanner = adapter.bluetoothLeScanner
        scanner?.startScan(scanFilters, settings, scanCallback)
    }

    fun stopScan() {
        scanner?.stopScan(scanCallback)
    }

    fun connect(lD: SrvDesc) {
        ld = lD
        Thread{
            if (bluetoothSocket != null) return@Thread
            try {
                bluetoothSocket = ld?.ld?.ble?.createInsecureL2capChannel(128)
                bluetoothSocket!!.connect()

                log("L2CAP connect successfully.")
                onConnectionSuccess(bluetoothSocket!!.maxTransmitPacketSize)
                read()

            }catch (e: IOException) {
                log("L2CAP connect failed ${e.message}.")
                bluetoothSocket = null
            }
        }.start()

    }

    fun disconnect() {
        if(bluetoothSocket == null) return
        try {
            bluetoothSocket!!.close()
            bluetoothSocket = null
            log("L2CAP disconnect successfully.")
        } catch (e: IOException) {
            log("L2CAP disconnect failed ${e.message}.")

        }
    }

    fun send(lD: SrvDesc, data: ByteArray, writeResponse: WriteResponse) {
        this.writeResponse = writeResponse
        ld = lD
        write(data)
    }

    private fun onAdvertising(result: ScanResult) {
        val manufacturer = result.scanRecord!!.manufacturerSpecificData
        val manufacturerSN = StringBuffer()
        for (i in 0 until manufacturer.size()) {
            val mfgData = manufacturer[manufacturer.keyAt(i)] ?: return
            val manufacturerData = Conversion.toUnsignedIntArray(mfgData)
            for (element in manufacturerData) {
                manufacturerSN.append(String.format("%02x", element and 0xff))
            }
        }
        log("onAdvertising ${ld!!.aoService.id} => ${result.scanRecord?.deviceName} - $manufacturerSN")
//         Update Link Descriptor specific data

        val manufacturerFull = manufacturerSN.toString()
        var csn = manufacturerFull.substring(0, 16)
        var version = manufacturerFull.substring(16, 22)

        ld!!.ld!!.deviceName = result.scanRecord?.deviceName!!
        ld!!.ld!!.type = LinkType.BLE
        ld!!.ld!!.mfgSN = manufacturerSN.toString()
        ld!!.ld!!.mfgSN = csn
        ld!!.ld!!.uuid = result.scanRecord!!.serviceUuids[0].toString()
        ld!!.ld!!.ble = result.device
        ld!!.ld!!.version = version

        val eventBuffer = EventBuffer(eventId = LinkEvent.ADV_IND, srvDesc = ld!!)
        commonAO!!.sendEvent(ld!!.aoService.id, eventBuffer)

        // send broadcast interrupt
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun onConnectionSuccess(mtu:Int) {
        ld!!.ld!!.mtu = mtu
        val eventBuffer = EventBuffer(eventId = LinkEvent.CONNECTION_RESPOND, srvDesc = ld!!)
        commonAO!!.sendEvent(ld!!.aoService.id, eventBuffer)

        // send broadcast interrupt
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

    }

    private fun read() {
        Thread {
            try {
                while (true) {
                    val data = ByteArray(2048)
                    val size  = bluetoothSocket!!.inputStream.read(data)
                    if(size == -1) break
                    else if(size != 0)
                        onReadSuccess(data.copyOfRange(0, size))
                }
            } catch (e:IOException) {
                log("L2CAP Read failed ${e.message}.")
                bluetoothSocket = null
            }
        }.start()
    }

    private fun onReadSuccess(value: ByteArray) {
        log("Data resp ${value.hexa()}")

        val eventBuffer = EventBuffer(eventId = LinkEvent.LL_RECEIVE_CB, srvDesc = ld!!, buffer = value)
        commonAO!!.sendEvent(ld!!.aoService.id, eventBuffer)

        // send broadcast interrupt
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun write(data: ByteArray) {
        log("write req: ${data.hexa()}")

        Thread {
            if (bluetoothSocket == null)  return@Thread
            try {
                bluetoothSocket?.outputStream?.write(data)
                writeResponse?.onWriteCallBack(null)
                log("L2CAP write successfully.")

                val eventBuffer = EventBuffer(eventId = LinkEvent.ADV_IND, srvDesc = ld!!)
                commonAO!!.sendEvent(ld!!.aoService.id, eventBuffer)

            } catch (e:IOException) {
                log("L2CAP write failed ${e.message}.")
                writeResponse?.onWriteCallBack(null)
            }
        }.start()

    }

    private fun log(str: String) {
        Log.d("L2CAP", str)
    }

    companion object {
        private const val ETH_SERVICE_DISCOVER_UUID = "19490000-5537-4f5e-99ca-290f4fbff142"
    }
}