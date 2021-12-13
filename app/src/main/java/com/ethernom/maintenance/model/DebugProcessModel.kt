package com.ethernom.maintenance.model

import java.io.Serializable

data class CapsuleOAModel(val ao: String, val cs: String, val event: Long) : Serializable

data class DebugProcessModel(
    val bl: Float,
    val ct: Boolean,
    val cts: String,
    val pa:  String,
    val uob: String,
    val ts: String,
    val capsuleOAs: MutableList<CapsuleOAModel>
) : Serializable

sealed class DebugProcessSealed{
    data class CapsuleStatus(val capsuleStatusList: MutableList<CapsuleStatusModel>): DebugProcessSealed()
    data class CapsuleAOs(val capsuleAOList: MutableList<CapsuleOAModel>): DebugProcessSealed()
}