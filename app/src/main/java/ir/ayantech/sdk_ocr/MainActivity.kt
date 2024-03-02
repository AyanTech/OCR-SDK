package ir.ayantech.sdk_ocr

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.core.content.ContextCompat.startActivity
import ir.ayantech.ocr_sdk.OCRConfig
import ir.ayantech.ocr_sdk.Constant
import ir.ayantech.ocr_sdk.OcrActivity
import ir.ayantech.ocr_sdk.OcrInitializer
import ir.ayantech.ocr_sdk.model.GetCardOcrResult
import ir.ayantech.sdk_ocr.databinding.ActivityMainBinding
import ir.ayantech.whygoogle.activity.WhyGoogleActivity

class MainActivity : WhyGoogleActivity<ActivityMainBinding>() {

    override val binder: (LayoutInflater) -> ActivityMainBinding
        get() = ActivityMainBinding::inflate
    override val containerId: Int = R.id.fragmentContainerFl
    var packageNamee: String =""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Constant.context = this
        val extras = intent.extras
        val data = extras?.getParcelableArrayList<GetCardOcrResult.Result>("GetCardOcrResult")
        val cardType = intent.getStringExtra("cardType")
        binding.tvResponse.text = "$data"
        Log.d("asdasdkjahdkjahskjd", "GetCardOcrResult: $data")
        Log.d("asdasdkjahdkjahskjd", "cardType: $cardType")

        binding.btnOcrCar.setOnClickListener {
            callApi("VehicleCard",false)

        }
        binding.btnIdCard.setOnClickListener {
            callApi("NationalCard",true)

        }
        binding.btnOcrBank.setOnClickListener {
            callApi("BankCard",true)

        }
    }

    fun callApi(cardType: String , singlePhoto:Boolean) {


        OCRConfig.builder()
            .setContext(this)
            .setBaseUrl("https://core.pishkhan24.ayantech.ir/webservices/Proxy.svc/")
            .setToken("016D5D1CAA694DFABECC3880772D74CF")
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


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val data = intent?.getParcelableArrayExtra("GetCardOcrResult").toString()
        Log.d("asdasdkjahdkjahskjd", "onNewIntent: $data")
    }
}