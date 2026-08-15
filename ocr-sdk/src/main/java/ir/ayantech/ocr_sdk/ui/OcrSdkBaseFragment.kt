package ir.ayantech.ocr_sdk.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import ir.ayantech.ocr_sdk.R
import ir.ayantech.ocr_sdk.component.init
import ir.ayantech.ocr_sdk.databinding.OcrFragmentCameraxBinding


open class OcrSdkBaseFragment : Fragment() {

    private var _binding: OcrFragmentCameraxBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = OcrFragmentCameraxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onFragmentCreated()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    open fun onFragmentCreated() {
        ocrActivity.window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        init()
        viewListeners()
    }

    fun accessViews(block: OcrFragmentCameraxBinding.() -> Unit) {
        binding.block()
    }

    fun getTopFragment(): Fragment? {
        return ocrActivity.getTopFragment()
    }

    open fun onBackPressed(): Boolean {
        when (getTopFragment()) {
            is OcrSdkOcrFragment -> ocrActivity.mFinishActivity()
        }
        return true
    }

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
                tBlock.secondTitle?.let {
                    tvVin.visibility = View.VISIBLE
                    tvVin.text = it
                }
            }
        }

    }

    open fun viewListeners() {}
    open fun callingApi(endPointName: String, value: String? = null) {}

}
