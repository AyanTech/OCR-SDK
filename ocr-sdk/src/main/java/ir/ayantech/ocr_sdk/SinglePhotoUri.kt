package ir.ayantech.ocr_sdk

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import ir.ayantech.ayannetworking.api.AyanCallStatus
import ir.ayantech.ocr_sdk.component.WaitingDialog
import ir.ayantech.ocr_sdk.component.init
import ir.ayantech.ocr_sdk.databinding.OcrFragmentCameraxBinding
import ir.ayantech.ocr_sdk.model.GetCardOcrResult
import ir.ayantech.ocr_sdk.model.HookApiCallStatusEnum
import ir.ayantech.ocr_sdk.model.UploadNewCardOcrImage
import ir.ayantech.whygoogle.helper.delayed
import ir.ayantech.whygoogle.helper.fragmentArgument
import ir.ayantech.whygoogle.helper.isNotNull
import ir.ayantech.whygoogle.helper.isNull
import ir.ayantech.whygoogle.helper.makeGone
import ir.ayantech.whygoogle.helper.nullableFragmentArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt


class SinglePhotoUri(

) :
    BaseFragment<OcrFragmentCameraxBinding>() {
    val REQUEST_IMAGE_CAPTURE = 1


    override val showingHeader: Boolean
        get() = false
    override val showingFooter: Boolean
        get() = false
    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> OcrFragmentCameraxBinding
        get() = OcrFragmentCameraxBinding::inflate
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    var frontImageUri: Uri? by nullableFragmentArgument(null)
    var pictureNumber: Int by fragmentArgument(1)
    private var fileID: String? by nullableFragmentArgument(null)
     private var compressing = false
    private var uploading = false
    private var OnCard = ""

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            if (allPermissionsGranted()) {
                binding.captureA.circularImg.performClick()
            } else {
                val permanentlyDenied = CameraXFragment.Companion.REQUIRED_PERMISSIONS.any { permission ->
                    !ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        permission
                    )
                }

                if (permanentlyDenied) {
                    // رد دائمی (don't ask again)
                    showGoToSettingsDialog()
                } else {
                    // رد موقت
                    showPermissionRationaleDialog()
                }
            }
        }

    private fun showPermissionRationaleDialog() {
        OneOptionDialog(
            title = getString(R.string.ocr_permission_request_msg),
            buttonText = getString(R.string.ocr_permission_open_setting_msg),
            context = requireActivity()
        ) {
            requestPermissions()
        }.show()

    }

    private fun showGoToSettingsDialog() {
        OneOptionDialog(
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


    var image: File? by nullableFragmentArgument(null)
    var imageUri: Uri? by nullableFragmentArgument(null)
    fun createImageUri(): Uri? {
        return image?.let {
            FileProvider.getUriForFile(
                ocrActivity,
                "${OCRConstant.Application_ID}.library.file.provider",
                it
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        accessViews {
            statusCheck()
            binding.captureB.root.makeGone()
            binding.tvDescB.makeGone()
            val contract = registerForActivityResult(ActivityResultContracts.TakePicture()) {
                Log.d(TAG, "contract: $it")
                if (!it) return@registerForActivityResult
                frontImageUri = imageUri

                statusCheck()
            }

            headerRl.init(
                title = ocrActivity.getString(R.string.ocr_taking_image)

            ) {
                ocrActivity.finishActivity()
            }
            statusCheck()


            binding.captureA.circularImg.setOnClickListener {
                if (!allPermissionsGranted()) {
                    requestPermissions()
                    return@setOnClickListener
                }
                val name = System.currentTimeMillis().toString()
                image = File(ocrActivity.filesDir, "$name.jpeg")
                pictureNumber = 1
                imageUri = createImageUri()
                contract.launch(imageUri)

            }
            btnSendImages.setOnClickListener {
                ocrActivity.sendUri(frontImageUri.toString())
            }
            captureA.circularImg.performClick()
        }
    }

    private fun statusCheck() {
        if (frontImageUri.isNotNull()) {
            Glide.with(ocrActivity)
                .load(Uri.parse(frontImageUri.toString()))
                .dontAnimate()
                .priority(Priority.IMMEDIATE)
                .into(binding.captureA.circularImg)
            binding.captureA.icCheck.visibility = View.VISIBLE

        }

        if (frontImageUri.isNotNull()) {

            Glide.with(ocrActivity)
                .load(Uri.parse(frontImageUri.toString()))
                .dontAnimate()
                .priority(Priority.IMMEDIATE)
                .into(binding.captureA.circularImg)
            binding.btnSendImages.isEnabled = true
            binding.captureA.icCheck.visibility = View.VISIBLE

        }
    }


    private fun requestPermissions() {

        permissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {

            } else {
                showToast("Permissions not granted by the user.")
                requireActivity().finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 10
        val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
            ).toTypedArray()
    }

    override fun init() {
    }

    override fun viewListeners() {
    }

    override fun callingApi(endPointName: String, value: String?) {
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val extras: Bundle? = data?.extras
            val imageBitmap: Bitmap? = extras?.get("data") as Bitmap?
            // Save the image to private storage
                frontImageUri = imageBitmap?.let { saveImageToPrivateStorage(it) }?.toUri()
        }
        statusCheck()
    }

    private fun saveImageToPrivateStorage(bitmap: Bitmap): String? {
        val fileName = System.currentTimeMillis().toString()
        try {

            requireContext().openFileOutput(fileName, Context.MODE_PRIVATE).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                "${requireContext().filesDir}/$fileName"
                return Uri.fromFile(ocrActivity.getFileStreamPath(fileName)).toString()

            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    override fun onDestroy() {
        coroutineScope.cancel()
         compressing = false
        uploading = false
        super.onDestroy()
    }
}

