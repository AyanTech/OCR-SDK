package ir.ayantech.ocr_sdk.tools

import android.content.Intent
import android.os.Build
import android.os.Parcelable


inline fun <reified T : Parcelable> Intent?.parcelable(key: String): T? {
    if (this == null) return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key) as? T
    }
}