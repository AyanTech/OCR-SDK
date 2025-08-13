package ir.ayantech.ocr_sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OcrConfig(
    val maxSizeMb: Int? = null,
    val className: String? = null,
     val cardType: String? = null,
    val singlePhoto: Boolean? = null,
    val extraInfo: String? = null,
    val textBlock: TextBlock? = null

) : Parcelable