package com.ethernom.maintenance

import android.app.Application
import com.ethernom.maintenance.ao.cm.CmAPI

class MainApplication: Application() {

    var cmAPI: CmAPI? = null

    var foo: String? =null

    override fun onCreate() {
        super.onCreate()
    }

}