package ir.ayantech.ocr_sdk

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.hardware.camera2.CaptureResult
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide.init
import ir.ayantech.ocr_sdk.databinding.OcrActivityBinding

import ir.ayantech.ayannetworking.api.AyanApi
import ir.ayantech.ayannetworking.api.AyanCommonCallStatus
import ir.ayantech.ayannetworking.api.CallingState
import ir.ayantech.ayannetworking.api.GetUserToken
import ir.ayantech.ocr_sdk.OCRConstant.Base_URL
import ir.ayantech.ocr_sdk.OCRConstant.EndPoint_GetCardOcrResult
import ir.ayantech.ocr_sdk.OCRConstant.EndPoint_UploadCardOCR
import ir.ayantech.ocr_sdk.OCRConstant.Token
import ir.ayantech.ocr_sdk.component.WaitingDialog
import ir.ayantech.ocr_sdk.model.CaptureConfig
import ir.ayantech.ocr_sdk.model.GetCardOcrResult
import ir.ayantech.ocr_sdk.model.OcrConfig
import ir.ayantech.ocr_sdk.model.OcrDataResult
import ir.ayantech.ocr_sdk.model.UriDataResult
import ir.ayantech.ocr_sdk.ui.SinglePhotoUri
import ir.ayantech.whygoogle.activity.WhyGoogleActivity
import ir.ayantech.whygoogle.helper.isNull
import kotlin.jvm.java


open class OcrActivity : WhyGoogleActivity<OcrActivityBinding>() {

    override val binder: (LayoutInflater) -> OcrActivityBinding
        get() = OcrActivityBinding::inflate
    override val containerId: Int = R.id.fragmentContainerFl

        // قبلی‌ها را با یک مدل واحد جایگزین می‌کنیم
        private var action: String? = null
          var captureConfig: CaptureConfig = CaptureConfig()
          var ocrConfig: OcrConfig = OcrConfig()

        // شبکه/دیالوگ: مثل قبل نگه‌داریم
          val ayanAPI by lazy { createAyanAPiCall(baseUrl = Base_URL) { Token } }
        private var dialog: WaitingDialog? = null

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

            when (action ){
                OcrHelper.Actions.CAPTURE_URI -> { // API 33+ typed getter
                    captureConfig = if (Build.VERSION.SDK_INT >= 33) {
                        src.getParcelableExtra(OcrHelper.Extras.CONFIG, CaptureConfig::class.java) ?: CaptureConfig()
                    } else {
                        @Suppress("DEPRECATION")
                        src.getParcelableExtra(OcrHelper.Extras.CONFIG) ?: CaptureConfig()
                    }}
                OcrHelper.Actions.OCR_RETURN_DATA -> {
                    // API 33+ typed getter
                    ocrConfig = if (Build.VERSION.SDK_INT >= 33) {
                        src.getParcelableExtra(OcrHelper.Extras.CONFIG, OcrConfig::class.java) ?: OcrConfig()
                    } else {
                        @Suppress("DEPRECATION")
                        src.getParcelableExtra(OcrHelper.Extras.CONFIG) ?: OcrConfig()
                    }
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
                        start(CameraXFragment().also {
                            it.backImageUri = "".toUri()
                            it.cardType = ocrConfig.cardType?.uppercase().toString()
                            it.extraInfo = ocrConfig.extraInfo.toString()
                        })
                    } else {
                        start(CameraXFragment().also {
                            it.cardType = ocrConfig.cardType?.uppercase().toString()
                            it.extraInfo = ocrConfig.extraInfo.toString()
                        })
                    }
                }
            }
        }

        fun sendUri(uri: Uri?) {
            val resultPayload = UriDataResult(
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
            setResult(Activity.RESULT_OK, result)
            finish()
        }

        fun sendData(dataList: ArrayList<GetCardOcrResult.Result>) {
            val resultPayload = OcrDataResult(
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
            setResult(Activity.RESULT_OK, result)
            finish()
        }

        fun mFinishActivity() {
            setResult(Activity.RESULT_CANCELED, Intent())
            finish()
        }
        private fun init() {
            dialog = WaitingDialog(
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
                            OneOptionDialog(
                                context = this@OcrActivity,
                                title = it.failureMessage,
                                icon = R.drawable.ocr_ic_wrong,
                                buttonText = "بازگشت",
                            ) { mFinishActivity() }.show()
                            return@failure
                        }
                        OneOptionDialog(
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

