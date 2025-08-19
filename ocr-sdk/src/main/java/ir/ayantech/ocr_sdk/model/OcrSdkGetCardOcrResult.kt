package ir.ayantech.ocr_sdk.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize


class OcrSdkGetCardOcrResult {
    data class Input(
        val FileID: String,

    )

    @Parcelize
    data class Output(
         val Result: List<Result>?,
        val CardID: String,
        val Status: String,
        val NextCallInterval: Long,
        val Retryable: Boolean,
    ):Parcelable



    @Parcelize
    data class Result(
        val Key: String?,
        val Value: String?
    ):Parcelable
}