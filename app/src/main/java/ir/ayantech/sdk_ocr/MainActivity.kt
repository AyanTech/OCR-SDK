package ir.ayantech.sdk_ocr

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import ir.ayantech.ocr_sdk.component.OcrSdkWaitingDialog
import ir.ayantech.ocr_sdk.enums.OcrSdkOcrCardTypesEnum
import ir.ayantech.ocr_sdk.model.EncodeImageListenerWithMetrics
import ir.ayantech.ocr_sdk.model.EncodeMetrics
import ir.ayantech.ocr_sdk.model.OcrSdkCaptureConfig
import ir.ayantech.ocr_sdk.model.OcrSdkOcrConfig
import ir.ayantech.ocr_sdk.model.OcrSdkTextBlock
import ir.ayantech.ocr_sdk.tools.CaptureContract
import ir.ayantech.ocr_sdk.tools.EncodeImageListener
import ir.ayantech.ocr_sdk.tools.OCRConfig
import ir.ayantech.ocr_sdk.tools.OCRConstant
import ir.ayantech.ocr_sdk.tools.OCRContract
import ir.ayantech.ocr_sdk.tools.OcrHelper
import ir.ayantech.sdk_ocr.databinding.ActivityMainBinding
import ir.ayantech.whygoogle.activity.WhyGoogleActivity
import ir.ayantech.whygoogle.helper.isNull
import kotlinx.coroutines.launch

class MainActivity : WhyGoogleActivity<ActivityMainBinding>() {

