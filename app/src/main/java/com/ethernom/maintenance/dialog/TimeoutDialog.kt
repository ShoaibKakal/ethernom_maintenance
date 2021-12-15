package com.ethernom.maintenance.dialog

import android.view.ViewGroup
import com.ethernom.maintenance.base.BaseDialog
import com.ethernom.maintenance.databinding.*
import com.ethernom.maintenance.model.DialogEnum
import com.ethernom.maintenance.utils.AppConstant

class TimeoutDialog(private val confirmCallBack: (Boolean) -> Unit): BaseDialog<DialogTimoutBinding>() {
    override fun getViewBinding(container: ViewGroup?, attachToRoot: Boolean): DialogTimoutBinding {
        return DialogTimoutBinding.inflate(layoutInflater, container, false)
    }

    override fun initView() {
        if (arguments != null) {
            val dialogModel: DialogEnum.DialogModel =
                when (requireArguments().getByte(AppConstant.DIALOG_TYPE)) {
                    DialogEnum.CONNECT_TIMEOUT.type -> DialogEnum.CONNECT_TIMEOUT.dialogContent(requireContext())
                    else -> throw IllegalArgumentException("Invalid dialog type!!!")
                }
            binding!!.apply {
                this.title.text = dialogModel.title
                this.content.text = dialogModel.content
                this.btnExit.setOnClickListener {
                    confirmCallBack.invoke(false)
                    dismiss()
                }
                this.btnRetry.setOnClickListener {
                    confirmCallBack.invoke(true)
                    dismiss()
                }
            }
        }
    }

}