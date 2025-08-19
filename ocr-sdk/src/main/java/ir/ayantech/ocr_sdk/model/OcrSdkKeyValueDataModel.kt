package ir.ayantech.ocr_sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OcrSdkKeyValueDataModel(
    val key: String?,
    val value: String?
) : Parcelable