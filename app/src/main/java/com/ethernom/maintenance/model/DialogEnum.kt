package com.ethernom.maintenance.model

import android.content.Context
import androidx.annotation.DrawableRes
import com.ethernom.maintenance.R
import com.ethernom.maintenance.errorCode.ErrorCode

enum class DialogEnum(val type: Byte) {
    NETWORK(0x01) {
        override fun dialogContent(context: Context, code: Int?): DialogModel = DialogModel(
            context.resources.getString(R.string.network_title),
            context.resources.getString(R.string.network_msg),
            context.resources.getString(R.string.okay),
            R.drawable.selector_save_qr,
            R.drawable.img_wifi
        )
    },

    ADVERTISE (0x02){
        override fun dialogContent(context: Context, code: Int?): DialogModel = DialogModel(
            context.resources.getString(R.string.advertise_device_title),
            context.resources.getString(R.string.advertise_device_msg),
            context.resources.getString(R.string.okay),
            R.drawable.selector_save_qr,
            R.drawable.img_info
        )
    },

    BLUETOOTH(0x03) {
        override fun dialogContent(context: Context, code: Int?): DialogModel = DialogModel(
            context.resources.getString(R.string.bluetooth_device_title),
            context.resources.getString(R.string.bluetooth_device_turn_on),
            context.resources.getString(R.string.turn_on),
            R.drawable.selector_save_qr,
            R.drawable.ic_ble_off
        )
    },

    RESET_PROGRESS (0x04){
        override fun dialogContent(context: Context, code: Int?): DialogModel = DialogModel(
            context.resources.getString(R.string.capsule_reset_title),
            context.resources.getString(R.string.capsule_reset_in_progress),
        )
    },

    RESET_SUCCESS(0x05) {
        override fun dialogContent(context: Context, code: Int?): DialogModel = DialogModel(
            context.resources.getString(R.string.capsule_reset_title),
            context.resources.getString(R.string.capsule_reset_success),
            context.resources.getString(R.string.okay),
            R.drawable.selector_save_qr,
            R.drawable.img_success
        )
    },

    RESET_FAILED(0x06) {
        override fun dialogContent(context: Context, code: Int?): DialogModel {
            val errMsg = ErrorCode.factoryResetError[code]
            return DialogModel(
                context.resources.getString(R.string.capsule_reset_title) + "\n(Code: $code)",
                context.resources.getString(errMsg!!),
                context.resources.getString(R.string.exit),
                R.drawable.selector_disable_ct,
                R.drawable.img_failed
            )
        }
    },

    DEBUG_PROGRESS(0x07){
        override fun dialogContent(context: Context, code: Int?): DialogModel = DialogModel(
            context.resources.getString(R.string.debug_title),
            context.resources.getString(R.string.debug_in_progress),
        )
    },

    DEBUG_SUCCESS(0x08) {
        override fun dialogContent(context: Context, code: Int?): DialogModel = DialogModel(
            context.resources.getString(R.string.debug_title),
            context.resources.getString(R.string.debug_success),
            context.resources.getString(R.string.okay),
            R.drawable.selector_save_qr,
            R.drawable.img_success
        )
    },

    DEBUG_FAILED (0x09){
        override fun dialogContent(context: Context, code: Int?): DialogModel {
            val errMsg = ErrorCode.debugProcessError[code]
            return DialogModel(
                context.resources.getString(R.string.debug_title) + "\n(Code: $code)",
                context.resources.getString(errMsg!!),
                context.resources.getString(R.string.exit),
                R.drawable.selector_disable_ct,
                R.drawable.img_failed
            )
        }
    },

    UPDATE_CT_FAILED (0x10){
        override fun dialogContent(context: Context, code: Int?): DialogModel {
            val errMsg = ErrorCode.debugProcessError[code]
            return DialogModel(
                context.resources.getString(R.string.debug_title) + "\n(Code: $code)",
                context.resources.getString(errMsg!!),
                context.resources.getString(R.string.turn_on),
                R.drawable.selector_disable_ct,
                R.drawable.img_failed
            )
        }
    },

    QR_PROGRESS (0x11){
        override fun dialogContent(context: Context, code: Int?): DialogModel = DialogModel(
            context.resources.getString(R.string.qr_code_title),
            context.resources.getString(R.string.qr_code_in_progress),
        )
    },

    QR_SUCCESS (0x12){
        override fun dialogContent(context: Context, code: Int?): DialogModel = DialogModel(
            context.resources.getString(R.string.qr_code_title),
            context.resources.getString(R.string.qr_code_success),
            context.resources.getString(R.string.okay),
            R.drawable.selector_save_qr,
            R.drawable.img_success
        )
    },

    QR_FAILED (0x13){
        override fun dialogContent(context: Context, code: Int?): DialogModel {
            val errMsg = ErrorCode.readQRCodeError[code]
            return DialogModel(
                context.resources.getString(R.string.qr_code_title) + "\n(Code: $code)",
                context.resources.getString(errMsg!!),
                context.resources.getString(R.string.exit),
                R.drawable.selector_disable_ct,
                R.drawable.img_failed
            )
        }
    },

    CONNECT_TIMEOUT(0x14) {
        override fun dialogContent(context: Context, code: Int?): DialogModel = DialogModel(
            context.resources.getString(R.string.connection_timeout_title),
            context.resources.getString(R.string.connection_timeout_msg)
        )
    };

    abstract fun dialogContent(context: Context, code: Int? = -1): DialogModel

    data class DialogModel(
        val title: String,
        val content: String,
        val button: String? = "",
        @DrawableRes
        val buttonBackgroundId: Int? = -1,
        @DrawableRes
        val iconId: Int? = - 1,
        val contentCode: Int? = -1
    )
}