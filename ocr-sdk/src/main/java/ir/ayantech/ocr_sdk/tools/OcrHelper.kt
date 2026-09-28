package ir.ayantech.ocr_sdk.tools

import android.content.Context
import android.net.Uri
import android.util.Log
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

    val TAG = "OcrHelperLogs"

    fun deleteCachedFileFromUri(context: Context, uri: Uri): Boolean {

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