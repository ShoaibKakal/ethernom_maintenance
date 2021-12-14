package com.ethernom.maintenance.model

data class LoginRequestBody(
    val user: String,
    val pass: String
)

data class LoginResponse(
    val status: String = ""
)
