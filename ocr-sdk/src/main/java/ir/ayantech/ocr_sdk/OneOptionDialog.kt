package ir.ayantech.ocr_sdk

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import ir.ayantech.ayannetworking.api.SimpleCallback
import ir.ayantech.ocr_sdk.component.AyanDialog
import ir.ayantech.ocr_sdk.databinding.OcrDialogOneOptionBinding
import ir.ayantech.whygoogle.helper.makeGone

class OneOptionDialog(
    context: Context,
    private val title: String,
    private val buttonText: String,
    private val icon: Int = R.drawable.ocr_ic_camera,
    private val isItForce: Boolean = false,
    private val onButtonClicked: SimpleCallback
) : AyanDialog<OcrDialogOneOptionBinding>(context) {

    override val binder: (LayoutInflater) -> OcrDialogOneOptionBinding
        get() = OcrDialogOneOptionBinding::inflate

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
            btnConfirm.setOnClickListener {
                dismiss()
                onButtonClicked()

            }
        }
    }

    private fun initViews() {
        if (isItForce) {
            binding.closeIv.makeGone()
            setCancelable(false)
        }
        binding.tvTitle.text = title
        binding.btnConfirm.text = buttonText
        binding.centerIconIv.setImageDrawable(ContextCompat.getDrawable(context, icon))
    }
}