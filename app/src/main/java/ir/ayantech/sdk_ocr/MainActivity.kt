package ir.ayantech.sdk_ocr

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ir.ayantech.ocr_sdk.component.OcrSdkWaitingDialog
import ir.ayantech.ocr_sdk.model.EncodeImageListenerWithMetrics
import ir.ayantech.ocr_sdk.model.EncodeMetrics
import ir.ayantech.ocr_sdk.model.OcrSdkCaptureConfig
import ir.ayantech.ocr_sdk.model.OcrSdkTextBlock
import ir.ayantech.ocr_sdk.tools.CaptureContract
import ir.ayantech.ocr_sdk.tools.OCRConfig
import ir.ayantech.ocr_sdk.tools.OCRConstant
import ir.ayantech.ocr_sdk.tools.OcrHelper
import ir.ayantech.sdk_ocr.databinding.ActivityMainBinding
import ir.ayantech.whygoogle.activity.WhyGoogleActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call

class MainActivity : WhyGoogleActivity<ActivityMainBinding>() {

    override val binder: (LayoutInflater) -> ActivityMainBinding get() = ActivityMainBinding::inflate
    override val containerId: Int = R.id.fragmentContainerFl

    private var cardFrontBase64: String? = null
    private var cardBackBase64: String? = null
    private var isProcessingFront: String = "0"
    private var currentCall: Call? = null
    private var isRequesting = false

    // قراردادهای دوربین و گالری
    private val urlContract = registerForActivityResult(CaptureContract()) { res ->
        compressImage(res?.uri, res?.extraInfo.toString())
    }

    private val mOpenGalleryContract =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                compressImage(it, isProcessingFront)
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OCRConstant.context = this

