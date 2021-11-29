package com.ethernom.maintenance.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
object FileManager {

    @RequiresApi(Build.VERSION_CODES.M)
    fun checkInternalStoragePermission(context: Context): Boolean{
        return context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    fun requestInternalStoragePermissions(activityCompat: Activity, requestCode: Int){
        ActivityCompat.requestPermissions(activityCompat, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), requestCode);
    }

    fun saveImage(context: Context, image: Bitmap, folderName: String, serialNumber: String): String? {
        var savedImagePath: String? = null
        val imageFileName = "${currentDateTime}_$serialNumber.jpg"
        val storageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString() + "/$folderName"
        )
        var success = true
        if (!storageDir.exists()) {
            success = storageDir.mkdirs()
        }
        if (success) {
            val imageFile = File(storageDir, imageFileName)
            savedImagePath = imageFile.absolutePath
            try {
                val fOut: OutputStream = FileOutputStream(imageFile)
                image.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                fOut.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Add the image to the system gallery
            galleryAddPic(context, savedImagePath)
            Toast.makeText(context, "Image saved to folder `Capsule`.", Toast.LENGTH_LONG).show()
        }
        return savedImagePath
    }

    @SuppressLint("SimpleDateFormat")
    val currentDateTime: String = SimpleDateFormat("yyyyMMdd").format(Date())

    private fun galleryAddPic(mContext: Context, imagePath: String) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val f = File(imagePath)
        val contentUri: Uri = Uri.fromFile(f)
        mediaScanIntent.data = contentUri
        mContext.sendBroadcast(mediaScanIntent)
    }

}