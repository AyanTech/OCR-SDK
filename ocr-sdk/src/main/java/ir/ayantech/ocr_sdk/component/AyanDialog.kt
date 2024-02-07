package ir.ayantech.ocr_sdk.component

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import ir.ayantech.ocr_sdk.R
import ir.ayantech.whygoogle.helper.viewBinding


abstract class AyanDialog<T : ViewBinding>(context: Context) :
    Dialog(context, R.style.AyanDialog) {

    val binding: T by viewBinding(binder)

    abstract val binder: (LayoutInflater) -> T

    open val isCentered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
         setContentView(binding.root)
        window?.setBackgroundDrawable(
            ContextCompat.getDrawable(
                this.context,
                R.drawable.ocr_back_dialog
            )
        )

        if (!isCentered) {
            window?.setGravity(Gravity.CENTER)
            val wl: WindowManager.LayoutParams = window?.attributes!!
            wl.y = 32
            this.onWindowAttributesChanged(wl)
        }
    }
}