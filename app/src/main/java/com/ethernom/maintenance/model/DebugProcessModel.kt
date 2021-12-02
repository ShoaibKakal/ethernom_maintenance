package com.ethernom.maintenance.model

import java.io.Serializable

data class CapsuleOAModel(val ao: String, val cs: String, val event: Long) : Serializable

data class DebugProcessModel(val bl: Float,val ctStatus: Boolean,val capsuleOAs: MutableList<CapsuleOAModel>): Serializable