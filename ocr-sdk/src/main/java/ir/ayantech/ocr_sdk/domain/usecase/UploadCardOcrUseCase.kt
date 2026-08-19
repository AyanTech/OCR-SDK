package ir.ayantech.ocr_sdk.domain.usecase

import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.model.ApiCallStatus
import ir.ayantech.ocr_sdk.data.UploadCardOcr
import kotlinx.coroutines.flow.Flow

interface UploadCardOcrUseCase {
    suspend operator fun invoke(
        requestBody: UploadCardOcr.UploadCardOcrRequestBody
    ): Flow<AyanAPIResult<UploadCardOcr.UploadCardOcrResponseModel, ApiCallStatus, Exception>>
}