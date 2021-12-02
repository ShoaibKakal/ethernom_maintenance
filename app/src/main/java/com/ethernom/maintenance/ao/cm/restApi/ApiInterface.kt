package com.ethernom.maintenance.ao.cm.restApi

import com.ethernom.maintenance.model.UnregisterResponse
import retrofit2.Call
import retrofit2.http.*

interface ApiInterface {

    @POST("/goopledevice/unregister") // Test Certificate
    fun unregister(@Query("cert") cert: String): Call<UnregisterResponse>
}