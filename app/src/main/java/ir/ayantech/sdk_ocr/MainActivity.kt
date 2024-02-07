package ir.ayantech.sdk_ocr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Constant.context = this
        val extras = intent.extras
        val data = extras?.getParcelableArrayList<GetCardOcrResult.Result>("GetCardOcrResult")
        val cardType = intent.getStringExtra("cardType")
         Log.d("asdasdkjahdkjahskjd", "GetCardOcrResult: $data")
         Log.d("asdasdkjahdkjahskjd", "cardType: $cardType")

    binding.btnOCR.setOnClickListener{
        OcrInitializer().setConfig(
            token="6C043F55214F45C3B08609AFBBE7E009",
            baseUrl = "https://core.pishkhan24.ayantech.ir/webservices/Proxy.svc/",
            uploadImageEndPoint  ="UploadNewCardOcrImage",
            getResultEndPoint = "GetCardOcrResult",
            this

        )
       startActivity(Intent(this, OcrActivity::class.java).also {
           it.putExtra("cardType", "BankCard")
           it.putExtra("className", "ir.ayantech.sdk_ocr.MainActivity")
       })
        finish()
     }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val data = intent?.getParcelableArrayExtra("GetCardOcrResult").toString()
        Log.d("asdasdkjahdkjahskjd", "onNewIntent: $data")
    }
}