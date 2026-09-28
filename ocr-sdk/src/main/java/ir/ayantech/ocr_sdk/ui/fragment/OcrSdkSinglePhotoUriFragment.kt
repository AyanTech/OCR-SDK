package ir.ayantech.ocr_sdk.ui.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import ir.ayantech.ocr_sdk.R
import ir.ayantech.ocr_sdk.dialog.OcrSdkOneOptionDialog
import ir.ayantech.ocr_sdk.tools.OCRConstant
import ir.ayantech.ocr_sdk.tools.isNotNull
import ir.ayantech.ocr_sdk.tools.nullableFragmentArgument
import java.io.File

class OcrSdkSinglePhotoUriFragment : OcrSdkBaseFragment() {

    var frontImageUri: Uri? by nullableFragmentArgument(null)
    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (allPermissionsGranted().not()) {
                val permanentlyDenied =
                    requiredPermissions.any { permission ->
                        !ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(),
                            permission
                        )
                    }

                if (permanentlyDenied) {
                    showGoToSettingsDialog()
                } else {
                    showPermissionRationaleDialog()
                }
            }
        }

    private fun showPermissionRationaleDialog() {
        OcrSdkOneOptionDialog(
            title = getString(R.string.ocr_permission_request_msg),
            buttonText = getString(R.string.ocr_permission_open_setting_msg),
            context = requireActivity()
        ) {
            requestPermissions()
        }.show()
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

    override fun onFragmentCreated() {
        super.onFragmentCreated()
        accessViews {
            binding.captureB.root.visibility = View.GONE
            binding.tvDescB.visibility = View.GONE
            val contract = registerForActivityResult(ActivityResultContracts.TakePicture()) {
                if (!it) return@registerForActivityResult
                frontImageUri = imageUri
                ocrActivity.sendUri(frontImageUri)
            }
            statusCheck()

            binding.captureA.circularImg.setOnClickListener {
                if (!allPermissionsGranted()) {
                    requestPermissions()
                    return@setOnClickListener
                }
                val name = System.currentTimeMillis().toString()
                image = File(ocrActivity.filesDir, "$name.jpeg")
                imageUri = createImageUri()
                contract.launch(imageUri)
            }
            btnSendImages.setOnClickListener {
                ocrActivity.sendUri(frontImageUri)
            }
        }
    }

    private fun statusCheck() {
        if (frontImageUri.isNotNull()) {
            Glide.with(ocrActivity)
                .load(frontImageUri.toString().toUri())
                .dontAnimate()
                .priority(Priority.IMMEDIATE)
                .into(binding.captureA.circularImg)
            binding.btnSendImages.isEnabled = true
            binding.captureA.icCheck.visibility = View.VISIBLE
        }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(requiredPermissions)
    }

    private fun allPermissionsGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        fun newInstance(): OcrSdkSinglePhotoUriFragment {
            return OcrSdkSinglePhotoUriFragment()
        }
    }

}
