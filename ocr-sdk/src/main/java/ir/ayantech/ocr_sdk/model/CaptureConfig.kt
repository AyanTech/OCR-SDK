package ir.ayantech.ocr_sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CaptureConfig(
    val maxSizeMb: Int? = null,
    val className: String? = null,
    val extraInfo: String? = null
) : Parcelable