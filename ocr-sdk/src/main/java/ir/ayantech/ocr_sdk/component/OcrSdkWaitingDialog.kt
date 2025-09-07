package ir.ayantech.ocr_sdk.component

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import ir.ayantech.ocr_sdk.databinding.OcrDialogWaitingBinding

/**
 * Dialog ساده برای نمایش حالت انتظار + پیام دلخواه.
 * اگر ProgressBar/درصد توی layout نداریم، درصد به متن چسبانده می‌شود.
 */
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

    /** بستن امن */
    fun hideDialog() {
        if (isShowing) dismiss()
        lastPercent = -1
    }

    /** فقط متن */
    fun changeText(value: String) {
        title = value
        binding.tvTitle.text = value
    }

    /** متن + درصد (اگر ProgressBar نداری، درصد را به متن می‌چسبانیم) */
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
