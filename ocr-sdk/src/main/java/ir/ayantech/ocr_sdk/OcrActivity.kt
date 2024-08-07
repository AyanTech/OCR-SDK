package ir.ayantech.ocr_sdk

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import ir.ayantech.ayannetworking.api.AyanApi
import ir.ayantech.ayannetworking.api.AyanCommonCallStatus
import ir.ayantech.ayannetworking.api.CallingState
import ir.ayantech.ayannetworking.api.GetUserToken
import ir.ayantech.ocr_sdk.OCRConstant.Base_URL
import ir.ayantech.ocr_sdk.OCRConstant.EndPoint_GetCardOcrResult
import ir.ayantech.ocr_sdk.OCRConstant.EndPoint_UploadCardOCR
import ir.ayantech.ocr_sdk.OCRConstant.Token
import ir.ayantech.ocr_sdk.OCRConstant.context
import ir.ayantech.ocr_sdk.component.WaitingDialog
import ir.ayantech.ocr_sdk.databinding.OcrActivityBinding
import ir.ayantech.ocr_sdk.model.GetCardOcrResult
import ir.ayantech.whygoogle.activity.WhyGoogleActivity
import ir.ayantech.whygoogle.helper.isNull


open class OcrActivity : WhyGoogleActivity<OcrActivityBinding>() {


    lateinit var originActivity: AppCompatActivity

    var cardType = ""
    var extraInfo = ""
    var singlePhoto = false
    val ayanAPI by lazy {
        createAyanAPiCall(baseUrl = Base_URL) { Token }
    }
    var dialog: WaitingDialog? = null
    override val binder: (LayoutInflater) -> OcrActivityBinding
        get() = OcrActivityBinding::inflate
    override val containerId: Int = R.id.fragmentContainerFl
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = applicationContext
        getIntentData()
        if (savedInstanceState == null)
            handleStartFragment()
    }

    private fun getIntentData() {
        Log.d("OCRLOGS", "intent: $intent")
        Log.d("OCRLOGS", "cardType: ${intent.getStringExtra("cardType").toString().uppercase()}")

        if (intent == null) {
            return
        } else {

            cardType = intent.getStringExtra("cardType").toString().uppercase()
            extraInfo = intent.getStringExtra("extraInfo").toString().uppercase()
            singlePhoto = intent.getBooleanExtra("singlePhoto", false)
            val className = intent.getStringExtra("className")
            if (className != null) {
                try {
                    val clazz = Class.forName(className)
                    originActivity = clazz.newInstance() as AppCompatActivity
                } catch (e: Exception) {
                    Log.d("ocrActivity", "getIntentData exception: $e")
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        cardType = intent.getStringExtra("cardType").toString()
        val className = intent.getStringExtra("className")
        if (className != null) {
            val clazz = Class.forName(className)
            originActivity = clazz.newInstance() as AppCompatActivity
        }
    }

    fun sendResult(dataList: ArrayList<GetCardOcrResult.Result>) {

        val intent = Intent(this, originActivity::class.java)
        intent.putParcelableArrayListExtra("GetCardOcrResult", dataList)
        intent.putExtra("cardType", cardType)
        intent.putExtra("extraInfo", extraInfo)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    fun finishActivity() {
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
    }

    private fun handleStartFragment() {

        if (EndPoint_UploadCardOCR.isNull() || Token.isNull() || EndPoint_GetCardOcrResult.isNull() || Base_URL.isNull()) {
            showToast("Base_Url or Token are not Initialized!")
            return
        }
        init()
        gettingPermissions()
        if (singlePhoto) {
            start(CameraXFragment().also {
                it.backImageUri = "".toUri()
                it.cardType = cardType
                it.extraInfo = extraInfo
            })
        } else {
            start(CameraXFragment().also {
                it.cardType = cardType
                it.extraInfo = extraInfo
            })
        }
    }

    private fun gettingPermissions() {

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ), 1
        )
    }

    private fun init() {
        dialog = this.let { context ->
            WaitingDialog(
                context = context,
                title = context.getString(R.string.ocr_loading_description)
            )
        }

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
                        ) {
                            finishActivity()
                        }.show()
                        return@failure
                    }
                    OneOptionDialog(
                        context = this@OcrActivity,
                        title = it.failureMessage,
                        icon = R.drawable.ocr_ic_wrong,
                        buttonText = "تلاش مجدد",
                    ) {
                        it.reCallApi()
                    }.show()
                }
                changeStatus {
                    when (it) {
                        CallingState.LOADING -> {}
                        CallingState.FAILED -> dialog?.hideDialog()
                        CallingState.SUCCESSFUL -> dialog?.hideDialog()
                        CallingState.NOT_USED -> dialog?.hideDialog()
                    }
                }
            },
            timeout = 120L

        )
    }

    fun showToast(text: String, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, text, length).show()
    }

    override fun onDestroy() {

        super.onDestroy()
    }
}
