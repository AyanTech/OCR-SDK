package ir.ayantech.ocr_sdk

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import ir.ayantech.ocr_sdk.databinding.OcrCustomCameraBinding
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

typealias LumaListener = (luma: Double) -> Unit

private val FRAME_THRESHOLD = TimeUnit.SECONDS.toMillis(1)

class CustomCameraFragment(

) :
    BaseFragment<OcrCustomCameraBinding>() {

    override val showingHeader: Boolean
        get() = false
    override val showingFooter: Boolean
        get() = false
    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> OcrCustomCameraBinding
        get() = OcrCustomCameraBinding::inflate

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var context: Context
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var contentResolver: ContentResolver
    var frontImageUri: Uri? = null
    var imageNumber = 1
    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                showToast("Permission request denied")
            } else {
                startCamera()
            }
        }

    override fun onCreate() {
        super.onCreate()
        accessViews {
            // Set up the listeners for take photo and video capture buttons
            captureB.setOnClickListener {
                takingPhoto()
            }
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
        context.let {
            cameraProviderFuture.addListener(
                Runnable { cameraProvider },
                ContextCompat.getMainExecutor(it)
            )
        }
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
    }

    private fun takingPhoto() {
        val name = System.currentTimeMillis().toString()

        val file =
            File(ocrActivity.cacheDir, "/$name.jpg")

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture!!.takePicture(outputFileOptions,
            ContextCompat.getMainExecutor(ocrActivity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    try {
                        //Image is captured and saved
                        when (imageNumber) {
                             1 ->
                                start(CameraXFragment().also {
                                    it.frontImageUri = outputFileResults.savedUri
                                    it.backImageUri = "".toUri()
                                })

                           2 -> start(
                                CameraXFragment().also {
                                    it.frontImageUri = frontImageUri
                                    it.backImageUri = outputFileResults.savedUri
                                })
                        }
                    } catch (e: IOException) {
                        Log.d(TAG, "catch $e")
                    }
                }

                override fun onError(error: ImageCaptureException) {
                    Log.d(TAG, "onError $error")

                }
            }
        )
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                showToast("Permissions not granted by the user.")
                requireActivity().finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder()
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 10
        val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                 }
            }.toTypedArray()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = requireContext()
        contentResolver = context.contentResolver
    }

    override fun init() {
    }

    override fun viewListeners() {
    }

    override fun callingApi(endPointName: String, value: String?) {
    }
}

