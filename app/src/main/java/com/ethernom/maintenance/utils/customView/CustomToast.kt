package com.ethernom.maintenance.utils.customView

import android.app.Activity
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.ethernom.maintenance.R
import com.ethernom.maintenance.utils.COLOR

@Suppress("DEPRECATION")
class CustomToast {
    companion object {
        private lateinit var layoutInflater: LayoutInflater
    }

    fun infoToast(context: Activity, colorType: COLOR, message: String) {
        layoutInflater = LayoutInflater.from(context)
        val layout = layoutInflater.inflate(R.layout.custom_toast, (context).findViewById(R.id.custom_toast_layout))
        val imageView = layout.findViewById<ImageView>(R.id.toastImage)
        val textView = layout.findViewById<TextView>(R.id.toastType)
        var colorID = 0

        when (colorType) {
            COLOR.SUCCESS -> {
                colorID = R.color.colorSuccess
                imageView.setImageResource(R.drawable.ic_success_icon)
                textView.text = context.resources.getString(R.string.toast_success)
            }
            COLOR.WARNING -> {
                colorID = R.color.colorWarning
                imageView.setImageResource(R.drawable.ic_warning)
                textView.text = context.resources.getString(R.string.toast_warning)
            }
            COLOR.DANGER -> {
                colorID = R.color.colorRed_A100
                imageView.setImageResource(R.drawable.ic_fail_icon)
                textView.text = context.resources.getString(R.string.log_in)
            }
        }

        val drawable = ContextCompat.getDrawable(context, R.drawable.toast_round_background)
        drawable?.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, colorID), PorterDuff.Mode.MULTIPLY)
        layout.background = drawable
        val desTextView = layout.findViewById<TextView>(R.id.custom_toast_message)
        desTextView.text = message

        val toast = Toast(context.applicationContext)
        toast.duration = Toast.LENGTH_SHORT
       // toast.setGravity(80, 0, 160)
        toast.setGravity(Gravity.BOTTOM or Gravity.FILL_HORIZONTAL, 0, 0)
        toast.view = layout//setting the view of custom toast layout
        toast.show()
    }
}

