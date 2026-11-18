package ir.ayantech.networking.repository.impl

import ir.ayantech.networking.datasource.OcrRemoteDataSource
import ir.ayantech.networking.repository.OcrRepository
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.model.ApiCallStatus
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.data.UploadCardOcr
import kotlinx.coroutines.flow.Flow

class OcrRepositoryImpl(
    private val remoteDataSource: OcrRemoteDataSource,
) : OcrRepository {
    override suspend fun getCardOcrResult(
        requestBody: GetCardOcrResult.GetCardOcrResultRequestBody,
        baseUrl: String?
    ): Flow<AyanAPIResult<GetCardOcrResult.GetCardOcrResultResponseModel, ApiCallStatus, Exception>> = remoteDataSource.getCardOcrResult(requestBody = requestBody, baseUrl = baseUrl)

    override suspend fun uploadCardOcr(
        requestBody: UploadCardOcr.UploadCardOcrRequestBody,
        baseUrl: String?
    ): Flow<AyanAPIResult<UploadCardOcr.UploadCardOcrResponseModel, ApiCallStatus, Exception>> = remoteDataSource.uploadCardOcr(requestBody = requestBody, baseUrl = baseUrl)
}
