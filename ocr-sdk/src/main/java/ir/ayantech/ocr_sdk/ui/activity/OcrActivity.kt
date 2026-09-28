package ir.ayantech.ocr_sdk.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import ir.ayantech.networking.ayanModel.Language
import ir.ayantech.networking.ayanModel.LogLevel
import ir.ayantech.networking.v2.AyanApi
import ir.ayantech.ocr_sdk.R
import ir.ayantech.ocr_sdk.component.OcrSdkWaitingDialog
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.data.model.OcrSdkCaptureConfig
import ir.ayantech.ocr_sdk.data.model.OcrSdkLanguage
import ir.ayantech.ocr_sdk.data.model.OcrSdkOcrConfig
import ir.ayantech.ocr_sdk.data.model.OcrSdkOcrDataResult
import ir.ayantech.ocr_sdk.data.model.OcrSdkUriDataResult
import ir.ayantech.ocr_sdk.databinding.OcrActivityBinding
import ir.ayantech.ocr_sdk.tools.OCRConstant.Base_URL
import ir.ayantech.ocr_sdk.tools.OCRConstant.Token
import ir.ayantech.ocr_sdk.tools.OcrEdgeToEdgeHelper
import ir.ayantech.ocr_sdk.tools.OcrHelper
import ir.ayantech.ocr_sdk.tools.isNull
import ir.ayantech.ocr_sdk.ui.fragment.OcrSdkBaseFragment
import ir.ayantech.ocr_sdk.ui.fragment.OcrSdkOcrFragment
import ir.ayantech.ocr_sdk.ui.fragment.OcrSdkSinglePhotoUriFragment
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

open class OcrActivity : AppCompatActivity() {

    private var languageResources: Resources? = null

    override fun getResources(): Resources = languageResources ?: super.getResources()

    private lateinit var _binding: OcrActivityBinding
    val binding get() = _binding

    private val containerId: Int = R.id.fragmentContainerFl

    private var action: String? = null
    var captureConfig: OcrSdkCaptureConfig = OcrSdkCaptureConfig()
    var ocrConfig: OcrSdkOcrConfig = OcrSdkOcrConfig()
    val language: OcrSdkLanguage
        get() = if (action == OcrHelper.Actions.CAPTURE_URI) captureConfig.language else ocrConfig.language

    val ayanAPI by lazy { createAyanAPiCall(baseUrl = Base_URL) { Token } }
    private var dialog: OcrSdkWaitingDialog? = null

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val handled = (getTopFragment() as? OcrSdkBaseFragment)?.onBackPressed() == true
            if (handled.not()) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        readInputIntent(intent)
        applyLanguage()
        ocrConfig.nightMode?.let { AppCompatDelegate.setDefaultNightMode(it) }
        super.onCreate(savedInstanceState)
        _binding = OcrActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val edgeToEdgeHelper = OcrEdgeToEdgeHelper(this).also { it.enable() }
        onBackPressedDispatcher.addCallback(this, backCallback)
        edgeToEdgeHelper.applyInsets(binding)
        init()

        if (savedInstanceState == null) {
            handleStartFragment()
            validateSdkInitialization()
        }
    }

    fun start(fragment: Fragment, addToBackStack: Boolean = true) {
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(containerId, fragment, fragment.javaClass.simpleName)
            .apply {
                if (addToBackStack) {
                    addToBackStack(fragment.javaClass.simpleName)
                }
            }
            .commit()
    }

    fun getTopFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(containerId)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val previousLanguage = language
        setIntent(intent)
        readInputIntent(intent)
        if (language != previousLanguage) recreate()
    }

    @SuppressLint("AppBundleLocaleChanges")
    private fun applyLanguage() {
        val locale = Locale.forLanguageTag(language.languageTag)
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        languageResources = baseContext.createConfigurationContext(configuration).resources
    }

    private fun readInputIntent(src: Intent) {
        action = src.action

        when (action) {
            OcrHelper.Actions.CAPTURE_URI -> {
                captureConfig = IntentCompat.getParcelableExtra(
                    src,
                    OcrHelper.Extras.CONFIG,
                    OcrSdkCaptureConfig::class.java
                ) ?: OcrSdkCaptureConfig()

                ocrConfig.textBlock = captureConfig.textBlock
            }

            OcrHelper.Actions.OCR_RETURN_DATA -> {
                ocrConfig = IntentCompat.getParcelableExtra(
                    src,
                    OcrHelper.Extras.CONFIG,
                    OcrSdkOcrConfig::class.java
                ) ?: OcrSdkOcrConfig()

            }
        }
    }

    private fun handleStartFragment() {
        when (action) {
            OcrHelper.Actions.CAPTURE_URI -> {
                start(OcrSdkSinglePhotoUriFragment.newInstance())
            }

            OcrHelper.Actions.OCR_RETURN_DATA, null -> {
                val backUri = if (ocrConfig.singlePhoto == true) "".toUri() else null
                start(
                    OcrSdkOcrFragment.newInstance(
                        cardType = ocrConfig.cardType?.uppercase().toString(),
                        backImageUri = backUri
                    )
                )
            }
        }
    }

    fun sendUri(uri: Uri?) {
        val resultPayload = OcrSdkUriDataResult(
            uri = uri,
            extraInfo = captureConfig.extraInfo
        )
        val result = Intent().apply {
            putExtra(OcrHelper.Extras.RESULT, resultPayload)
        }
        setResult(RESULT_OK, result)
        finish()
    }

    fun sendData(dataList: List<GetCardOcrResult.OcrResult>) {
        val resultPayload = OcrSdkOcrDataResult(
            cardType = ocrConfig.cardType?.uppercase(),
            items = dataList,
            extraInfo = ocrConfig.extraInfo
        )
        val result = Intent().apply {
            putExtra(OcrHelper.Extras.RESULT, Json.encodeToString(resultPayload))
        }
        setResult(RESULT_OK, result)
        finish()
    }

    fun mFinishActivity() {
        setResult(RESULT_CANCELED, Intent())
        finish()
    }

    private fun init() {
        val waitingDialog = OcrSdkWaitingDialog(
            context = this,
            title = getString(R.string.ocr_loading_description)
        )
        dialog = waitingDialog
        binding.root.post {
            if (!isFinishing && !isDestroyed) waitingDialog.create()
        }
    }

    fun showProgress(message: String) {
        dialog?.showDialog(message)
    }

    fun updateProgress(percent: Int, message: String) {
        dialog?.update(message, percent)
    }

    fun hideProgress() {
        dialog?.hideDialog()
    }

    override fun onDestroy() {
        dialog?.dismiss()
        dialog = null
        super.onDestroy()
    }

    private fun createAyanAPiCall(baseUrl: String, getToken: () -> String?): AyanApi {
        return AyanApi.Builder(context = this, baseUrl = baseUrl)
            .setInvokeUserToken { getToken() ?: "" }
            .setTimeOutDuration(120.seconds)
            .setLogLevel(LogLevel.DO_NOT_LOG)
            .setAcceptLanguage(if (language == OcrSdkLanguage.PERSIAN) Language.PERSIAN else Language.ENGLISH)
            .build()
    }

    private fun validateSdkInitialization() {
        if (Token.isNull() || Base_URL.isNull()) {
            showToast(getString(R.string.ocr_sdk_not_initialized))
        }
    }

    fun showToast(text: String, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, text, length).show()
    }
}
