package ir.ayantech.ocr_sdk.domain.usecase.impl

import ir.ayantech.networking.repository.OcrRepository
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.model.ApiCallStatus
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.domain.usecase.GetCardOcrResultUseCase
import kotlinx.coroutines.flow.Flow

class GetCardOcrResultUseCaseImpl(private val ocrRepository: OcrRepository) : GetCardOcrResultUseCase {
    override suspend operator fun invoke(
        requestBody: GetCardOcrResult.GetCardOcrResultRequestBody
    ): Flow<AyanAPIResult<GetCardOcrResult.GetCardOcrResultResponseModel, ApiCallStatus, Exception>> {
        return ocrRepository.getCardOcrResult(requestBody)
    }
}
