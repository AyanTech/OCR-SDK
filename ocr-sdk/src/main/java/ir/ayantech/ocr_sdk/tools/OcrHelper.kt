package ir.ayantech.ocr_sdk.tools

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

object OcrHelper {

    object Actions {
        const val CAPTURE_URI = "ir.ayantech.ocr_sdk.action.CAPTURE_URI"
        const val OCR_RETURN_DATA = "ir.ayantech.ocr_sdk.action.CAPTURE_AND_UPLOAD"
    }

    object Extras {
        const val CONFIG = "ir.ayantech.ocr_sdk.extra.CONFIG"
        const val RESULT = "ir.ayantech.ocr_sdk.extra.RESULT"
        const val TEXTS = "ir.ayantech.ocr_sdk.extra.TEXTS"
    }

    open val TAG = "OcrHelperLogs"

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

    @Suppress("DEPRECATION")
    suspend fun encodeImageToBase64(
        context: Context,
        imageUri: Uri?,
        maxBase64Mb: Double = 4.0,              // سقف خود Base64 (MB، اعشاری)
        minBase64Mb: Double? = null,            // حداقل Base64 (MB، اختیاری؛ برای upscale)
        maxOriginalFileMb: Double? = null,      // پیش‌غربال حجم فایل ورودی (اختیاری)
        maxOriginalMegaPixels: Double? = null,  // پیش‌غربال رزولوشن خیلی بزرگ (اختیاری)
        listener: EncodeImageListener
    ): Unit = withContext(Dispatchers.IO) {
        ImageBase64Engine.encode(
            context = context,
            imageUri = imageUri,
            maxBase64Mb = maxBase64Mb,
            minBase64Mb = minBase64Mb,
            maxOriginalFileMb = maxOriginalFileMb,
            maxOriginalMegaPixels = maxOriginalMegaPixels,
            listener = listener
        )

    }
}