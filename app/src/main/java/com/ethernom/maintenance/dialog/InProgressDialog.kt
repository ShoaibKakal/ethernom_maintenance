package com.ethernom.maintenance.dialog

import android.view.ViewGroup
import com.ethernom.maintenance.base.BaseDialog
import com.ethernom.maintenance.databinding.DialogInProgressBinding
import com.ethernom.maintenance.model.DialogEnum
import com.ethernom.maintenance.utils.AppConstant

class InProgressDialog: BaseDialog<DialogInProgressBinding>() {
    override fun getViewBinding(container: ViewGroup?, attachToRoot: Boolean): DialogInProgressBinding {
        return DialogInProgressBinding.inflate(layoutInflater, container, false)
    }

    override fun initView() {
        if (arguments != null) {
            val dialogModel: DialogEnum.DialogModel =
                when (requireArguments().getByte(AppConstant.DIALOG_TYPE)) {
                    DialogEnum.RESET_PROGRESS.type -> DialogEnum.RESET_PROGRESS.dialogContent(requireContext())
                    DialogEnum.DEBUG_PROGRESS.type -> DialogEnum.DEBUG_PROGRESS.dialogContent(requireContext())
                    DialogEnum.QR_PROGRESS.type -> DialogEnum.QR_PROGRESS.dialogContent(requireContext())
                    else -> throw IllegalArgumentException("Invalid dialog type!!!")
                }
            binding!!.apply {
                this.title.text = dialogModel.title
                this.content.text = dialogModel.content
            }
        }
    }

}