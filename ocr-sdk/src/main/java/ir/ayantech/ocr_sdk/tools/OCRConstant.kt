package ir.ayantech.ocr_sdk.tools

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak")
object OCRConstant {
    lateinit var context: Context
    private val prefs by lazy { context.getSharedPreferences("ocr_sdk_prefs", Context.MODE_PRIVATE) }

    const val REQUEST_CODE_OCR_RESULT = 1

    var Application_ID: String
        get() = prefs.getString("Application_ID", "") ?: ""
        set(value) = prefs.edit().putString("Application_ID", value).apply()

    var EndPoint_UploadCardOCR: String
        get() = prefs.getString("EndPoint_UploadCardOCR", "") ?: ""
        set(value) = prefs.edit().putString("EndPoint_UploadCardOCR", value).apply()

    var EndPoint_GetCardOcrResult: String
        get() = prefs.getString("EndPoint_GetCardOcrResult", "") ?: ""
        set(value) = prefs.edit().putString("EndPoint_GetCardOcrResult", value).apply()

    var Token: String
        get() = prefs.getString("Token", "") ?: ""
        set(value) = prefs.edit().putString("Token", value).apply()

    var Base_URL: String
        get() = prefs.getString("Base_URL", "") ?: ""
        set(value) = prefs.edit().putString("Base_URL", value).apply()
}
