package ir.ayantech.ocr_sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


class OcrSdkUploadNewCardOcrImageTestApplication {

    @Parcelize
    data class Input(
        val cardType: String,
        val cardFrontImage : String,
        val cardBackImage : String,
        val traceNumber : String,
        val token : String,

    ): Parcelable

    @Parcelize
    data class Output(
        val FileID: String,
    ): Parcelable
}