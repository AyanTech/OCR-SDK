package ir.ayantech.sdk_ocr

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import ir.ayantech.ocr_sdk.OCRConfig
import ir.ayantech.ocr_sdk.OCRConstant
import ir.ayantech.ocr_sdk.OcrActivity
import ir.ayantech.ocr_sdk.OcrHelper
import ir.ayantech.ocr_sdk.component.WaitingDialog
import ir.ayantech.sdk_ocr.databinding.ActivityMainBinding
import ir.ayantech.whygoogle.activity.WhyGoogleActivity
import kotlinx.coroutines.launch

class MainActivity : WhyGoogleActivity<ActivityMainBinding>() {

    override val binder: (LayoutInflater) -> ActivityMainBinding
        get() = ActivityMainBinding::inflate
    override val containerId: Int = R.id.fragmentContainerFl
    var packageNamee: String = ""
    var ocrResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data?.extras
                val uri = data?.getString("uri")
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
                    val base64 = OcrHelper.encodeImageToBase64(this@MainActivity, uri?.toUri(), 1)
                    dialog.hideDialog()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OCRConstant.context = this

        binding.btnOcrCar.setOnClickListener {


            callApi("VehicleCard", true, true)

        }
        binding.btnIdCard.setOnClickListener {
            callApi("NationalCard", true, false)

        }
        binding.btnOcrBank.setOnClickListener {
            callApi("BankCard", true, false)

        }
    }

    fun callApi(cardType: String, singlePhoto: Boolean, getUri: Boolean) {

        val quality = binding.ed.text
        OCRConfig.builder()
            .setContext(this)
            .setApplicationID("ir.ayantech.sdk_ocr")
            .setBaseUrl("https://core.pishkhan24.ayantech.ir/webservices/Proxy.svc/")
            .setToken("1AEDF1D7398E4C6A92D8FE2DA77789D1")
            .setUploadImageEndPoint("UploadNewCardOcrImage")
            .setGetResultEndPoint("GetCardOcrResult")
            .build()

        val intent = Intent(this, OcrActivity::class.java)
        intent.putExtra("gettingUri", getUri)
        intent.putExtra("cardType", cardType)
        intent.putExtra("maxSizeInMb", quality)
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