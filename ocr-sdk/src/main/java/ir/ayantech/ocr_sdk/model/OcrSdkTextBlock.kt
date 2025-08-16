package ir.ayantech.ocr_sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OcrSdkTextBlock(
    val title: String? = null,
    val firstImageHolderText: String? = null,
    val secondImageHolderText: String? = null,
    val buttonText: String? = null
) : Parcelable
