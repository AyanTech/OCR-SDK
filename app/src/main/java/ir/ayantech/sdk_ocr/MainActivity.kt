package ir.ayantech.sdk_ocr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import ir.ayantech.ocr_sdk.OCRConfig
import ir.ayantech.ocr_sdk.OCRConstant
import ir.ayantech.ocr_sdk.OcrActivity
import ir.ayantech.ocr_sdk.model.GetCardOcrResult
import ir.ayantech.sdk_ocr.databinding.ActivityMainBinding
import ir.ayantech.whygoogle.activity.WhyGoogleActivity

class MainActivity : WhyGoogleActivity<ActivityMainBinding>() {

    override val binder: (LayoutInflater) -> ActivityMainBinding
        get() = ActivityMainBinding::inflate
    override val containerId: Int = R.id.fragmentContainerFl
    var packageNamee: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OCRConstant.context = this

        binding.btnOcrCar.setOnClickListener {


            callApi("VehicleCard", false)

        }
        binding.btnIdCard.setOnClickListener {
            callApi("NationalCard", true)

        }
        binding.btnOcrBank.setOnClickListener {
            callApi("BankCard", true)

        }
    }

    fun callApi(cardType: String, singlePhoto: Boolean) {


        OCRConfig.builder()
            .setContext(this)
            .setApplicationID("ir.ayantech.sdk_ocr")
            .setBaseUrl("https://core.pishkhan24.ayantech.ir/webservices/Proxy.svc/")
            .setToken("1AEDF1D7398E4C6A92D8FE2DA77789D1")
            .setUploadImageEndPoint("UploadNewCardOcrImage")
            .setGetResultEndPoint("GetCardOcrResult")
            .build()

        startActivity(Intent(this, OcrActivity::class.java).also {
            it.putExtra("cardType", cardType)
            it.putExtra("singlePhoto", singlePhoto)
            it.putExtra("className", "ir.ayantech.sdk_ocr.MainActivity")
        })
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("asdasdkjahdkjahskjd", "onActivityResult")
        Toast.makeText(this, "$requestCode", Toast.LENGTH_SHORT).show()
        if (data != null) {
            val dataArray = data.getParcelableArrayExtra("GetCardOcrResult")
            val cardType = intent.getStringExtra("cardType")
            binding.tvResponse.text = "$dataArray"
            Log.d("asdasdkjahdkjahskjd", "GetCardOcrResult: $dataArray")
            Log.d("asdasdkjahdkjahskjd", "cardType: $cardType")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val data = intent?.getParcelableArrayExtra("GetCardOcrResult").toString()
        Log.d("asdasdkjahdkjahskjd", "onNewIntent: $data")
    }
}