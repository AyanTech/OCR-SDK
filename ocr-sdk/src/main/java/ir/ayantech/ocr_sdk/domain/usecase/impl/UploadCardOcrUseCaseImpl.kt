package ir.ayantech.ocr_sdk.domain.usecase.impl

import ir.ayantech.networking.repository.OcrRepository
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.model.ApiCallStatus
import ir.ayantech.ocr_sdk.data.UploadCardOcr
import ir.ayantech.ocr_sdk.domain.usecase.UploadCardOcrUseCase
import kotlinx.coroutines.flow.Flow

class UploadCardOcrUseCaseImpl(private val ocrRepository: OcrRepository) : UploadCardOcrUseCase {
    override suspend operator fun invoke(
        requestBody: UploadCardOcr.UploadCardOcrRequestBody
    ): Flow<AyanAPIResult<UploadCardOcr.UploadCardOcrResponseModel, ApiCallStatus, Exception>> {
        return ocrRepository.uploadCardOcr(requestBody)
    }
}
