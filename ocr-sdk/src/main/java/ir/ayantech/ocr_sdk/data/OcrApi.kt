package ir.ayantech.ocr_sdk.data

import com.alirezabdn.generator.AyanAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@AyanAPI(
    endpoint = "UploadCardOCR",
    methodImplName = "uploadCardOcr",
    separationCategory = "Ocr"
)
class UploadCardOcr {
    @Serializable
    data class UploadCardOcrRequestBody(
        @SerialName("ImageArray") val imageArray: List<String?>,
        @SerialName("Type") val type: String
    )

    @Serializable
    data class UploadCardOcrResponseModel(
        @SerialName("FileID") val fileId: String
    )
}

@AyanAPI(
    endpoint = "GetCardOcrResult",
    methodImplName = "getCardOcrResult",
    separationCategory = "Ocr"
)
class GetCardOcrResult {
    @Serializable
    data class GetCardOcrResultRequestBody(
        @SerialName("FileID") val fileId: String
    )

    @Serializable
    data class GetCardOcrResultResponseModel(
        @SerialName("Result") val result: List<OcrResult>?,
        @SerialName("CardID") val cardId: String,
        @SerialName("Status") val status: String,
        @SerialName("NextCallInterval") val nextCallInterval: Long,
        @SerialName("Retryable") val retryable: Boolean
    )

    @Serializable
    data class OcrResult(
        @SerialName("Key") val key: String?,
        @SerialName("Value") val value: String?
    )
}
