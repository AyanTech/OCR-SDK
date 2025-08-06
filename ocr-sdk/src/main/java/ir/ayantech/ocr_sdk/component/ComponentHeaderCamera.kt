package ir.ayantech.ocr_sdk.component

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import ir.ayantech.ayannetworking.api.SimpleCallback
import ir.ayantech.ocr_sdk.databinding.OcrComponentHeaderCameraBinding

fun OcrComponentHeaderCameraBinding.init(
    title: String,
    onBackButtonClicked: SimpleCallback
) {
    val str = SpannableStringBuilder(title)
    str.setSpan(
        StyleSpan(Typeface.BOLD),
        0,
        str.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    tvTitle.text = str
    backIv.setOnClickListener {
        onBackButtonClicked()
    }
}






