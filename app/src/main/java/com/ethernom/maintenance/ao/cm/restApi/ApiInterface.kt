package com.ethernom.maintenance.ao.cm.restApi

import com.ethernom.maintenance.model.LoginRequestBody
import com.ethernom.maintenance.model.LoginResponse
import com.ethernom.maintenance.model.UnregisterResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiInterface {

    // Unregister Certificate
    @POST("/goopledevice/unregister")
    fun unregister(@Query("cert") cert: String): Call<UnregisterResponse>

    // Login Request
    @POST("/credential/login")
    fun loginRequest(@Body loginRequestBody: LoginRequestBody): Call<LoginResponse>
}