package com.ethernom.maintenance.base

import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding

abstract class BaseDialog<VB: ViewBinding>: DialogFragment() {
    lateinit var binding: VB


}