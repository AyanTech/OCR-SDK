package ir.ayantech.ocr_sdk.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import ir.ayantech.ayannetworking.api.AyanCallStatus
import ir.ayantech.ocr_sdk.R
import ir.ayantech.ocr_sdk.component.OcrSdkWaitingDialog
import ir.ayantech.ocr_sdk.dialog.OcrSdkOneOptionDialog
import ir.ayantech.ocr_sdk.model.OcrSdkGetCardOcrResult
import ir.ayantech.ocr_sdk.model.OcrSdkHookApiCallStatusEnum
import ir.ayantech.ocr_sdk.model.OcrSdkUploadNewCardOcrImage
import ir.ayantech.ocr_sdk.tools.EncodeImageListener
import ir.ayantech.ocr_sdk.tools.OCRConstant
import ir.ayantech.ocr_sdk.tools.OcrHelper
import ir.ayantech.ocr_sdk.tools.OcrHelper.encodeImageToBase64
import ir.ayantech.whygoogle.helper.delayed
import ir.ayantech.whygoogle.helper.fragmentArgument
import ir.ayantech.whygoogle.helper.isNotNull
import ir.ayantech.whygoogle.helper.isNull
import ir.ayantech.whygoogle.helper.nullableFragmentArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class OcrSdkOcrFragment : OcrSdkBaseFragment() {

    companion object {
        private const val TAG = "OcrSdkOcrFragment"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    //region Data's...
    var cardType: String by fragmentArgument("")
    var extraInfo: String by fragmentArgument("")
    //endregion


    //region Ocr Sdk Waiting Dialog...
    private var progressDialog: OcrSdkWaitingDialog? = null
    private var progressShowing = false
    private var lastProgress = -1
    private var lastProgressAt = 0L
    //endregion

    private fun showProgress(message: String) {
        if (!isAdded) return
        if (progressDialog == null) {
            progressDialog = OcrSdkWaitingDialog(requireContext(), message)
        } else {
            progressDialog?.changeText(message)
        }
        if (!progressShowing) {
            progressDialog?.showDialog()
            progressShowing = true
        }
    }

    private fun updateProgress(percent: Int, message: String) {
        if (!isAdded) return
        val now = System.currentTimeMillis()
        if (percent == lastProgress && now - lastProgressAt < 120) return
        lastProgress = percent; lastProgressAt = now
        progressDialog?.update(message, percent)
    }

    private fun hideProgress() {
        progressDialog?.hideDialog()
        progressShowing = false
        lastProgress = -1
    }

    // ---------- Fragment state ----------
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    var frontImageUri: Uri? by nullableFragmentArgument(null)
    var backImageUri: Uri? by nullableFragmentArgument(null)
    var pictureNumber: Int by fragmentArgument(1)

    private var fileID: String? by nullableFragmentArgument(null)
    private var compressing = false
    private var uploading = false

    private var onCardBase64: String? = null
    private var backCardBase64: String? = null

    private var image: File? by nullableFragmentArgument(null)
    private var imageUri: Uri? by nullableFragmentArgument(null)

    private fun createImageUri(): Uri? =
        image?.let {
            try {
                FileProvider.getUriForFile(
                    ocrActivity,
                    "${OCRConstant.Application_ID}.library.file.provider",
                    it
                )
            } catch (e: Exception) {
                Log.d(TAG, "FileProviderError: $e"); null
            }
        }

    // ---------- Permissions ----------
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            if (allPermissionsGranted()) {
                binding.captureA.circularImg.performClick()
            } else {
                val permanentlyDenied = REQUIRED_PERMISSIONS.any { permission ->
                    !ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        permission
                    )
                }
                if (permanentlyDenied) showGoToSettingsDialog() else showPermissionRationaleDialog()
            }
        }

    private fun showPermissionRationaleDialog() {
        OcrSdkOneOptionDialog(
            title = getString(R.string.ocr_permission_request_msg),
            buttonText = getString(R.string.ocr_permission_open_setting_msg),
            context = requireActivity()
        ) { requestPermissions() }.show()
    }

    private fun showGoToSettingsDialog() {
        OcrSdkOneOptionDialog(
            title = getString(R.string.ocr_permission_using_setting_msg),
            buttonText = getString(R.string.ocr_permission_open_setting_msg),
            context = requireActivity()
        ) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context?.packageName, null)
            }
            startActivity(intent)
        }.show()
    }

    private fun requestPermissions() {
        permissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    // ---------- Camera contract ----------
    private val takePictureContract =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            Log.d(TAG, "TakePicture: $ok")
            if (!ok) return@registerForActivityResult

            val picked = imageUri ?: return@registerForActivityResult

            val listener = object : EncodeImageListener {
                override fun onProgress(percent: Int, message: String) {
                    updateProgress(percent, message)
                }

                override fun onSuccess(base64: String) {
                    if (pictureNumber == 1) {
                        onCardBase64 = base64
                        frontImageUri = picked
                    } else {
                        backCardBase64 = base64
                        backImageUri = picked
                    }
                    hideProgress()
                    statusCheck()
                    compressing = false
                }

                override fun onFailed(reason: String, throwable: Throwable?) {
                    Toast.makeText(ocrActivity, reason, Toast.LENGTH_SHORT).show()
                    hideProgress()
                    Log.d(TAG, "onFailed: $reason + $throwable")

                    compressing = false
                }
            }

            if (compressing) return@registerForActivityResult
            compressing = true
            showProgress(getString(R.string.ocr_compressing))

            lifecycleScope.launch {
                encodeImageToBase64(
                    context = ocrActivity,
                    imageUri = picked,
                    maxBase64Mb = ocrActivity.ocrConfig.maxBase64Mb ?: 3.0,
                    minBase64Mb = ocrActivity.ocrConfig.minBase64Mb ?: 2.0,
                    listener = listener
                )
            }
        }

    // ---------- Lifecycle ----------
    override fun onCreate() {
        super.onCreate()
        accessViews {
            statusCheck()

            captureA.circularImg.setOnClickListener {
                if (!allPermissionsGranted()) {
                    requestPermissions(); return@setOnClickListener
                }
                val name = System.currentTimeMillis().toString()
                image = File(ocrActivity.filesDir, "$name.jpeg")
                pictureNumber = 1
                imageUri = createImageUri()
                takePictureContract.launch(imageUri)
            }

            captureB.circularImg.setOnClickListener {
                if (!allPermissionsGranted()) {
                    requestPermissions(); return@setOnClickListener
                }
                val name = System.currentTimeMillis().toString()
                image = File(ocrActivity.filesDir, "$name.jpeg")
                pictureNumber = 2
                imageUri = createImageUri()
                takePictureContract.launch(imageUri)
            }

            btnSendImages.setOnClickListener {
                checkIfCallingAPI()
            }

            if (ocrActivity.ocrConfig.singlePhoto == true) {
                captureB.circularImageViewParent.visibility = View.GONE
                tvDescB.visibility = View.GONE
                if (allPermissionsGranted()) captureA.circularImg.performClick() else requestPermissions()
            }
        }
    }

    private fun statusCheck() {
        if (frontImageUri.isNotNull()) {
            Glide.with(ocrActivity)
                .load(frontImageUri)
                .dontAnimate()
                .priority(Priority.IMMEDIATE)
                .into(binding.captureA.circularImg)
            binding.captureA.icCheck.visibility = View.VISIBLE
        }
        if (backImageUri.isNotNull()) {
            Glide.with(ocrActivity)
                .load(backImageUri)
                .dontAnimate()
                .priority(Priority.IMMEDIATE)
                .into(binding.captureB.circularImg)
            binding.captureB.icCheck.visibility = View.VISIBLE
        }
        updateButtonStatus()

    }

    private fun updateButtonStatus() {
        if (frontImageUri.isNotNull() && (backImageUri.isNotNull() || backImageUri?.equals("") == true)) {
            binding.btnSendImages.isEnabled = true
            binding.captureA.icCheck.visibility = View.VISIBLE
            binding.captureB.icCheck.visibility = View.VISIBLE
        }
    }

    private fun effectiveCardType(): String {
        val fromConfig = ocrActivity.ocrConfig.cardType
        return when {
            !fromConfig.isNullOrEmpty() -> fromConfig
            cardType.isNotEmpty() -> cardType
            else -> ""
        }
    }

    private fun checkIfCallingAPI() {
        if (fileID.isNull())
            callingApi(endPointName = OCRConstant.EndPoint_UploadCardOCR)
        else
            callingApi(endPointName = OCRConstant.EndPoint_GetCardOcrResult)
    }
    override fun callingApi(endPointName: String, value: String?) {
        when (endPointName) {
            OCRConstant.EndPoint_UploadCardOCR -> {
                try {
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (!uploading) {
                            progressDialog?.showDialog(getString(R.string.ocr_sending))
                            ayanApi.timeout = 90
                            val images = listOf(onCardBase64, backCardBase64 ?: "")
                                .filter { it?.isNotEmpty() == true }

                            val input = OcrSdkUploadNewCardOcrImage.Input(
                                ImageArray = images,
                                Type = effectiveCardType()
                                // , ExtraInfo = extraInfo.takeIf { it.isNotEmpty() }
                            )

                            ayanApi.ayanCall<OcrSdkUploadNewCardOcrImage.Output>(
                                endPoint = OCRConstant.EndPoint_UploadCardOCR,
                                input = input,
                                ayanCallStatus = AyanCallStatus {
                                    success { output ->
                                        uploading = true
                                        fileID = output.response?.Parameters?.FileID
                                        OCRConstant.EndPoint_GetCardOcrResult.let {
                                            callingApi(it, fileID)
                                        }
                                    }
                                    failure {
                                        hideProgress()
                                        this.ayanCommonCallingStatus?.dispatchFail(it)
                                    }
                                }
                            )

                        } else {
                            OCRConstant.EndPoint_GetCardOcrResult.let {
                                callingApi(it, fileID)
                            }
                        }
                    }
                } catch (e: Exception) {
                    hideProgress()
                    Log.d(TAG, "Upload error: $e")
                }
            }

            OCRConstant.EndPoint_GetCardOcrResult -> {
                progressDialog?.showDialog(getString(R.string.ocr_downloading_data))
                ocrActivity.runOnUiThread {
                    ayanApi.timeout = 10
                    ayanApi.ayanCall<OcrSdkGetCardOcrResult.Output>(
                        endPoint = OCRConstant.EndPoint_GetCardOcrResult,
                        input = value?.let { OcrSdkGetCardOcrResult.Input(FileID = it) },
                        ayanCallStatus = AyanCallStatus {
                            success { output ->
                                val response = output.response?.Parameters
                                when (response?.Status) {
                                    OcrSdkHookApiCallStatusEnum.Successful.name -> {
                                        hideProgress()
                                        val data = ArrayList<OcrSdkGetCardOcrResult.Result>()
                                        response.Result?.forEach { data.add(it) }
                                        OcrHelper.deleteCachedFileFromUri(
                                            requireActivity(),
                                            frontImageUri ?: "".toUri()
                                        )
                                        OcrHelper.deleteCachedFileFromUri(
                                            requireActivity(),
                                            backImageUri ?: "".toUri()
                                        )
                                        ocrActivity.sendData(data)
                                    }

                                    OcrSdkHookApiCallStatusEnum.Pending.name -> {
                                        delayed(response.NextCallInterval) {
                                            callingApi(OCRConstant.EndPoint_GetCardOcrResult, value)
                                        }
                                    }

                                    OcrSdkHookApiCallStatusEnum.Failed.name -> {
                                        if (response.Retryable) {
                                            hideProgress()
                                            checkIfCallingAPI()
                                            binding.btnSendImages.text =
                                                getString(R.string.retry_send)
                                        } else {
                                            fileID = null
                                            hideProgress()
                                            frontImageUri = null
                                            backImageUri = null
                                            Toast.makeText(
                                                context,
                                                getString(R.string.ocr_retry_again),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            compressing = false
                                            uploading = false
                                        }
                                    }
                                }
                            }
                            failure {
                                this.ayanCommonCallingStatus?.dispatchFail(it)
                                hideProgress()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val extras: Bundle? = data?.extras
            val imageBitmap: Bitmap? = extras?.get("data") as Bitmap?
            val uriStr = imageBitmap?.let { saveImageToPrivateStorage(it) }
            if (pictureNumber == 1) frontImageUri = uriStr?.toUri() else backImageUri =
                uriStr?.toUri()
        }
        statusCheck()
    }

    private fun saveImageToPrivateStorage(bitmap: Bitmap): String? {
        val fileName = System.currentTimeMillis().toString()
        return try {
            requireContext().openFileOutput(fileName, Context.MODE_PRIVATE).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                Uri.fromFile(ocrActivity.getFileStreamPath(fileName)).toString()
            }
        } catch (e: IOException) {
            e.printStackTrace(); null
        }
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        hideProgress()
        compressing = false
        uploading = false
        super.onDestroy()
    }
}
