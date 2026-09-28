package ir.ayantech.ocr_sdk.domain.usecase

import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.model.ApiCallStatus
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import kotlinx.coroutines.flow.Flow

interface GetCardOcrResultUseCase {
    suspend operator fun invoke(
        requestBody: GetCardOcrResult.GetCardOcrResultRequestBody
    ): Flow<AyanAPIResult<GetCardOcrResult.GetCardOcrResultResponseModel, ApiCallStatus, Exception>>
}

