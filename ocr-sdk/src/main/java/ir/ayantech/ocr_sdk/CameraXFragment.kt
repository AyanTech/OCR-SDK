package ir.ayantech.ocr_sdk

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
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
import ir.ayantech.whygoogle.helper.isNotNull
import ir.ayantech.whygoogle.helper.isNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable.start
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
    var frontImageUri: Uri? = null
    var backImageUri: Uri? = null
    var pictureNumber = 1
    private var fileID: String? = null
    private lateinit var dialog: WaitingDialog
    private var compressing = false
    private var uploading = false
    private var OnCard = ""
    private var backOfCard = ""

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


    override fun onCreate() {
        super.onCreate()
        accessViews {

            dialog = WaitingDialog(
                requireContext(),
                getString(R.string.ocr_compressing)
            )

            headerRl.init(
                title = when (frontImageUri.isNull()) {
                    true -> ocrActivity.getString(R.string.ocr_camera_description_front)
                    else -> ocrActivity.getString(R.string.ocr_camera_description_back)
                }
            ) {
                ocrActivity.finishActivity()
            }
            statusCheck()
            if (ocrActivity.singlePhoto) {
                captureB.circularImageViewParent.visibility = View.GONE
                tvDescB.visibility = View.GONE
            }
            captureA.circularImg.setOnClickListener {
                captureAndSaveImage(1)
            }
            captureB.circularImg.setOnClickListener {
                captureAndSaveImage(2)
            }
            btnSendImages.setOnClickListener {
                checkIfCallingAPI()
            }
        }

        // Request camera permissions
        /*      if (allPermissionsGranted()) {
                  startCamera()
              } else {
                  requestPermissions()
              }*/
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
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    override fun init() {
    }

    override fun viewListeners() {
    }

    private fun deleteImage(imageUri: Uri) {
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                Log.d(TAG, "deleteImage: call > P")

                val fDelete = File(getRealPathFromURI(imageUri))
                if (fDelete.exists()) {
                    Log.d(TAG, "deleteImage: Done > P")
                    fDelete.delete()
                }
            } else {
                Log.d(TAG, "deleteImage: call < P")

                val uri = Uri.parse(imageUri.toString())
                val projection = arrayOf(MediaStore.Images.Media.DATA)
                val cursor: Cursor? =
                    requireActivity().contentResolver.query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    val filePath = cursor.getString(columnIndex)

                    // Delete the file
                    val file = File(filePath)
                    if (file.exists()) {
                        if (file.delete()) {
                            Log.d(TAG, "deleteImage: Done < P")
                        } else {
                            Log.d(TAG, "deleteImage: Failed")

                        }
                    } else {
                        Log.d(TAG, "deleteImage: Not Exist")

                    }
                    cursor.close()
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "deleteImage: $e")
        }
        compressing = false
        uploading = false
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

            Constant.EndPoint_UploadCardOCR -> {

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
                                    endPoint = Constant.EndPoint_UploadCardOCR,
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
                                            Constant.EndPoint_GetCardOcrResult?.let {
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
                                Constant.EndPoint_GetCardOcrResult?.let {
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


            Constant.EndPoint_GetCardOcrResult -> {
                Log.d(TAG, "uploading -> EndPoint_GetCardOcrResult api call = fileID is= $value")

                dialog.changeText(getString(R.string.ocr_downloading_data))
                ocrActivity.runOnUiThread {
                    Log.d(TAG, "callingApi: ")
                    ayanApi.timeout = 10
                    ayanApi.ayanCall<GetCardOcrResult.Output>(
                        endPoint = Constant.EndPoint_GetCardOcrResult!!,
                        input = value?.let { GetCardOcrResult.Input(FileID = it) },
                        ayanCallStatus = AyanCallStatus {
                            success { output ->
                                val response = output.response?.Parameters
                                when (response?.Status) {

                                    HookApiCallStatusEnum.Successful.name -> {
                                        dialog.hideDialog()
                                        backImageUri?.let { back -> deleteImage(back) }
                                        frontImageUri?.let { front -> deleteImage(front) }


                                        val data = ArrayList<GetCardOcrResult.Result>()
                                        response.Result?.forEach {
                                            data.add(it)
                                        }
                                        ocrActivity.sendResult(data)
                                    }

                                    HookApiCallStatusEnum.Pending.name -> {

                                        delayed(response.NextCallInterval) {
                                            callingApi(
                                                endPointName = Constant.EndPoint_GetCardOcrResult,
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
                                            backImageUri?.let { back -> deleteImage(back) }
                                            frontImageUri?.let { front -> deleteImage(front) }
                                            frontImageUri = null
                                            backImageUri = null
                                            Toast.makeText(
                                                context,
                                                getString(R.string.ocr_retry_again),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            compressing = false
                                            uploading = false

                                            start(CameraXFragment())

                                        }
                                    }
                                }
                            }
                            failure {
                                this.ayanCommonCallingStatus?.dispatchFail(it)
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

        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

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
            callingApi(endPointName = Constant.EndPoint_UploadCardOCR)
        else
            callingApi(endPointName = Constant.EndPoint_GetCardOcrResult)
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        dialog.hideDialog()
        compressing = false
        uploading = false
        backImageUri?.let { back -> deleteImage(back) }
        frontImageUri?.let { front -> deleteImage(front) }
        super.onDestroy()
    }
}

