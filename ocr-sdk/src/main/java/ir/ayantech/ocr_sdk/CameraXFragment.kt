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
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import ir.ayantech.whygoogle.helper.nullableFragmentArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt


class CameraXFragment(

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
    var backImageUri: Uri? by nullableFragmentArgument(null)
    var pictureNumber: Int by fragmentArgument(1)
    private var fileID: String? by nullableFragmentArgument(null)
    private lateinit var dialog: WaitingDialog
    private var compressing = false
    private var uploading = false
    private var OnCard= ""
    private var backOfCard= ""
    var cardType: String by fragmentArgument("")
    var extraInfo: String by fragmentArgument("")

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
            val contract = registerForActivityResult(ActivityResultContracts.TakePicture()) {
                Log.d(TAG, "contract: $it")
                if (!it) return@registerForActivityResult
                if (pictureNumber == 1)
                    frontImageUri = imageUri
                else
                    backImageUri = imageUri

                statusCheck()
            }
            dialog = WaitingDialog(
                requireContext(),
                getString(R.string.ocr_compressing)
            )

            headerRl.init(
                title = ocrActivity.getString(R.string.ocr_camera_desc)

            ) {
                ocrActivity.finishActivity()
            }
            statusCheck()
            if (ocrActivity.singlePhoto) {
                captureB.circularImageViewParent.visibility = View.GONE
                tvDescB.visibility = View.GONE
            }

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
            captureB.circularImg.setOnClickListener {
                if (!allPermissionsGranted()) {
                    requestPermissions()
                    return@setOnClickListener
                }
                val name = System.currentTimeMillis().toString()
                image = File(ocrActivity.filesDir, "$name.jpeg")
                pictureNumber = 2
                imageUri = createImageUri()
                contract.launch(imageUri)
            }
            btnSendImages.setOnClickListener {
                checkIfCallingAPI()

            }
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

        if (backImageUri.isNotNull()) {
            Glide.with(ocrActivity)
                .load(Uri.parse(backImageUri.toString()))
                .dontAnimate()
                .priority(Priority.IMMEDIATE)
                .into(binding.captureB.circularImg)
            binding.captureB.icCheck.visibility = View.VISIBLE

        }

        if (frontImageUri.isNotNull() && (backImageUri.isNotNull() || backImageUri?.equals("") == true)) {

            Glide.with(ocrActivity)
                .load(Uri.parse(frontImageUri.toString()))
                .dontAnimate()
                .priority(Priority.IMMEDIATE)
                .into(binding.captureA.circularImg)

            Glide.with(ocrActivity)
                .load(Uri.parse(backImageUri.toString()))
                .dontAnimate()
                .priority(Priority.IMMEDIATE)
                .into(binding.captureB.circularImg)
            binding.btnSendImages.isEnabled = true
            binding.captureA.icCheck.visibility = View.VISIBLE
            binding.captureB.icCheck.visibility = View.VISIBLE

        }
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

    private fun getRealPathFromURI(uri: Uri): String {
        var path = ""
        if (ocrActivity.contentResolver != null) {
            val cursor: Cursor? =
                uri.let { ocrActivity.contentResolver.query(it, null, null, null, null) }
            if (cursor != null) {
                cursor.moveToFirst()
                val idx: Int = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                path = cursor.getString(idx)
                cursor.close()
            }
        }
        return path
    }

    override fun callingApi(endPointName: String, value: String?) {

        when (endPointName) {

            OCRConstant.EndPoint_UploadCardOCR -> {

                Log.d(TAG, "compressing")

                dialog.changeText("در حال فشرده سازی...")
                dialog.showDialog()

                val job = coroutineScope.launch {
                    if (!compressing) {

                        try {
                            Log.d(TAG, "convert to base 64")

                            OnCard = encodeImageToBase64(frontImageUri)
                            if (backImageUri.isNotNull() && backImageUri?.equals("") == false)
                                backOfCard = encodeImageToBase64(backImageUri)
                            //Update UI
                            compressing = true
                        } catch (e: Exception) {
                            Log.d(TAG, "callingApiException: $e")
                        }


                    }
                }
                job.invokeOnCompletion {
                    try {
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (!uploading) {
                                Log.d(TAG, "!uploading")
                                dialog.changeText("در حال ارسال تصاویر...")
                                ayanApi.timeout = 90
                                ayanApi.ayanCall<UploadNewCardOcrImage.Output>(
                                    endPoint = OCRConstant.EndPoint_UploadCardOCR,
                                    input =
                                    UploadNewCardOcrImage.Input(
                                        ImageArray = listOf(
                                            OnCard,
                                            backOfCard
                                        ).filter { it.isNotEmpty() },
                                        Type = ocrActivity.cardType
                                    ),
                                    ayanCallStatus = AyanCallStatus {
                                        success { output ->
                                            val response = output.response?.Parameters
                                            uploading = true
                                            Log.d(TAG, "callingApi File ID: ${response?.FileID}")
                                            fileID = response?.FileID
                                            OCRConstant.EndPoint_GetCardOcrResult?.let {
                                                callingApi(
                                                    endPointName = it,
                                                    response?.FileID
                                                )
                                            }
                                        }
                                        failure {
                                            dialog.hideDialog()
                                            this.ayanCommonCallingStatus?.dispatchFail(it)
                                            Log.d(
                                                TAG,
                                                "calling UploadNewCardOcrImage api failure: $it"
                                            )

                                        }
                                    }
                                )

                            } else {
                                OCRConstant.EndPoint_GetCardOcrResult?.let {
                                    callingApi(
                                        endPointName = it,
                                        fileID
                                    )
                                    Log.d(
                                        TAG,
                                        "uploading -> EndPoint_GetCardOcrResult api call = fileID is= $fileID"
                                    )

                                }
                            }
                        }
                    } catch (e: Exception) {
                        dialog.hideDialog()
                        Log.d(TAG, "e: $e")
                    }
                }
            }


            OCRConstant.EndPoint_GetCardOcrResult -> {
                Log.d(TAG, "uploading -> EndPoint_GetCardOcrResult api call = fileID is= $value")
                dialog.showDialog()
                dialog.changeText(getString(R.string.ocr_downloading_data))
                ocrActivity.runOnUiThread {
                    Log.d(TAG, "callingApi: ")
                    ayanApi.timeout = 10
                    ayanApi.ayanCall<GetCardOcrResult.Output>(
                        endPoint = OCRConstant.EndPoint_GetCardOcrResult,
                        input = value?.let { GetCardOcrResult.Input(FileID = it) },
                        ayanCallStatus = AyanCallStatus {
                            success { output ->
                                val response = output.response?.Parameters
                                when (response?.Status) {

                                    HookApiCallStatusEnum.Successful.name -> {
                                        dialog.hideDialog()

                                        val data = ArrayList<GetCardOcrResult.Result>()
                                        response.Result?.forEach {
                                            data.add(it)
                                        }
                                        ocrActivity.sendResult(data)
                                    }

                                    HookApiCallStatusEnum.Pending.name -> {

                                        delayed(response.NextCallInterval) {
                                            callingApi(
                                                endPointName = OCRConstant.EndPoint_GetCardOcrResult,
                                                value
                                            )
                                        }
                                    }

                                    HookApiCallStatusEnum.Failed.name -> {

                                        if (response.Retryable) {
                                            dialog.hideDialog()
                                            checkIfCallingAPI()
                                            binding.btnSendImages.text =
                                                getString(R.string.retry_send)
                                        } else {
                                            fileID = null
                                            dialog.hideDialog()
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
                                dialog.hideDialog()

                            }
                        }

                    )
                }
            }
        }
    }

    private fun captureAndSaveImage(imageNumber: Int) {
        pictureNumber = imageNumber
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        /*      // Generating file name
              val imageName = "test.png"
              val image = File(ocrActivity.cacheDir, imageName)

              val fileUri = Uri.fromFile(image);

            ;*/
        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    /* private fun takingPhoto() {
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
                         when (frontImageUri.isNull()) {
                             true ->
                                 start(ImageViewFragment().also {
                                     it.frontImageUri = outputFileResults.savedUri
                                 })

                             else -> start(
                                 ImageViewFragment().also {
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
     }*/


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val extras: Bundle? = data?.extras
            val imageBitmap: Bitmap? = extras?.get("data") as Bitmap?
            // Save the image to private storage
            if (pictureNumber == 1)
                frontImageUri = imageBitmap?.let { saveImageToPrivateStorage(it) }?.toUri()
            else
                backImageUri = imageBitmap?.let { saveImageToPrivateStorage(it) }?.toUri()
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

    private fun encodeImageToBase64(imageUri: Uri? = null): String {


        val bitmap = MediaStore.Images.Media.getBitmap(
            ocrActivity.contentResolver,
            imageUri
        )

        var outputStream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        val width = bitmap.width
        val height = bitmap.height

//        val portraitBitmap = height > width
        val largeSize = maxOf(width, height)
        val scaleFactor: Float = 1920f / largeSize.toFloat()

        if (largeSize > 1000) {
            //large image
            outputStream = ByteArrayOutputStream()
            val newBitmap = Bitmap.createScaledBitmap(
                bitmap,
                (width * scaleFactor).roundToInt(), (height * scaleFactor).roundToInt(), false
            )
            newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            Log.d("FinalW", newBitmap.width.toString())
            Log.d("FinalH", newBitmap.height.toString())
        }

        val byteArray: ByteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)

    }

    private fun checkIfCallingAPI() {
        if (fileID.isNull())
            callingApi(endPointName = OCRConstant.EndPoint_UploadCardOCR)
        else
            callingApi(endPointName = OCRConstant.EndPoint_GetCardOcrResult)
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        if (::dialog.isInitialized)
            dialog.hideDialog()
        compressing = false
        uploading = false
        super.onDestroy()
    }
}

