package ir.ayantech.networking.repository.impl

import ir.ayantech.networking.datasource.OcrRemoteDataSource
import ir.ayantech.networking.repository.OcrRepository
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.data.UploadCardOcr

class OcrRepositoryImpl(
    private val remoteDataSource: OcrRemoteDataSource,
) : OcrRepository {
    override suspend fun getCardOcrResult(
        requestBody: GetCardOcrResult.GetCardOcrResultRequestBody,
        baseUrl: String?
    ): Result<GetCardOcrResult.GetCardOcrResultResponseModel> = remoteDataSource.getCardOcrResult(requestBody = requestBody, baseUrl = baseUrl)

    override suspend fun uploadCardOcr(
        requestBody: UploadCardOcr.UploadCardOcrRequestBody,
        baseUrl: String?
    ): Result<UploadCardOcr.UploadCardOcrResponseModel> = remoteDataSource.uploadCardOcr(requestBody = requestBody, baseUrl = baseUrl)
}
