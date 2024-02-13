package ir.ayantech.ocr_sdk

import android.graphics.Camera
import android.view.KeyEvent
import android.widget.Toast
import androidx.viewbinding.ViewBinding
import ir.ayantech.whygoogle.fragment.WhyGoogleFragment


abstract class BaseFragment<T : ViewBinding> : WhyGoogleFragment<T>() {

    //region Initializing..
    val ocrActivity by lazy { requireActivity() as OcrActivity }
    val ayanApi by lazy { ocrActivity.ayanAPI }
    open val progressBar by lazy { ocrActivity.binding.progressBar }
    open val showingHeader = true
    open val showingFooter = true
    open val TAG = "OCRLOGS"
    open val icLeftHeaderImageView: Int?
        get() = R.drawable.ocr_ic_arrow_left_header
    open val showingRightHeaderIcon = false
    open val isItHomeHeader = false
    //endregion

    open fun dispatchKeyEvent(event: KeyEvent): Boolean = true

    override fun onCreate() {
        super.onCreate()
        init()
        viewListeners()
    }

    override fun onBackPressed(): Boolean {
            when (getTopFragment()) {
                is CameraXFragment -> ocrActivity.finishActivity()
                is ImageViewFragment -> start(CameraXFragment())
            }
        return true
    }

    fun showToast(text: String, length: Int = Toast.LENGTH_SHORT) {
        ocrActivity.showToast(text, length)
    }

    abstract  fun init()
    abstract fun viewListeners()
    abstract fun callingApi(endPointName: String, value: String? = null)

}