        initOCR()
        setSpinner()
        setAdapter()
        setClickListeners()
        checkPermissions()
    }

    private fun initOCR() {
        OCRConfig.builder()
            .setContext(this)
            .setApplicationID("ir.ayantech.sdk_ocr")
            .setBaseUrl("https://core.pishkhan24.ayantech.ir/webservices/Proxy.svc/")
            .setToken("0C42307F3F4E44FD8FEE1ABAD7141A9A")
            .setUploadImageEndPoint("UploadNewCardOcrImage")
            .setGetResultEndPoint("GetCardOcrResult")
            .build()
    }

    private fun setClickListeners() {
        binding.layoutFrontPhoto.setOnClickListener {
            hideKeyboard()
            openSelectionDialog("0")
        }
        binding.layoutBackPhoto.setOnClickListener {
            hideKeyboard()
            openSelectionDialog("1")
        }

        binding.btnSend.setOnClickListener {
            hideKeyboard()
            if (isRequesting) cancelRequest() else sendRequest()
        }
    }

    private fun sendRequest() {
        if (cardFrontBase64 == null) {
            Toast.makeText(this, "Please select front image", Toast.LENGTH_SHORT).show()
            return
        }

        val manager = OcrNetworkManager(this)
        toggleLoading(true)

        lifecycleScope.launch(Dispatchers.Default) {
            currentCall = manager.sendOcrRequest(
                cardType = binding.spinner.selectedItem.toString(),
                cardFrontBase64 = cardFrontBase64 ?: "",
                cardBackBase64 = cardBackBase64 ?: "",
                traceNumber = "OCR_TEST_${System.currentTimeMillis()}",
                token = "TEST_TOKEN",
                callback = object : OcrNetworkManager.OcrCallback {
                    override fun onSuccess(data: OcrResponseModel) {
                        runOnUiThread {
                            toggleLoading(false)
                            binding.tvStatusCode.text = data.statusCode
                            binding.tvDescription.text = data.description
                            binding.tvTimeSpent.text = "${data.timeSpent} ms"
                            (binding.rvOcrResults.adapter as? OcrResultAdapter)?.updateData(
                                data.ocrRowModelList ?: mutableListOf()
                            )
                        }
                    }

                    override fun onError(message: String) {
                        runOnUiThread {
                            toggleLoading(false)
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
    }

    private fun cancelRequest() {
        currentCall?.cancel()
        currentCall = null
        toggleLoading(false)
        Toast.makeText(this, "Request Canceled", Toast.LENGTH_SHORT).show()
    }

    private fun toggleLoading(showLoading: Boolean) {
        isRequesting = showLoading
        with(binding) {
            if (showLoading) {
                // حالت در حال اجرا: دکمه قرمز و متن لغو
                btnSend.text = "Cancel Request"
                btnSend.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#E57373")) // قرمز ملایم
                loadingProgress.visibility = View.VISIBLE

                // اگر می‌خواهی پروگرس‌بار با متن تداخل نداشته باشه،
                // می‌تونی پروگرس‌بار رو در XML به سمت چپ Gravity کنی (مثلاً start|center_vertical)
            } else {
                // حالت عادی
                btnSend.text = "Analyze Card"
                btnSend.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#00ACC1")) // فیروزه‌ای
                loadingProgress.visibility = View.GONE
            }
        }
    }

    private fun getRandomFunnyMessage(): String {
        return listOf(
            "Putting the image on a diet... 🥗",
            "Shrinking pixels, please wait... 🤏",
            "Making it slim and fit... ✨",
            "Feeding the hungry OCR engine... 🍕",
            "Compressing like a pro... 🏋️‍♂️",
            "Teaching the image to be small... 🎓",
            "Just a sec, pixel surgery in progress... 🏥"
        ).random()
    }

    private fun compressImage(uri: Uri?, isFront: String) {
        if (uri == null) return
        val dialog = OcrSdkWaitingDialog(this, getRandomFunnyMessage())
        dialog.showDialog()

        lifecycleScope.launch {
            val maxS = binding.edMax.text.toString().toDoubleOrNull() ?: 4.0
            val minS = binding.edMin.text.toString().toDoubleOrNull() ?: 1.0

            OcrHelper.encodeImageToBase64(
                this@MainActivity, uri, maxS, minS,
                listener = object : EncodeImageListenerWithMetrics {
                    override fun onSuccess(base64Str: String, metrics: EncodeMetrics) {
                        dialog.hideDialog()
                        if (isFront == "0") cardFrontBase64 = base64Str else cardBackBase64 =
                            base64Str

                        val targetImg =
                            if (isFront == "0") binding.frontImage else binding.backImage

                        // نمایش تصویر
                        Glide.with(this@MainActivity)
                            .asBitmap()
                            .load(Base64.decode(base64Str, Base64.DEFAULT))
                            .transform(CenterCrop())
                            .into(targetImg)

                        // اصلاح استایل تصویر انتخاب شده
                        targetImg.imageTintList = null
                        targetImg.alpha = 1.0f
                        targetImg.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                        targetImg.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                        targetImg.scaleType = ImageView.ScaleType.CENTER_CROP

                        OcrHelper.deleteCachedFileFromUri(this@MainActivity, uri)
                    }

                    override fun onFailed(reason: String, t: Throwable?) {
                        dialog.hideDialog()
                    }

                    override fun onProgress(p: Int, m: String) {}
                    override fun onSuccess(base64: String) {}
                })
        }
    }

    private fun openSelectionDialog(isFront: String) {
        isProcessingFront = isFront
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Photo Source")
            .setItems(arrayOf("Camera", "Gallery")) { _, which ->
                if (which == 0) {
                    urlContract.launch(
                        OcrSdkCaptureConfig(
                            className = "ir.ayantech.sdk_ocr.MainActivity",
                            extraInfo = isFront,
                            textBlock = OcrSdkTextBlock(
                                title= "OCR",
                                firstImageHolderText = "Camera Image",
                                secondImageHolderText = "Camera Image",
                                buttonText = "Confirm"
                            )
                        )
                    )
                } else {
                    mOpenGalleryContract.launch(arrayOf("image/*"))
                }
            }.show()
    }

    private fun setSpinner() {
        val types = arrayOf("car_card", "bank_card", "national_card", "car_green_sheet","legal_driving_records_inquiry","gas_bill_khorasan_razavi_ocr","car_green_sheet_finder")
        binding.spinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, types).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
    }

    private fun setAdapter() {
        binding.rvOcrResults.layoutManager = LinearLayoutManager(this)
        binding.rvOcrResults.adapter = OcrResultAdapter(mutableListOf())
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm =
                getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus() // این خط باعث می‌شود فوکوس از روی EditText هم برداشته شود
        }
    }
    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (currentFocus != null) {
            hideKeyboard()
        }
        return super.dispatchTouchEvent(ev)
    }
}