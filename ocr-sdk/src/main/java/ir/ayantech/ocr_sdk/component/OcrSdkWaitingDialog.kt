package ir.ayantech.ocr_sdk.component

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import ir.ayantech.ocr_sdk.databinding.OcrDialogWaitingBinding

class WaitingDialog(
    context: Context,
    private val title: String,
) : AyanDialog<OcrDialogWaitingBinding>(context) {

    fun showDialog() {
        if (isShowing) return
        else show()

    }

    fun hideDialog() {
        if (isShowing) dismiss()
    }

    init {
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }

    override val binder: (LayoutInflater) -> OcrDialogWaitingBinding
        get() = OcrDialogWaitingBinding::inflate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViews()
        setupActions()
    }

    private fun setupActions() {
        binding.apply {
            closeIv.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun initViews() {
        binding.tvTitle.text = title

    }

    fun changeText(value: String) {
        binding.tvTitle.text = value

    }
}