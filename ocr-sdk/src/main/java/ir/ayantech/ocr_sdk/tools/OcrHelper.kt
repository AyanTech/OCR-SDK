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
import java.io.FileNotFoundException

object OcrHelper {
    object Actions {
        const val CAPTURE_URI = "ir.ayantech.ocr_sdk.action.CAPTURE_URI"
        const val OCR_RETURN_DATA = "ir.ayantech.ocr_sdk.action.CAPTURE_AND_UPLOAD"
    }
    object Extras {
        const val CONFIG = "ir.ayantech.ocr_sdk.extra.CONFIG"
        const val RESULT = "ir.ayantech.ocr_sdk.extra.RESULT"
    }
    /**
     * Converts an image to Base64 string.
     *
     * @param maxSizeInMb Recommended range is between 3 and 5 MB. Values outside this range may impact quality or performance.
     */
    open val TAG = "OcrHelperLogs"

    suspend fun encodeImageToBase64(
        context: Context,
        imageUri: Uri?,
        maxSizeInMb: Int?  // حداکثر حجم base64 به مگابایت
    ): String? = withContext(Dispatchers.IO) {
        try {
             if (imageUri == null) return@withContext ""

            val originalBitmap =
                MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)

            val maxSizeInBytes = (maxSizeInMb ?: 4) * 1024 * 1024

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

                if ((base64SizeInBytes / maxSizeInBytes) > 10) {
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
                TAG,
                "${encodedString.toByteArray(Charsets.UTF_8).size / 1024} KB at quality $quality scale $scale"
            )

            if (currentBitmap != originalBitmap) {
                currentBitmap.recycle()
            }

            return@withContext encodedString
        } catch (e: Exception) {
            Log.d(TAG, "encodeImageToBase64 errorMessage: ${e.message} cause:${e.cause}")
            return@withContext null
        }

    }

    fun deleteCachedFileFromUri(context: Context, uri: Uri): Boolean {

        // 1) چک سریع وجود داشتن
        val exists = try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (_: FileNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
        if (!exists) {
            Log.d(TAG, "Not found (no such file): $uri")
            return false
        }

        // 2) تلاش برای حذف
        return try {
            val rows = context.contentResolver.delete(uri, null, null)
            if (rows > 0) {
                Log.d(TAG, "Deleted: $uri")
                true
            } else {
                Log.d(TAG, "Delete returned rows=0 (not deleted): $uri")
                false
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to delete: ${e.message}")
            false
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid URI or wrong authority: ${e.message}")
            false
        } catch (e: UnsupportedOperationException) {
            Log.w(TAG, "Provider does not support delete(): ${e.message}")
            false
        }
    }
}