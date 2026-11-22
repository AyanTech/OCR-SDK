package ir.ayantech.networking.datasource.impl

import ir.ayantech.networking.datasource.OcrRemoteDataSource
import ir.ayantech.ocr_sdk.tools.OCRConstant
import ir.ayantech.networking.v2.AyanApi
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.data.UploadCardOcr

class OcrRemoteDataSourceImpl(
    private val ayanApi: AyanApi,
) : OcrRemoteDataSource {
    override suspend fun getCardOcrResult(
        requestBody: GetCardOcrResult.GetCardOcrResultRequestBody,
        baseUrl: String?
    ): Result<GetCardOcrResult.GetCardOcrResultResponseModel> = ayanApi.post<GetCardOcrResult.GetCardOcrResultRequestBody, GetCardOcrResult.GetCardOcrResultResponseModel>(
        body = requestBody,
        endPoint = OCRConstant.getResultPath,
        baseUrl = baseUrl
    )

    override suspend fun uploadCardOcr(
        requestBody: UploadCardOcr.UploadCardOcrRequestBody,
        baseUrl: String?
    ): Result<UploadCardOcr.UploadCardOcrResponseModel> = ayanApi.post<UploadCardOcr.UploadCardOcrRequestBody, UploadCardOcr.UploadCardOcrResponseModel>(
        body = requestBody,
        endPoint = OCRConstant.uploadPath,
        baseUrl = baseUrl
    )
}
