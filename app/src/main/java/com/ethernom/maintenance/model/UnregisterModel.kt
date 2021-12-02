package com.ethernom.maintenance.model

data class UnregisterRequestBody (val cert: String ="")
data class UnregisterResponse (val cert: String = "",val token: Boolean, val status: String = "")