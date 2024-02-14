package ir.ayantech.ocr_sdk

import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import ir.ayantech.ayannetworking.api.AyanCallStatus
import ir.ayantech.ocr_sdk.Constant.EndPoint_GetCardOcrResult
import ir.ayantech.ocr_sdk.Constant.EndPoint_UploadCardOCR
import ir.ayantech.ocr_sdk.component.WaitingDialog
import ir.ayantech.ocr_sdk.databinding.OcrFragmentImageViewBinding
import ir.ayantech.ocr_sdk.model.GetCardOcrResult
import ir.ayantech.ocr_sdk.model.HookApiCallStatusEnum
import ir.ayantech.ocr_sdk.model.UploadNewCardOcrImage
import ir.ayantech.whygoogle.helper.delayed
import ir.ayantech.whygoogle.helper.isNotNull
import ir.ayantech.whygoogle.helper.isNull
import ir.ayantech.whygoogle.helper.trying
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

class ImageViewFragment(
    /*    val mobileNumber: String,
        val hash: String?,
        val card: String,
        var fileID: String? = null,*/

) :
    BaseFragment<OcrFragmentImageViewBinding>() {

    private lateinit var dialog: WaitingDialog
    var frontImageUri: Uri? = null
    var backImageUri: Uri? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var compressing = false
    private var uploading = false
    private var OnCard = ""
    private var backOfCard = ""
    private var fileID: String? = null

    override val showingHeader: Boolean
        get() = false
    override val showingFooter: Boolean
        get() = false

    override fun init() {
        accessViews {
            dialog = WaitingDialog(
                requireContext(),
                getString(R.string.ocr_compressing)
            )
            capturedPictureIv.setImageURI(
                if (backImageUri.isNull()) frontImageUri else backImageUri
            )
        }
    }

    private fun checkIfCallingAPI() {
        if (fileID.isNull())
            callingApi(endPointName = EndPoint_UploadCardOCR)
        else
            callingApi(endPointName = EndPoint_GetCardOcrResult)
    }

    override fun viewListeners() {

        binding.btnTakePhoto.setOnClickListener {
            if (ocrActivity.singlePhoto) {
                checkIfCallingAPI()
                return@setOnClickListener
            }
            if (backImageUri.isNull()) {
                start(CameraXFragment().also {
                    it.frontImageUri = frontImageUri
                })
            } else {
                checkIfCallingAPI()
            }
        }

        binding.btnTryAgain.setOnClickListener {
            if (backImageUri.isNull()) {
                frontImageUri?.let { front -> deleteImage(front) }
                start(CameraXFragment())
            } else {
                backImageUri?.let { back -> deleteImage(back) }
                frontImageUri?.let { front -> deleteImage(front) }
                start(CameraXFragment())
            }
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


    private fun deleteImage(imageUri: Uri) {
        trying {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                val fDelete = File(getRealPathFromURI(imageUri))
                if (fDelete.exists()) {
                    fDelete.delete()
                }
            } else {
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
                            Log.d(TAG, "deleteImage: Done")
                        } else {
                            Log.d(TAG, "deleteImage: Failed")

                        }
                    } else {
                        Log.d(TAG, "deleteImage: Not Exist")

                    }
                    cursor.close()
                }
            }
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

            EndPoint_UploadCardOCR -> {

                Log.d(TAG, "compressing")

                dialog.changeText("در حال فشرده سازی...")
                dialog.showDialog()

                val job = coroutineScope.launch {
                    if (!compressing) {

                        try {
                            Log.d(TAG, "convert to base 64")

                            OnCard = encodeImageToBase64(frontImageUri)
                            if (backImageUri.isNotNull())
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
                                    endPoint = EndPoint_UploadCardOCR,
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
                                            EndPoint_GetCardOcrResult?.let {
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
                                EndPoint_GetCardOcrResult?.let {
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


            EndPoint_GetCardOcrResult -> {
                Log.d(TAG, "uploading -> EndPoint_GetCardOcrResult api call = fileID is= $value")

                dialog.changeText(getString(R.string.ocr_downloading_data))
                ocrActivity.runOnUiThread {
                    Log.d(TAG, "callingApi: ")
                    ayanApi.timeout = 10
                    ayanApi.ayanCall<GetCardOcrResult.Output>(
                        endPoint = EndPoint_GetCardOcrResult!!,
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
                                                endPointName = EndPoint_GetCardOcrResult,
                                                value
                                            )
                                        }
                                    }

                                    HookApiCallStatusEnum.Failed.name -> {

                                        if (response.Retryable) {
                                            dialog.hideDialog()
                                            checkIfCallingAPI()
                                            binding.btnTakePhoto.text =
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


    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> OcrFragmentImageViewBinding
        get() = OcrFragmentImageViewBinding::inflate

    override fun onDestroy() {
        coroutineScope.cancel()
        dialog.hideDialog()
        compressing = false
        uploading = false
        backImageUri?.let { back -> deleteImage(back) }
        frontImageUri?.let { front -> deleteImage(front) }
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
    }
}
