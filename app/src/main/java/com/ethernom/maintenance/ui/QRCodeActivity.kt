package com.ethernom.maintenance.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.annotation.RequiresApi
import com.ethernom.maintenance.R
import com.ethernom.maintenance.base.BaseActivity
import com.ethernom.maintenance.databinding.ActivityQrcodeBinding
import com.ethernom.maintenance.utils.AppConstant
import com.ethernom.maintenance.utils.FileManager

@RequiresApi(Build.VERSION_CODES.M)
class QRCodeActivity : BaseActivity<ActivityQrcodeBinding>(){

    private var bitmap: Bitmap? = null
    private var qrgEncoder: QRGEncoder? = null
    private var deviceCSN = ""
    private val REQUEST_INTERNAL_STORAGE_CODE = 111

    override fun getViewBidingClass(): ActivityQrcodeBinding {
        return ActivityQrcodeBinding.inflate(layoutInflater)
    }

    override fun initView() {
        showToolbarBackPress(R.string.qr_code_toolbar)
        if(intent.extras!!.containsKey(AppConstant.DEVICE_KEY)) {
            val deviceName = intent.getStringExtra(AppConstant.DEVICE_KEY)
            val sn = intent.getStringExtra(AppConstant.SERIAL_NUMBER_KEY)
            val data = "$deviceName,${sn!!.lowercase()}"
            binding.tvDeviceName.text = "Device Name:\n$deviceName"
            generateQR(data)
            handleButton()
        }
    }

    private fun generateQR(messageText: String) {
        val manager = getSystemService(WINDOW_SERVICE) as WindowManager
        val display = manager.defaultDisplay
        val point = Point()
        display.getSize(point)

        val width: Int = point.x
        val height: Int = point.y

        var dimen = if (width < height) width else height
        dimen = dimen * 3 / 5

        qrgEncoder = QRGEncoder(messageText, null, QRGContents.Type.TEXT, dimen)
        bitmap = qrgEncoder!!.encodeAsBitmap()
        binding.imageQR.setImageBitmap(bitmap)
    }

    private fun handleButton(){
        binding.btnSaveQr.setOnClickListener {
            if (bitmap == null) return@setOnClickListener
            if (FileManager.checkInternalStoragePermission(this)){
                if(!FileManager.saveImage(this, bitmap!!, "Capsule", deviceCSN).isNullOrEmpty()){
                    binding.btnShowQr.visibility = View.VISIBLE
                }
            }
            else {
                FileManager.requestInternalStoragePermissions(this, REQUEST_INTERNAL_STORAGE_CODE)
            }
        }

        binding.btnShowQr.setOnClickListener {
            openFolder()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_INTERNAL_STORAGE_CODE && hasAllPermissionsGranted(grantResults)){
            if(!FileManager.saveImage(this, bitmap!!, "Capsule", deviceCSN).isNullOrEmpty()){
                binding.btnShowQr.visibility = View.VISIBLE
            }
        }
    }

    private fun hasAllPermissionsGranted(grantResults: IntArray): Boolean {
        for (grantResult in grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "PERMISSION_DENIED", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun openFolder(folderName: String = "Capsule") {
        val selectedUri: Uri = Uri.parse(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString() + "/$folderName")
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setDataAndType(selectedUri, "*/*")
        startActivity(Intent.createChooser(intent, "Open folder"))

    }

    override fun onBackPressed() {
        startPreviousActivity(DiscoverActivity::class.java, true)
    }


}