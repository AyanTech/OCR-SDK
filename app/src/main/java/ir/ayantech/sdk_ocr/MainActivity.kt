package ir.ayantech.sdk_ocr

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
    var ocrResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data?.extras

            }
        }

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

        val intent = Intent(this, OcrActivity::class.java)
        intent.putExtra("cardType", cardType)
        intent.putExtra("singlePhoto", singlePhoto)
        intent.putExtra("className", "ir.ayantech.sdk_ocr.MainActivity")
        ocrResult.launch(intent)

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val data = intent?.getParcelableArrayExtra("GetCardOcrResult").toString()
        Log.d("asdasdkjahdkjahskjd", "onNewIntent: $data")
    }
}