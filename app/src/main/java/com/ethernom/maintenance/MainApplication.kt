package com.ethernom.maintenance

import android.app.Application
import com.ethernom.maintenance.ao.CommonAO
import com.ethernom.maintenance.ao.cm.CmAPI
import com.ethernom.maintenance.ao.select.SelectAPI

class MainApplication: Application() {

    var commonAO: CommonAO? = null
    var selectAPI: SelectAPI? = null
    var cmAPI: CmAPI? = null

    var foo: String? =null

    override fun onCreate() {
        super.onCreate()
    }

}