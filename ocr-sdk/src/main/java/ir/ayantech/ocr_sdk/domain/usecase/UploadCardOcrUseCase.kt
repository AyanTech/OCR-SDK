package ir.ayantech.ocr_sdk.domain.usecase

import ir.ayantech.ocr_sdk.data.UploadCardOcr

interface UploadCardOcrUseCase {
    suspend operator fun invoke(
        requestBody: UploadCardOcr.UploadCardOcrRequestBody
    ): Result<UploadCardOcr.UploadCardOcrResponseModel>
}