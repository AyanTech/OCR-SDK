package ir.ayantech.ocr_sdk.data.model

import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OcrSdkOcrDataResult(
    @SerialName("cardType") val cardType: String?,
    @SerialName("items") val items: List<GetCardOcrResult.OcrResult>,
    @SerialName("extraInfo") val extraInfo: String? = null
)
