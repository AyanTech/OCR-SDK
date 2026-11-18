package ir.ayantech.networking.datasource

import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.model.ApiCallStatus
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.data.UploadCardOcr
import kotlinx.coroutines.flow.Flow

interface OcrRemoteDataSource {
    suspend fun getCardOcrResult(
        requestBody: GetCardOcrResult.GetCardOcrResultRequestBody,
        baseUrl: String? = null
    ): Flow<AyanAPIResult<GetCardOcrResult.GetCardOcrResultResponseModel, ApiCallStatus, Exception>>

    suspend fun uploadCardOcr(
        requestBody: UploadCardOcr.UploadCardOcrRequestBody,
        baseUrl: String? = null
    ): Flow<AyanAPIResult<UploadCardOcr.UploadCardOcrResponseModel, ApiCallStatus, Exception>>
}
