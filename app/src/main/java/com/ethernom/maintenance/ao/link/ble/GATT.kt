package com.ethernom.maintenance.ao.link.ble

import android.bluetooth.*
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

@RequiresApi(Build.VERSION_CODES.M)
class GATT(ctx: Context) {
    private val intent = Intent(BROADCAST_INTERRUPT)

    private val ETH_SERVICE_DISCOVER_UUID = "19490000-5537-4f5e-99ca-290f4fbff142"
    private val ETH_NEW_SERVICE_UUID = UUID.fromString("19500001-5537-4f5e-99ca-290f4fbff142")
    private val ETH_NEW_RX_CHARACTERISTIC_UUID = UUID.fromString("19500002-5537-4f5e-99ca-290f4fbff142")
    private val ETH_NEW_TX_CHARACTERISTIC_UUID = UUID.fromString("19500003-5537-4f5e-99ca-290f4fbff142")
    private val ETH_CHARACTERISTIC_CONFIG_UUID: UUID = Conversion.convertFromInteger(0x2902)!!

    private var context: Context = ctx
    private var manager: BluetoothManager = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
    private var adapter: BluetoothAdapter = manager.adapter
    private var scanner: BluetoothLeScanner = adapter.bluetoothLeScanner
    private var gatt: BluetoothGatt? = null
    private var gattService: BluetoothGattService? = null
    private var gattRXCharacteristic: BluetoothGattCharacteristic? = null
    private var gattTXCharacteristic: BluetoothGattCharacteristic? = null
    private var gattDescriptor: BluetoothGattDescriptor? = null
    private var ld: SrvDesc? = null
    private var scanCallback: ScanCallback? = null
    private var writeResponse: WriteResponse? = null


    fun scan(lD: SrvDesc) {
        ld = lD
        val scanFilters: MutableList<ScanFilter> = ArrayList()
        val settings = ScanSettings.Builder().build() // Default setting is active scanning
        val scanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(ETH_SERVICE_DISCOVER_UUID)).build() // Filter only Ethernom discover UUID
        scanFilters.add(scanFilter)
        // SCAN CALLBACK
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (result.scanRecord?.manufacturerSpecificData == null) return
                if (result.scanRecord?.serviceUuids == null) return
                onAdvertising(result)
            }
        }
        scanner.startScan(scanFilters, settings, scanCallback)
    }

    fun stopScan() {
        scanner.stopScan(scanCallback)
    }

    fun connect(lD: SrvDesc) {
        ld = lD
        gatt = ld?.ld?.ble?.connectGatt(context, false, gattCallback)
    }

    fun send(lD: SrvDesc, data: ByteArray, writeResponse: WriteResponse) {
        this.writeResponse = writeResponse
        ld = lD
        try {
            write(data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun write(data: ByteArray): Boolean {
        return try {
            log("Data Write ${data.hexa()}")
            gattRXCharacteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT // Write with response
            gattRXCharacteristic?.value = data
            if (!gatt?.writeCharacteristic(gattRXCharacteristic)!!) {
                log("Error on doWrite")
                return false
            }
            true
        } catch (ex: Exception) {
            log("Something went wrong" + ex.message)
            false
        }
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        // For Bluetooth Gatt Call back method
        @RequiresApi(api = Build.VERSION_CODES.O)
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, state: Int) {
            if (state == BluetoothGatt.STATE_CONNECTED) {
                log("gatt connected, discover services")
                gatt = g
                g.discoverServices()
            }
            if (state == BluetoothGatt.STATE_DISCONNECTED) {
                log("gat disconnected")
                gatt?.close()
                gatt = null
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {

            gattService = g.getService(ETH_NEW_SERVICE_UUID)
            if (gattService == null) return log("Service not found")

            gattRXCharacteristic = gattService?.getCharacteristic(ETH_NEW_RX_CHARACTERISTIC_UUID)
            if (gattRXCharacteristic == null) return log("RX Characteristic not found")

            gattTXCharacteristic = gattService?.getCharacteristic(ETH_NEW_TX_CHARACTERISTIC_UUID)
            if (gattTXCharacteristic == null) return log("TX Characteristic not found")

            gattDescriptor = gattTXCharacteristic?.getDescriptor(ETH_CHARACTERISTIC_CONFIG_UUID)
            if (gattDescriptor == null) return log("Descriptor not found")

            // also have to enable notifications on the peer device by writing
            // to the device’s client characteristic configuration descriptor.
            gattDescriptor?.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            g.setCharacteristicNotification(gattTXCharacteristic, true)
            g.writeDescriptor(gattDescriptor)
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status != 0) return log("Connection fail")
            log("Connection success")
            // Request default MTU
            gatt?.requestMtu(512)
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            log("ATT_MTU : $mtu")
            onConnectionSuccess(mtu)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if(status == 0) {
                if (writeResponse != null) {
                    val tmpWriteResponse: WriteResponse = writeResponse!!
                    writeResponse = null
                    tmpWriteResponse.onWriteCallBack(characteristic?.value)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            log("onCharacteristicChanged called")
            onReadSuccess(characteristic.value)
        }
    }

    fun disconnect() {
        if(gatt == null) return
        try {
            gatt!!.close()
            gatt = null
            log("GATT disconnect successfully.")
        } catch (e: IOException) {
            log("GATT disconnect failed ${e.message}.")

        }
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
        ld!!.ld!!.deviceName = result.scanRecord?.deviceName!!
        ld!!.ld!!.type = LinkType.BLE
        ld!!.ld!!.mfgSN = manufacturerSN.toString()
        ld!!.ld!!.uuid = result.scanRecord!!.serviceUuids[0].toString()
        ld!!.ld!!.ble = result.device

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

    private fun onReadSuccess(value: ByteArray) {
        log("Data resp ${value.hexa()}")

        val eventBuffer = EventBuffer(eventId = LinkEvent.LL_RECEIVE_CB, srvDesc = ld!!, buffer = value)
        commonAO!!.sendEvent(ld!!.aoService.id, eventBuffer)

        // send broadcast interrupt
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun log(str: String) {
        Log.d("GATT", str)
    }
}