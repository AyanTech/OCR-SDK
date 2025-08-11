package ir.ayantech.ocr_sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OcrDataResult(
    val cardType: String?,
    val items: ArrayList<GetCardOcrResult.Result>,
    val extraInfo: String? = null
) : Parcelable