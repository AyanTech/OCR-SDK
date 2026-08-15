package ir.ayantech.ocr_sdk.tools

import android.app.Dialog
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding

fun Any?.isNull() = this == null
fun Any?.isNotNull() = this != null

fun delayed(delayMillis: Long, action: () -> Unit) {
    Handler(Looper.getMainLooper()).postDelayed(action, delayMillis)
}

fun <T : ViewBinding> Dialog.viewBinding(binder: (LayoutInflater) -> T) = lazy {
    binder(layoutInflater)
}