    override val binder: (LayoutInflater) -> ActivityMainBinding
        get() = ActivityMainBinding::inflate
    override val containerId: Int = R.id.fragmentContainerFl
    var packageNamee: String = "ir.ayantech.sdk_ocr.MainActivity"
    val requestReadPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> /* ادامه‌ی کار */ }
    val responseLog = arrayListOf<String>()
    private val urlContract = registerForActivityResult(CaptureContract()) { uriDataResult ->

        val uri = uriDataResult?.uri
        val extraInfo = uriDataResult?.extraInfo

        Log.d("asdasda", ": $uri")
        Log.d("asdasda", ": $extraInfo")

        compressImage(uri, 4.0)
    }

    private val ocrContract = registerForActivityResult(OCRContract()) { ocrDataResult ->

        val items = ocrDataResult?.items
        val extraInfo = ocrDataResult?.extraInfo
        val cardType = ocrDataResult?.cardType

        Log.d("asdasda", "log: $ocrDataResult")

        binding.tvResponse.text = ocrDataResult.toString()
    }


    private val openDocContract = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // برای دسترسی‌های بعدی (پس از ری‌استارت اپ) پرسیست کن:
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
           compressImage(uri, getMaxCompressionInt())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OCRConstant.context = this
        binding.edMax.text = 4.toString().toEditable()


        binding.btnUri.setOnClickListener {
            initSDK()
            urlContract.launch(
                OcrSdkCaptureConfig(
                    maxSizeMb = getMaxCompressionInt(),
                    minSizeMb = getMinCompressionInt(),
                    className = packageNamee,
                    extraInfo = "capture test"
                )
            )

        }
        binding.btnOcrDouble.setOnClickListener {
            initSDK()
            ocrContract.launch(
                OcrSdkOcrConfig(
                    maxSizeMb = getMaxCompressionInt(),
                    minSizeMb = getMinCompressionInt(),
                    className = packageNamee,
                    cardType = OcrSdkOcrCardTypesEnum.VehicleCard.value,
                    singlePhoto = false,
                    extraInfo = "testi",
                    textBlock = OcrSdkTextBlock(
                        title = "نوشته بالا",
                        firstImageHolderText = "عکس اول",
                        secondImageHolderText = "عکس دوم",
                        buttonText = "باتن پایین"
                    )
                )
            )
        }
        binding.btnOcrSingle.setOnClickListener {
            initSDK()
            ocrContract.launch(
                OcrSdkOcrConfig(
                    maxSizeMb = getMaxCompressionInt(),
                    minSizeMb = getMinCompressionInt(),
                    className = packageNamee,
                    cardType = OcrSdkOcrCardTypesEnum.NationalCard.value,
                    singlePhoto = true,
                    extraInfo = "testi",
                    textBlock = OcrSdkTextBlock(
                        title = "نوشته بالا",
                        firstImageHolderText = "عکس اول",
                        secondImageHolderText = "عکس دوم",
                        buttonText = "باتن پایین"
                    )
                )
            )
        }
        binding.btnFromGallery.setOnClickListener {
            askReadPermission()
            openDocContract.launch(arrayOf("image/*"))

        }
    }
    private fun initSDK(){
        OCRConfig.builder()
            .setContext(this)
            .setApplicationID("ir.ayantech.sdk_ocr")
            .setBaseUrl("https://core.pishkhan24.ayantech.ir/webservices/Proxy.svc/")
            .setToken("0C42307F3F4E44FD8FEE1ABAD7141A9A")
            .setUploadImageEndPoint("UploadNewCardOcrImage")
            .setGetResultEndPoint("GetCardOcrResult")
            .build()
    }
    private fun getMaxCompressionInt(): Double {
        return binding.edMax.text.toString().toDouble()
    }
    private fun getMinCompressionInt(): Double {
        return binding.edMin.text.toString().toDouble()
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val data = intent?.getParcelableArrayExtra("GetCardOcrResult").toString()
        Log.d("asdasdkjahdkjahskjd", "onNewIntent: $data")
    }

    fun askReadPermission() {
        val perm = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
        requestReadPermission.launch(perm)
    }

    fun compressImage(uri: Uri?, maxSizeInMb: Double) {
        if (uri.isNull()) return

        val dialog = OcrSdkWaitingDialog(
            context = this,
            title = "درحال فشرده سازی..."
        )
        dialog.showDialog()
        lifecycleScope.launch {
            val base64 = OcrHelper.encodeImageToBase64(
                this@MainActivity,
                uri,
                getMaxCompressionInt(),
                getMinCompressionInt(),
                listener = object : EncodeImageListenerWithMetrics {
                    override fun onSuccess(base64: String) {
                        dialog.hideDialog()
                        contentResolver.openInputStream(uri!!)?.use { input ->
                            Glide.with(this@MainActivity)
                                .load(uri)
                                .dontAnimate()
                                .priority(Priority.IMMEDIATE)
                                .into(binding.captureA.circularImg)
                        }
                        Toast.makeText(
                            this@MainActivity,
                            "عملیات با موفتیت انجام شد",
                            Toast.LENGTH_SHORT
                        ).show()
                        uri?.let {
                            OcrHelper.deleteCachedFileFromUri(this@MainActivity, uri)
                        }
                        Log.d("asdad", "onSuccess: $uri")

                    }

                    override fun onFailed(reason: String, throwable: Throwable?) {
                        updateLog(reason)
                        dialog.hideDialog()
                        Log.d("asdad", "onFailed: $reason + $throwable")
                        Toast.makeText(
                            this@MainActivity,
                            reason + throwable?.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onProgress(percent: Int, message: String) {


                    }

                    override fun onSuccess(
                        base64: String,
                        metrics: EncodeMetrics
                    ) {
                        dialog.hideDialog()
                        contentResolver.openInputStream(uri!!)?.use { input ->
                            Glide.with(this@MainActivity)
                                .load(uri)
                                .dontAnimate()
                                .priority(Priority.IMMEDIATE)
                                .into(binding.captureA.circularImg)
                        }
                        Toast.makeText(
                            this@MainActivity,
                            "عملیات با موفتیت انجام شد",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateLog(metrics.toString())
                    }
                })

        }
    }

    fun String.toEditable(): Editable {
        return SpannableStringBuilder(this)
    }

    fun updateLog(log: String) {
        responseLog.add(log)
        responseLog.add("")
        binding.tvLog.text = responseLog.toString()
    }
}