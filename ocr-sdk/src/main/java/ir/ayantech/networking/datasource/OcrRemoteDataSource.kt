package ir.ayantech.networking.datasource

import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.data.UploadCardOcr

interface OcrRemoteDataSource {
    suspend fun getCardOcrResult(
        requestBody: GetCardOcrResult.GetCardOcrResultRequestBody,
        baseUrl: String? = null
    ): Result<GetCardOcrResult.GetCardOcrResultResponseModel>

    suspend fun uploadCardOcr(
        requestBody: UploadCardOcr.UploadCardOcrRequestBody,
        baseUrl: String? = null
    ): Result<UploadCardOcr.UploadCardOcrResponseModel>
}
