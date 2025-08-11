package ir.ayantech.sdk_ocr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import ir.ayantech.ocr_sdk.OCRConfig
import ir.ayantech.ocr_sdk.OCRConstant
import ir.ayantech.ocr_sdk.OcrActivity
import ir.ayantech.ocr_sdk.OcrHelper
import ir.ayantech.ocr_sdk.component.WaitingDialog
import ir.ayantech.ocr_sdk.enums.OcrCardTypesEnum
import ir.ayantech.ocr_sdk.model.CaptureConfig
import ir.ayantech.ocr_sdk.model.OcrConfig
import ir.ayantech.ocr_sdk.tools.CaptureContract
import ir.ayantech.ocr_sdk.tools.OCRContract
 import ir.ayantech.sdk_ocr.databinding.ActivityMainBinding
import ir.ayantech.whygoogle.activity.WhyGoogleActivity
import kotlinx.coroutines.launch

class MainActivity : WhyGoogleActivity<ActivityMainBinding>() {

    override val binder: (LayoutInflater) -> ActivityMainBinding
        get() = ActivityMainBinding::inflate
    override val containerId: Int = R.id.fragmentContainerFl
    var packageNamee: String = "ir.ayantech.sdk_ocr.MainActivity"
    private val urlContract = registerForActivityResult(CaptureContract()) { uriDataResult ->

        val uri = uriDataResult?.uri
        val extraInfo = uriDataResult?.extraInfo

        Log.d("asdasda", ": $uri")
        Log.d("asdasda", ": $extraInfo")

        Glide.with(this)
            .load(uri)
            .dontAnimate()
            .priority(Priority.IMMEDIATE)
            .into(binding.captureA.circularImg)

        val dialog = WaitingDialog(
            context = this,
            title = "درحال فشرده سازی..."
        )
        dialog.showDialog()
        lifecycleScope.launch {
            val base64 = OcrHelper.encodeImageToBase64(this@MainActivity, uri, 4)
            // send "base64" to api
            dialog.hideDialog()
            uri?.let {
                OcrHelper.deleteCachedFileFromUri(this@MainActivity, uri)
            }
        }
    }
    private val ocrContract = registerForActivityResult(OCRContract()) { ocrDataResult ->

        val items = ocrDataResult?.items
        val extraInfo = ocrDataResult?.extraInfo
        val cardType = ocrDataResult?.cardType

        Log.d("asdasda", "log: $ocrDataResult")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OCRConstant.context = this

        OCRConfig.builder()
            .setContext(this)
            .setApplicationID("ir.ayantech.sdk_ocr")
            .setBaseUrl("https://core.pishkhan24.ayantech.ir/webservices/Proxy.svc/")
            .setToken("1AEDF1D7398E4C6A92D8FE2DA77789D1")
            .setUploadImageEndPoint("UploadNewCardOcrImage")
            .setGetResultEndPoint("GetCardOcrResult")
            .build()

        binding.btnOcrCar.setOnClickListener {
            ocrContract.launch(
                OcrConfig(
                    maxSizeMb = 4,
                    className = packageNamee,
                    cardType = OcrCardTypesEnum.VehicleCard.value,
                    singlePhoto = false,
                    extraInfo = "testi"
                )
            )
        }
        binding.btnIdCard.setOnClickListener {

            urlContract.launch(CaptureConfig(
                maxSizeMb = 4,
                className = packageNamee,
                extraInfo = "capture test"
            ))
        }
        binding.btnOcrBank.setOnClickListener {

        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val data = intent?.getParcelableArrayExtra("GetCardOcrResult").toString()
        Log.d("asdasdkjahdkjahskjd", "onNewIntent: $data")
    }
}