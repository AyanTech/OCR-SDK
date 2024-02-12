package ir.ayantech.ocr_sdk

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import ir.ayantech.ocr_sdk.component.init
import ir.ayantech.ocr_sdk.databinding.OcrFragmentCameraxBinding
import ir.ayantech.whygoogle.helper.isNull
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

typealias LumaListener = (luma: Double) -> Unit

private val FRAME_THRESHOLD = TimeUnit.SECONDS.toMillis(1)

class CameraXFragment(

) :
    BaseFragment<OcrFragmentCameraxBinding>() {

    override val showingHeader: Boolean
        get() = false
    override val showingFooter: Boolean
        get() = false
    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> OcrFragmentCameraxBinding
        get() = OcrFragmentCameraxBinding::inflate

    // This property is only valid between onCreateView and
    // onDestroyView.
    /*private val baseLoaderCallback = object : BaseLoaderCallback(requireContext()) {
       override fun onManagerConnected(status: Int) {
           when (status) {
               LoaderCallbackInterface.SUCCESS -> {
                   Log.d(TAG, "OpenCV loaded successfully")
                   processImages()
               }
               else -> super.onManagerConnected(status)
           }
       }
   }*/
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var context: Context
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var contentResolver: ContentResolver
    var frontImageUri: Uri? = null
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
            headerRl.init(
                title = when (frontImageUri.isNull()) {
                    true -> context.getString(R.string.ocr_camera_description_front)
                    else -> context.getString(R.string.ocr_camera_description_back)
                }
            ) {
                ocrActivity.finishActivity()
            }
            // Set up the listeners for take photo and video capture buttons
            captureB.setOnClickListener {
                takePhoto()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return
        // Create time stamped name and MediaStore entry.
        val name = System.currentTimeMillis().toString()
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }
        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()
        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults) {
                    /*        val msg = "Photo capture succeeded: ${output.savedUri}"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            Log.d(TAG, msg)*/
                    try {
                        //Image is captured and saved
                        when (frontImageUri.isNull()) {
                            true ->
                                start(ImageViewFragment().also {
                                    it.frontImageUri = output.savedUri
                                })

                            else -> start(
                                ImageViewFragment().also {
                                    it.frontImageUri = frontImageUri
                                    it.backImageUri = output.savedUri
                                })
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        )
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
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
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
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
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

