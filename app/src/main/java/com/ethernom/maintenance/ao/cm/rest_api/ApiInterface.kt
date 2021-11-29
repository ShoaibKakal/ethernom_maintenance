package com.ethernom.maintenance.ao.cm.rest_api

import retrofit2.Call
import retrofit2.http.*

interface ApiInterface {

    @GET("/CapsuleCert/get")
    fun getCapsuleCert(): Call<CapsuleCertResponse>

    @POST("/CapsuleCert/verify")
    fun verifyCapsuleCertificate(@Query("Cert") Cert: String): Call<VerifyCapsuleCertResponse>
}