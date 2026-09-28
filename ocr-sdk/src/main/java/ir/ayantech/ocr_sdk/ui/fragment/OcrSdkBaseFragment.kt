package ir.ayantech.ocr_sdk.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import ir.ayantech.ocr_sdk.R
import ir.ayantech.ocr_sdk.component.init
import ir.ayantech.ocr_sdk.data.model.OcrSdkLanguage
import ir.ayantech.ocr_sdk.data.model.OcrSdkTextBlock
import ir.ayantech.ocr_sdk.databinding.OcrFragmentCameraxBinding
import ir.ayantech.ocr_sdk.ui.activity.OcrActivity

open class OcrSdkBaseFragment : Fragment() {

    private var viewBinding: OcrFragmentCameraxBinding? = null
    val binding get() = requireNotNull(viewBinding)

    val ocrActivity: OcrActivity
        get() = requireActivity() as OcrActivity

    open val TAG = "OCRLOGS"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val inflatedBinding = OcrFragmentCameraxBinding.inflate(inflater, container, false)
        viewBinding = inflatedBinding
        return inflatedBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onFragmentCreated()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }

    open fun onFragmentCreated() {
        applyLayoutDirection()
        binding.captureA.circularImg.contentDescription =
            getString(R.string.ocr_camera_description_front)
        binding.captureB.circularImg.contentDescription =
            getString(R.string.ocr_camera_description_back)
        init()
    }

    private fun applyLayoutDirection() {
        val direction = if (ocrActivity.language == OcrSdkLanguage.PERSIAN) {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }
        ocrActivity.window.decorView.layoutDirection = direction
        binding.root.layoutDirection = direction
    }

    fun accessViews(block: OcrFragmentCameraxBinding.() -> Unit) {
        binding.block()
    }

    open fun onBackPressed(): Boolean {
        ocrActivity.mFinishActivity()
        return true
    }

    fun showToast(text: String, length: Int = Toast.LENGTH_SHORT) {
        ocrActivity.showToast(text, length)
    }

    open fun init() {
        binding.apply {
            tvDescA.text = getString(R.string.card_front)
            tvDescB.text = getString(R.string.back_card)
            btnSendImages.text = getString(R.string.ocr_send)
            headerRl.backIv.contentDescription = getString(R.string.ocr_back)
            headerRl.init(getString(R.string.ocr_camera_desc)) {
                ocrActivity.mFinishActivity()
            }
        }
        ocrActivity.ocrConfig.textBlock?.let(::applyTextBlock)
    }

    private fun applyTextBlock(textBlock: OcrSdkTextBlock) {
        binding.apply {
            textBlock.title?.let { headerRl.tvTitle.text = getString(it) }
            textBlock.firstImageHolderText?.let { tvDescA.text = getString(it) }
            textBlock.secondImageHolderText?.let { tvDescB.text = getString(it) }
            textBlock.buttonText?.let { btnSendImages.text = getString(it) }
            textBlock.secondTitle?.let {
                tvVin.text = getString(it)
                tvVin.visibility = View.VISIBLE
            }
        }
    }

}
