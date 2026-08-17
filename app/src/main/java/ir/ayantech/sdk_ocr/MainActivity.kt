package ir.ayantech.sdk_ocr

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import ir.ayantech.ocr_sdk.data.model.OcrSdkOcrConfig
import ir.ayantech.ocr_sdk.data.model.OcrSdkTextBlock
import ir.ayantech.ocr_sdk.enums.OcrSdkOcrCardTypesEnum
import ir.ayantech.ocr_sdk.tools.OCRConfig
import ir.ayantech.ocr_sdk.tools.OCRConstant
import ir.ayantech.ocr_sdk.tools.OCRContract
import ir.ayantech.sdk_ocr.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val urlContract = registerForActivityResult(OCRContract()) { result ->
        Log.d("TAG_AG", "uri: ${result?.items?.firstOrNull()} | extra info: ${result?.extraInfo} ")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        super.onCreate(savedInstanceState)
        OCRConstant.context = this
        initOCR()

        binding.btnOpenOcr.setOnClickListener {
            openOCR()
        }
    }

    private fun openOCR() {
        val ocrConfig = OcrSdkOcrConfig(
            maxBase64Mb = 1.0,
            minBase64Mb = 0.2,
            className = MainActivity::class.java.name,
            cardType = OcrSdkOcrCardTypesEnum.BankCard.value,
            singlePhoto = false,
            extraInfo = "Product name",
            textBlock = OcrSdkTextBlock(
                secondTitle = "از کارت بانکی خود عکس بگیرید.",
                firstImageHolderText = "روی کارت",
                secondImageHolderText = "پشت کارت",
                buttonText = "تایید"
            )
        )
        urlContract.launch(ocrConfig)
    }

    private fun initOCR() {
        OCRConfig.builder()
            .setContext(this)
            .setApplicationID("ir.ayantech.sdk_ocr")
            .setBaseUrl("https://application.billingsystem.ayantech.ir/WebServices/Core.svc/")
            .setToken("03544A9F0DFA4F9DACFEDAF8B9EBC398")
            .setUploadImageEndPoint("CardOcrUploadImage")
            .setGetResultEndPoint("CardOcrGetResult")
            .build()
    }
}