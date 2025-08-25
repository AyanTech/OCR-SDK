package ir.ayantech.ocr_sdk.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OcrSdkOcrConfig(
    val maxBase64Mb: Double? = null,
    val minBase64Mb: Double? = null,
    val className: String? = null,
    val cardType: String? = null,
    val singlePhoto: Boolean? = null,
    val extraInfo: String? = null,
    val textBlock: OcrSdkTextBlock? = null // Must also be Parcelable
) : Parcelable
