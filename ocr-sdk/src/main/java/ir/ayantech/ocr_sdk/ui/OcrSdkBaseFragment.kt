package ir.ayantech.ocr_sdk

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import ir.ayantech.ocr_sdk.component.init
import ir.ayantech.ocr_sdk.databinding.OcrFragmentCameraxBinding
import ir.ayantech.whygoogle.fragment.WhyGoogleFragment


open class OcrBaseFragment : WhyGoogleFragment<OcrFragmentCameraxBinding>() {


    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> OcrFragmentCameraxBinding
        get() = OcrFragmentCameraxBinding::inflate

    //region Initializing..
    val ocrActivity by lazy { requireActivity() as OcrActivity }
    val ayanApi by lazy { ocrActivity.ayanAPI }
    open val showingHeader = true
    open val showingFooter = true
    open val TAG = "OCRLOGS"
    open val icLeftHeaderImageView: Int?
        get() = R.drawable.ocr_ic_arrow
    open val showingRightHeaderIcon = false
    open val isItHomeHeader = false
    //endregion

    open fun dispatchKeyEvent(event: KeyEvent): Boolean = true

    override fun onCreate() {
        super.onCreate()
        ocrActivity.window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        init()
        viewListeners()
    }

    override fun onBackPressed(): Boolean {
        when (getTopFragment()) {
            is OcrFragmentOcr -> ocrActivity.mFinishActivity()
        }
        return true
    }

    fun showToast(text: String, length: Int = Toast.LENGTH_SHORT) {
        ocrActivity.showToast(text, length)
    }

    open fun init() {
        val txtBlock = ocrActivity.ocrConfig.textBlock
        accessViews {
            headerRl.init(
                title = ocrActivity.getString(R.string.ocr_camera_desc)

            ) {
                ocrActivity.mFinishActivity()
            }
            txtBlock?.let { tBlock ->
                tBlock.title?.let { title ->
                    headerRl.tvTitle.text = title
                }
                tBlock.firstImageHolderText?.let { tv1 ->
                    tvDescA.text = tv1
                }
                tBlock.secondImageHolderText?.let { tv2 ->
                    tvDescB.text = tv2
                }
                tBlock.buttonText?.let { btnText ->
                    btnSendImages.text = btnText
                }
            }
        }

    }

    open fun viewListeners() {}
    open fun callingApi(endPointName: String, value: String? = null) {}

}