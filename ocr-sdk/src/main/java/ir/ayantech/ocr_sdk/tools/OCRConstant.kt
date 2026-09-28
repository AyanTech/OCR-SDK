package ir.ayantech.ocr_sdk.tools

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit

@SuppressLint("StaticFieldLeak")
object OCRConstant {
    lateinit var context: Context
    private val prefs by lazy {
        context.getSharedPreferences(
            "ocr_sdk_prefs",
            Context.MODE_PRIVATE
        )
    }

    var application_ID: String
        get() = prefs.getString("Application_ID", "") ?: ""
        set(value) = prefs.edit { putString("Application_ID", value) }

    var token: String
        get() = prefs.getString("Token", "") ?: ""
        set(value) = prefs.edit { putString("Token", value) }

    var baseURL: String
        get() = prefs.getString("Base_URL", "") ?: ""
        set(value) = prefs.edit { putString("Base_URL", value) }
}
