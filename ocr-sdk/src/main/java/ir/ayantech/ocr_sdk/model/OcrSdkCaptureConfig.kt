package ir.ayantech.ocr_sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OcrSdkCaptureConfig(
    val className: String? = null,
    val extraInfo: String? = null,
    val textBlock: OcrSdkTextBlock? = null
) : Parcelable