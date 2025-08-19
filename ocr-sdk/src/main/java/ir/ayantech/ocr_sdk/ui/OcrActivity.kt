package ir.ayantech.ocr_sdk.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import ir.ayantech.ayannetworking.api.AyanApi
import ir.ayantech.ayannetworking.api.AyanCommonCallStatus
import ir.ayantech.ayannetworking.api.CallingState
import ir.ayantech.ayannetworking.api.GetUserToken
import ir.ayantech.ocr_sdk.tools.OCRConstant.Base_URL
import ir.ayantech.ocr_sdk.tools.OCRConstant.EndPoint_GetCardOcrResult
import ir.ayantech.ocr_sdk.tools.OCRConstant.EndPoint_UploadCardOCR
import ir.ayantech.ocr_sdk.tools.OCRConstant.Token
import ir.ayantech.ocr_sdk.tools.OcrHelper
import ir.ayantech.ocr_sdk.dialog.OcrSdkOneOptionDialog
import ir.ayantech.ocr_sdk.R
import ir.ayantech.ocr_sdk.component.OcrSdkWaitingDialog
import ir.ayantech.ocr_sdk.databinding.OcrActivityBinding
import ir.ayantech.ocr_sdk.model.OcrSdkCaptureConfig
import ir.ayantech.ocr_sdk.model.OcrSdkGetCardOcrResult
import ir.ayantech.ocr_sdk.model.OcrSdkOcrConfig
import ir.ayantech.ocr_sdk.model.OcrSdkOcrDataResult
import ir.ayantech.ocr_sdk.model.OcrSdkTextBlock
import ir.ayantech.ocr_sdk.model.OcrSdkUriDataResult
import ir.ayantech.whygoogle.activity.WhyGoogleActivity
import ir.ayantech.whygoogle.helper.isNull


open class  OcrActivity : WhyGoogleActivity<OcrActivityBinding>() {

    override val binder: (LayoutInflater) -> OcrActivityBinding
        get() = OcrActivityBinding::inflate
    override val containerId: Int = R.id.fragmentContainerFl

    private var action: String? = null
    var captureConfig: OcrSdkCaptureConfig = OcrSdkCaptureConfig()
    var ocrConfig: OcrSdkOcrConfig = OcrSdkOcrConfig()
    var textBlock: OcrSdkTextBlock? = null

    val ayanAPI by lazy { createAyanAPiCall(baseUrl = Base_URL) { Token } }
    private var dialog: OcrSdkWaitingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentContainerFl) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }

        readInputIntent(intent)

        if (savedInstanceState == null) {
            init()
            handleStartFragment()
            validateSdkInitialization()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        readInputIntent(intent)
    }

    private fun readInputIntent(src: Intent) {
        action = src.action

        when (action) {
            OcrHelper.Actions.CAPTURE_URI -> {
                captureConfig = IntentCompat.getParcelableExtra(
                    src,
                    OcrHelper.Extras.CONFIG,            // change if you use a different key
                    OcrSdkCaptureConfig::class.java
                ) ?: OcrSdkCaptureConfig()

                textBlock = captureConfig.textBlock
            }

            OcrHelper.Actions.OCR_RETURN_DATA -> {
                ocrConfig = IntentCompat.getParcelableExtra(
                    src,
                    OcrHelper.Extras.CONFIG,            // use your actual key for OCR config if different
                    OcrSdkOcrConfig::class.java
                ) ?: OcrSdkOcrConfig()

                textBlock = ocrConfig.textBlock
            }
        }
    }

    private fun handleStartFragment() {
        when (action) {
            OcrHelper.Actions.CAPTURE_URI -> {
                start(SinglePhotoUri())
            }

            OcrHelper.Actions.OCR_RETURN_DATA, null -> {
                if (ocrConfig.singlePhoto == true) {
                    start(OcrSdkOcrFragment().also {
                        it.backImageUri = "".toUri()
                        it.cardType = ocrConfig.cardType?.uppercase().toString()
                        it.extraInfo = ocrConfig.extraInfo.toString()
                    })
                } else {
                    start(OcrSdkOcrFragment().also {
                        it.cardType = ocrConfig.cardType?.uppercase().toString()
                        it.extraInfo = ocrConfig.extraInfo.toString()
                    })
                }
            }
        }
    }

    fun sendUri(uri: Uri?) {
        val resultPayload = OcrSdkUriDataResult(
            uri = uri,
            extraInfo = captureConfig.extraInfo
        )
        val result = Intent().apply {
            if (Build.VERSION.SDK_INT >= 33) {
                putExtra(OcrHelper.Extras.RESULT, resultPayload)
            } else {
                @Suppress("DEPRECATION")
                putExtra(OcrHelper.Extras.RESULT, resultPayload)
            }
        }
        setResult(RESULT_OK, result)
        finish()
    }

    fun sendData(dataList: ArrayList<OcrSdkGetCardOcrResult.Result>) {
        val resultPayload = OcrSdkOcrDataResult(
            cardType = ocrConfig.cardType?.uppercase(),
            items = dataList,
            extraInfo = ocrConfig.extraInfo
        )
        val result = Intent().apply {
            if (Build.VERSION.SDK_INT >= 33) {
                putExtra(OcrHelper.Extras.RESULT, resultPayload)
            } else {
                @Suppress("DEPRECATION")
                putExtra(OcrHelper.Extras.RESULT, resultPayload)
            }
        }
        setResult(RESULT_OK, result)
        finish()
    }

    fun mFinishActivity() {
        setResult(RESULT_CANCELED, Intent())
        finish()
    }

    private fun init() {
        dialog = OcrSdkWaitingDialog(
            context = this,
            title = getString(R.string.ocr_loading_description)
        )
    }

    private fun createAyanAPiCall(baseUrl: String, getToken: GetUserToken? = null): AyanApi {
        return AyanApi(
            context = this,
            getUserToken = getToken,
            defaultBaseUrl = baseUrl,
            AyanCommonCallStatus {
                failure {
                    if (it.failureCode == "G00002" || it.failureCode == "GR0004") {
                        OcrSdkOneOptionDialog(
                            context = this@OcrActivity,
                            title = it.failureMessage,
                            icon = R.drawable.ocr_ic_wrong,
                            buttonText = "بازگشت",
                        ) { mFinishActivity() }.show()
                        return@failure
                    }
                    OcrSdkOneOptionDialog(
                        context = this@OcrActivity,
                        title = it.failureMessage,
                        icon = R.drawable.ocr_ic_wrong,
                        buttonText = "تلاش مجدد",
                    ) { it.reCallApi() }.show()
                }
                changeStatus {
                    when (it) {
                        CallingState.LOADING -> {}
                        CallingState.FAILED,
                        CallingState.SUCCESSFUL,
                        CallingState.NOT_USED -> dialog?.hideDialog()
                    }
                }
            },
            timeout = 120L
        )
    }

    private fun validateSdkInitialization() {
        if (EndPoint_UploadCardOCR.isNull() || Token.isNull() || EndPoint_GetCardOcrResult.isNull() || Base_URL.isNull()) {
            showToast("Base_Url or Token are not Initialized!")
        }
    }

    fun showToast(text: String, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, text, length).show()
    }

}

