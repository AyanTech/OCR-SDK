package ir.ayantech.ocr_sdk.ui.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import ir.ayantech.ocr_sdk.R
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.data.model.OcrSdkHookApiCallStatusEnum
import ir.ayantech.ocr_sdk.data.model.*
import ir.ayantech.ocr_sdk.dialog.OcrSdkOneOptionDialog
import ir.ayantech.ocr_sdk.tools.*
import ir.ayantech.ocr_sdk.tools.OcrHelper.encodeImageToBase64
import ir.ayantech.ocr_sdk.ui.viewmodel.OcrUiState
import ir.ayantech.ocr_sdk.ui.viewmodel.OcrViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class OcrSdkOcrFragment : OcrSdkBaseFragment() {

    private val viewModel: OcrViewModel by viewModel()

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        fun newInstance(
            cardType: String,
            extraInfo: String,
            backImageUri: Uri? = null
        ): OcrSdkOcrFragment {
            return OcrSdkOcrFragment().apply {
                this.cardType = cardType
                this.extraInfo = extraInfo
                this.backImageUri = backImageUri
            }
        }
    }

    var cardType: String by fragmentArgument("")
    var extraInfo: String by fragmentArgument("")

    private var progressShowing = false
    private var lastProgress = -1
    private var lastProgressAt = 0L

    private fun showProgress(message: String) {
        if (!isAdded) return
        ocrActivity.showProgress(message)
        progressShowing = true
    }

    private fun updateProgress(percent: Int, message: String) {
        if (!isAdded) return
        val now = System.currentTimeMillis()
        if (percent == lastProgress && now - lastProgressAt < 120) return
        lastProgress = percent; lastProgressAt = now
        ocrActivity.updateProgress(percent, message)
    }

    private fun hideProgress() {
        ocrActivity.hideProgress()
        progressShowing = false
        lastProgress = -1
    }

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

    private val takePictureContract =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
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
                    showToast(reason)
                    hideProgress()
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

    override fun onFragmentCreated() {
        super.onFragmentCreated()
        observeViewModel()
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
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is OcrUiState.Loading -> showProgress(getString(R.string.ocr_loading_description))
                        is OcrUiState.UploadSuccess -> {
                            fileID = state.fileId
                            uploading = true
                            viewModel.getCardOcrResult(state.fileId)
                        }

                        is OcrUiState.ResultSuccess -> handleApiResult(state.response)
                        is OcrUiState.Error -> {
                            hideProgress()
                            showToast(state.message)
                        }

                        is OcrUiState.Idle -> hideProgress()
                    }
                }
            }
        }
    }

    private fun handleApiResult(response: GetCardOcrResult.GetCardOcrResultResponseModel) {
        when (response.status) {
            OcrSdkHookApiCallStatusEnum.Successful.name -> {
                hideProgress()
                val data = ArrayList<GetCardOcrResult.OcrResult>()
                response.result?.forEach { data.add(GetCardOcrResult.OcrResult(it.key, it.value)) }
                OcrHelper.deleteCachedFileFromUri(requireActivity(), frontImageUri ?: "".toUri())
                OcrHelper.deleteCachedFileFromUri(requireActivity(), backImageUri ?: "".toUri())
                ocrActivity.sendData(data)
            }

            OcrSdkHookApiCallStatusEnum.Pending.name -> {
                delayed(response.nextCallInterval) {
                    viewModel.getCardOcrResult(fileID ?: "")
                }
            }

            OcrSdkHookApiCallStatusEnum.Failed.name -> {
                hideProgress()
                if (response.retryable) {
                    binding.btnSendImages.text = getString(R.string.retry_send)
                } else {
                    fileID = null
                    frontImageUri = null
                    backImageUri = null
                    showToast(getString(R.string.ocr_retry_again))
                    uploading = false
                }
            }
        }
    }

    private fun statusCheck() {
        if (frontImageUri.isNotNull()) {
            Glide.with(ocrActivity).load(frontImageUri).dontAnimate().priority(Priority.IMMEDIATE)
                .into(binding.captureA.circularImg)
            binding.captureA.icCheck.visibility = View.VISIBLE
        }
        if (backImageUri.isNotNull()) {
            Glide.with(ocrActivity).load(backImageUri).dontAnimate().priority(Priority.IMMEDIATE)
                .into(binding.captureB.circularImg)
            binding.captureB.icCheck.visibility = View.VISIBLE
        }
        updateButtonStatus()
    }

    private fun updateButtonStatus() {
        if (frontImageUri.isNotNull() && (backImageUri.isNotNull() || ocrActivity.ocrConfig.singlePhoto == true)) {
            binding.btnSendImages.isEnabled = true
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
        if (fileID.isNull()) {
            val images =
                listOf(onCardBase64, backCardBase64 ?: "").filter { it?.isNotEmpty() == true }
            viewModel.uploadCardOcr(images, effectiveCardType())
        } else {
            viewModel.getCardOcrResult(fileID ?: "")
        }
    }
}
