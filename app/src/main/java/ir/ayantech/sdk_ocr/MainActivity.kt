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
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.chuckerteam.chucker.api.ChuckerCollector
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

    // تعریف منیجر به صورت یکپارچه برای جلوگیری از نشت حافظه
    private lateinit var ocrManager: OcrNetworkManager

    private val urlContract = registerForActivityResult(CaptureContract()) { res ->
        compressImage(res?.uri, res?.extraInfo.toString())
    }

    private val mOpenGalleryContract = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            compressImage(it, isProcessingFront)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OCRConstant.context = this
        initOCR()

        // مقداردهی اولیه منیجر با لیستنر بج چاکر
        ocrManager = OcrNetworkManager(this) {
            runOnUiThread { showChuckerBadge() }
        }

        setSpinner()
        setAdapter()
        setClickListeners()
      //  checkPermissions()
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
        binding.layoutFrontPhoto.setOnClickListener { hideKeyboard(); openSelectionDialog("0") }
        binding.layoutBackPhoto.setOnClickListener { hideKeyboard(); openSelectionDialog("1") }
        binding.btnSend.setOnClickListener {
            hideKeyboard()
            if (isRequesting) cancelRequest() else sendRequest()
        }
        binding.btnOpenChucker.setOnClickListener {
            hideChuckerBadge()
            try {
                startActivity(com.chuckerteam.chucker.api.Chucker.getLaunchIntent(this))
            } catch (e: Exception) {
                Toast.makeText(this, "Chucker is not available", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnResetData.setOnClickListener {
            resetData()
            Toast.makeText(this, "All data and logs cleared 🧹", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideChuckerBadge() {
        binding.chuckerBadge.animate()
            .scaleX(0f).scaleY(0f)
            .setDuration(200)
            .withEndAction { binding.chuckerBadge.visibility = View.GONE }
            .start()
    }

    private fun sendRequest() {
        if (cardFrontBase64 == null) {
            Toast.makeText(this, "Please select front image", Toast.LENGTH_SHORT).show()
            return
        }


        binding.tvStatusCode.text = "---"
        binding.tvDescription.text = "Processing..."
        (binding.rvOcrResults.adapter as? OcrResultAdapter)?.updateData(mutableListOf())

        toggleLoading(true)

        lifecycleScope.launch(Dispatchers.Default) {
            currentCall = ocrManager.sendOcrRequest(
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
                            (binding.rvOcrResults.adapter as? OcrResultAdapter)?.updateData(data.ocrRowModelList ?: mutableListOf())
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
                btnSend.text = "Cancel Request"
                btnSend.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E57373"))
                loadingProgress.visibility = View.VISIBLE
            } else {
                btnSend.text = "Analyze Card"
                btnSend.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00ACC1"))
                loadingProgress.visibility = View.GONE
            }
        }
    }

    private fun getRandomFunnyMessage(): String {
        return listOf("Putting the image on a diet... 🥗", "Shrinking pixels... 🤏", "Making it slim... ✨", "Feeding the engine... 🍕").random()
    }

    private fun compressImage(uri: Uri?, isFront: String) {
        if (uri == null) return
        val dialog = OcrSdkWaitingDialog(this, getRandomFunnyMessage())
        dialog.showDialog()

        lifecycleScope.launch {
            val maxS = binding.edMax.text.toString().toDoubleOrNull() ?: 1.0
            val minS = binding.edMin.text.toString().toDoubleOrNull() ?: 0.7
            OcrHelper.encodeImageToBase64(this@MainActivity, uri, maxS, minS,
                listener = object : EncodeImageListenerWithMetrics {
                    override fun onSuccess(base64Str: String, metrics: EncodeMetrics) {
                        dialog.hideDialog()
                        if (isFront == "0") cardFrontBase64 = base64Str else cardBackBase64 = base64Str
                        val targetImg = if (isFront == "0") binding.frontImage else binding.backImage
                        Glide.with(this@MainActivity).asBitmap().load(Base64.decode(base64Str, Base64.DEFAULT)).transform(CenterCrop()).into(targetImg)
                        targetImg.imageTintList = null
                        targetImg.alpha = 1.0f
                        targetImg.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                        targetImg.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                        targetImg.scaleType = ImageView.ScaleType.CENTER_CROP
                        OcrHelper.deleteCachedFileFromUri(this@MainActivity, uri)
                    }
                    override fun onFailed(reason: String, t: Throwable?) { dialog.hideDialog() }
                    override fun onProgress(p: Int, m: String) {}
                    override fun onSuccess(base64: String) {}
                })
        }
    }

    private fun resetData() {
        cardFrontBase64 = null
        cardBackBase64 = null
        binding.tvStatusCode.text = "---"
        binding.tvDescription.text = "No request sent yet"
        binding.tvTimeSpent.text = "0 ms"
        (binding.rvOcrResults.adapter as? OcrResultAdapter)?.updateData(mutableListOf())

        // پاک کردن لاگ‌های چاکر هنگام ریست
        ocrManager.clearChuckerLogs()
        hideChuckerBadge()

        with(binding) {
            val resetImg = { img: ImageView ->
                img.setImageResource(ir.ayantech.ocr_sdk.R.drawable.ocr_ic_camera)
                img.imageTintList = ColorStateList.valueOf(Color.parseColor("#00ACC1"))
                img.alpha = 0.6f
                img.scaleType = ImageView.ScaleType.CENTER_CROP
                val size = (56 * resources.displayMetrics.density).toInt()
                img.layoutParams.width = size
                img.layoutParams.height = size
            }
            resetImg(frontImage)
            resetImg(backImage)
        }
    }

    private fun openSelectionDialog(isFront: String) {
        isProcessingFront = isFront
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Select Photo Source")
            .setItems(arrayOf("Camera", "Gallery")) { _, which ->
                if (which == 0) {
                    urlContract.launch(OcrSdkCaptureConfig(
                        className = "ir.ayantech.sdk_ocr.MainActivity",
                        extraInfo = isFront,
                        textBlock = OcrSdkTextBlock(title= "OCR", firstImageHolderText = "Camera", secondImageHolderText = "Camera", buttonText = "Confirm")))
                } else { mOpenGalleryContract.launch(arrayOf("image/*")) }
            }.show()
    }

    private fun setSpinner() {
        val types = arrayOf("car_card", "bank_card", "national_card", "car_green_sheet","legal_driving_records_inquiry","gas_bill_khorasan_razavi_ocr","car_green_sheet_finder")
        binding.spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types).apply {
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
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (currentFocus != null && ev?.action == android.view.MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is android.widget.EditText) {
                val outRect = android.graphics.Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) hideKeyboard()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun showChuckerBadge() {
        if (binding.chuckerBadge.isVisible) return
        binding.chuckerBadge.visibility = View.VISIBLE
        binding.chuckerBadge.scaleX = 0f
        binding.chuckerBadge.scaleY = 0f
        binding.chuckerBadge.animate()
            .scaleX(1f).scaleY(1f)
            .setDuration(300)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .start()
    }
}