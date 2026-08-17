package ir.ayantech.ocr_sdk.data

import com.alirezabdn.generator.AyanAPI
import ir.ayantech.ocr_sdk.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@AyanAPI(
    endpoint = BuildConfig.UPLOAD_PATH,
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
    endpoint = BuildConfig.GET_RESULT_PATH,
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
        @SerialName("Result") val result: List<OcrResult>? = null,
        @SerialName("CardID") val cardId: String? = null,
        @SerialName("Status") val status: String,
        @SerialName("Description") val description: String? = null,
        @SerialName("NextCallInterval") val nextCallInterval: Long = 0,
        @SerialName("Retryable") val retryable: Boolean = false
    )

    @Serializable
    data class OcrResult(
        @SerialName("Key") val key: String?,
        @SerialName("Value") val value: String?
    )
}
