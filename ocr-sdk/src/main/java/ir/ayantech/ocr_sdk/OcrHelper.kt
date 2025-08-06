package ir.ayantech.ocr_sdk

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object OcrHelper {
    /**
     * Converts an image to Base64 string.
     *
     * @param maxSizeInMb Recommended range is between 3 and 5 MB. Values outside this range may impact quality or performance.
     */
    suspend fun encodeImageToBase64(
        context: Context,
        imageUri: Uri?,
        maxSizeInMb: Int = 3  // حداکثر حجم base64 به مگابایت
    ): String = withContext(Dispatchers.IO) {
         if (imageUri == null) return@withContext ""

        val originalBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)

        val maxSizeInBytes = maxSizeInMb * 1024 * 1024

        var quality = 100
        var scale = 1.0f

        var encodedString = ""
        var currentBitmap = originalBitmap

        var qualityStep = 5
        var scaleStep = 0.025f

        do {
            val width = (originalBitmap.width * scale).toInt()
            val height = (originalBitmap.height * scale).toInt()

            if (currentBitmap != originalBitmap) {
                currentBitmap.recycle()
            }

            currentBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)

            val outputStream = ByteArrayOutputStream()
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()

            encodedString = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            val base64SizeInBytes = encodedString.toByteArray(Charsets.UTF_8).size

            if (base64SizeInBytes <= maxSizeInBytes || (quality <= 10 && scale <= 0.5f)) {
                break
            }

            if ((base64SizeInBytes / maxSizeInBytes) > 10){
                qualityStep = 10
                scaleStep = 0.2f
            }


                if (quality > 10) {
                    quality -= qualityStep
                    if (quality < 10) quality = 10
                }

            if (scale > 0.5f) {
                scale -= scaleStep
                if (scale < 0.5f) scale = 0.5f
            }

        } while (true)

        Log.d(
            "Base64Size",
            "${encodedString.toByteArray(Charsets.UTF_8).size / 1024} KB at quality $quality scale $scale"
        )

        if (currentBitmap != originalBitmap) {
            currentBitmap.recycle()
        }

        return@withContext encodedString
    }

}