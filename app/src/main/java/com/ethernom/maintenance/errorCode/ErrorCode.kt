package com.ethernom.maintenance.errorCode

import com.ethernom.maintenance.R

object ErrorCode {
    val factoryResetError: HashMap<Int, Int>
        get() {
            val hashMap = HashMap<Int, Int>()
            hashMap[0] = R.string.capsule_not_onboard
            hashMap[1] = R.string.capsule_not_yet_user_onboard
            hashMap[2] = R.string.capsule_reset_timeout
            hashMap[3] = R.string.capsule_reset_failed
            return hashMap
        }

    val debugProcessError: HashMap<Int, Int>
        get() {
            val hashMap = HashMap<Int, Int>()
            hashMap[0] = R.string.capsule_not_onboard
            hashMap[1] = R.string.capsule_not_yet_user_onboard
            hashMap[2] = R.string.debug_timeout
            hashMap[3] = R.string.debug_failed
            hashMap[4] = R.string.debug_update_ct
            return hashMap
        }

    val readQRCodeError: HashMap<Int, Int>
        get() {
            val hashMap = HashMap<Int, Int>()
            hashMap[0] = R.string.capsule_not_onboard
            hashMap[1] = R.string.capsule_not_yet_user_onboard
            hashMap[2] = R.string.qr_code_timeout
            hashMap[3] = R.string.qr_code_failed
            return hashMap
        }

    val loginError: HashMap<Int, Int>
        get() {
            val hashMap = HashMap<Int, Int>()
            hashMap[0] = R.string.login_failed
            hashMap[1] = R.string.login_timeout
            return hashMap
        }
}