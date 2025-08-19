package ir.ayantech.ocr_sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OcrSdkOcrDataResult(
    val cardType: String?,
    val items: ArrayList<OcrSdkGetCardOcrResult.Result>,
    val extraInfo: String? = null
) : Parcelable
