package com.ethernom.maintenance.dialog

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.ethernom.maintenance.base.BaseDialog
import com.ethernom.maintenance.databinding.DialogConfirmBinding
import com.ethernom.maintenance.model.DialogEnum
import com.ethernom.maintenance.utils.AppConstant

class ConfirmDialog(private val confirmCallback: () -> Unit) : BaseDialog<DialogConfirmBinding>() {
    override fun getViewBinding(
        container: ViewGroup?,
        attachToRoot: Boolean
    ): DialogConfirmBinding {
        return DialogConfirmBinding.inflate(layoutInflater, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        if (arguments != null) {
            val dialogModel: DialogEnum.DialogModel =
                when (requireArguments().getByte(AppConstant.DIALOG_TYPE)) {
                    DialogEnum.NETWORK.type -> DialogEnum.NETWORK.dialogContent(requireContext())
                    DialogEnum.ADVERTISE.type -> DialogEnum.ADVERTISE.dialogContent(requireContext())
                    DialogEnum.BLUETOOTH.type -> DialogEnum.BLUETOOTH.dialogContent(requireContext())

                    DialogEnum.RESET_SUCCESS.type -> DialogEnum.RESET_SUCCESS.dialogContent(requireContext())
                    DialogEnum.RESET_FAILED.type -> {
                        val errorCode = requireArguments().getInt(AppConstant.ERROR_CODE, -1)
                        DialogEnum.RESET_FAILED.dialogContent(requireContext(), errorCode)
                    }

                    DialogEnum.DEBUG_SUCCESS.type -> DialogEnum.DEBUG_SUCCESS.dialogContent(requireContext())
                    DialogEnum.DEBUG_FAILED.type -> {
                        val errorCode = requireArguments().getInt(AppConstant.ERROR_CODE, -1)
                        DialogEnum.DEBUG_FAILED.dialogContent(requireContext(), errorCode)
                    }
                    DialogEnum.UPDATE_CT_FAILED.type -> {
                        val errorCode = requireArguments().getInt(AppConstant.ERROR_CODE, -1)
                        DialogEnum.UPDATE_CT_FAILED.dialogContent(requireContext(), errorCode)
                    }

                    DialogEnum.QR_SUCCESS.type -> DialogEnum.QR_SUCCESS.dialogContent(requireContext())
                    DialogEnum.QR_FAILED.type -> {
                        val errorCode = requireArguments().getInt(AppConstant.ERROR_CODE, -1)
                        DialogEnum.QR_FAILED.dialogContent(requireContext(), errorCode)
                    }

                    else -> throw IllegalArgumentException("Invalid dialog type!!!")
                }
            binding!!.apply {
                this.title.text =
                    if (dialogModel.contentCode == -1) dialogModel.title
                    else "${dialogModel.title}\n(Code: ${dialogModel.contentCode})"
                this.content.text = dialogModel.content
                this.imvLogo.setImageResource(dialogModel.iconId!!)
                this.btnConfirm.text = dialogModel.button
                this.btnConfirm.background =
                    ContextCompat.getDrawable(requireContext(), dialogModel.buttonBackgroundId!!)
                this.btnConfirm.setOnClickListener {
                    confirmCallback.invoke()
                    dismiss()
                }

            }
        }
    }
}