package ir.ayantech.ocr_sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


class OcrSdkUploadNewCardOcrImage {

    @Parcelize
    data class Input(
        val ImageArray: List<String?>,
        val Type : String
    ): Parcelable

    @Parcelize
    data class Output(
        val FileID: String,
    ): Parcelable
}