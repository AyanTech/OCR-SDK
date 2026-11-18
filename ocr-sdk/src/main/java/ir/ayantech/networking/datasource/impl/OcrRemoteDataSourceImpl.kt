package ir.ayantech.networking.datasource.impl

import ir.ayantech.networking.datasource.OcrRemoteDataSource
import ir.ayantech.ocr_sdk.tools.OCRConstant
import ir.ayantech.networking.v2.AyanApi
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.model.ApiCallStatus
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.data.UploadCardOcr
import kotlinx.coroutines.flow.Flow

class OcrRemoteDataSourceImpl(
    private val ayanApi: AyanApi,
) : OcrRemoteDataSource {
    override suspend fun getCardOcrResult(
        requestBody: GetCardOcrResult.GetCardOcrResultRequestBody,
        baseUrl: String?
    ): Flow<AyanAPIResult<GetCardOcrResult.GetCardOcrResultResponseModel, ApiCallStatus, Exception>> = ayanApi.post<GetCardOcrResult.GetCardOcrResultRequestBody, GetCardOcrResult.GetCardOcrResultResponseModel>(
        body = requestBody,
        endPoint = OCRConstant.getResultPath,
        baseUrl = baseUrl
    )

    override suspend fun uploadCardOcr(
        requestBody: UploadCardOcr.UploadCardOcrRequestBody,
        baseUrl: String?
    ): Flow<AyanAPIResult<UploadCardOcr.UploadCardOcrResponseModel, ApiCallStatus, Exception>> = ayanApi.post<UploadCardOcr.UploadCardOcrRequestBody, UploadCardOcr.UploadCardOcrResponseModel>(
        body = requestBody,
        endPoint = OCRConstant.uploadPath,
        baseUrl = baseUrl
    )
}
