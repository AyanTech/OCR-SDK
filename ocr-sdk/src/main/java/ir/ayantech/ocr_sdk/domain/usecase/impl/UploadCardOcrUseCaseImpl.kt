package ir.ayantech.ocr_sdk.domain.usecase.impl

import ir.ayantech.networking.repository.OcrRepository
import ir.ayantech.ocr_sdk.data.UploadCardOcr
import ir.ayantech.ocr_sdk.domain.usecase.UploadCardOcrUseCase

class UploadCardOcrUseCaseImpl(private val ocrRepository: OcrRepository) : UploadCardOcrUseCase {
    override suspend operator fun invoke(
        requestBody: UploadCardOcr.UploadCardOcrRequestBody
    ): Result<UploadCardOcr.UploadCardOcrResponseModel> {
        return ocrRepository.uploadCardOcr(requestBody)
    }
}
