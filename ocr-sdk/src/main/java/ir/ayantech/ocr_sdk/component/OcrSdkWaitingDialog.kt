package ir.ayantech.ocr_sdk.component

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import ir.ayantech.ocr_sdk.databinding.OcrDialogWaitingBinding

class OcrSdkWaitingDialog(
    context: Context,
    private var title: String
) : OcrSdkAyanDialog<OcrDialogWaitingBinding>(context) {

    private var lastPercent: Int = -1

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
            closeIv.setOnClickListener { dismiss() }
        }
    }

    private fun initViews() {
        binding.tvTitle.text = title
    }

    fun showDialog(initialMessage: String? = null) {
        if (!isShowing) show()
        initialMessage?.let { changeText(it) }
    }

    fun hideDialog() {
        if (isShowing) dismiss()
        lastPercent = -1
    }

    fun changeText(value: String) {
        title = value
        binding.tvTitle.text = value
    }

    fun update(message: String, percent: Int? = null) {
        if (percent == null || percent < 0) {
            changeText(message)
            return
        }
        if (percent != lastPercent) {
            lastPercent = percent.coerceIn(0, 100)
            changeText("$message  $lastPercent%")
        }
    }
}
