package com.ethernom.maintenance.utils.customView

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.ethernom.maintenance.R

class LoadingView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    private var txtDescription: TextView? = null
    private lateinit var mActivity: Activity

    fun show(activity: Activity, parent: ViewGroup? = null): LoadingView {
        mActivity = activity

        val layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        layoutParams.setMargins(0, 0, 0, 0)
        parent?.addView(this, layoutParams) ?: run {
            activity.window.decorView
                    .findViewById<ViewGroup>(android.R.id.content)
                    .addView(this, layoutParams)

            activity.window.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }

        return this
    }

    fun hide() {
        removeFromSuperView()
        mActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.dialog_message_loading, this)
        txtDescription = findViewById(R.id.text_description)
    }

    @SuppressLint("SetTextI18n")
    fun setLoadingDescription(description: String) {
        txtDescription!!.text = description
    }
}

fun View.removeFromSuperView() {
    (parent as ViewGroup).removeView(this)
}