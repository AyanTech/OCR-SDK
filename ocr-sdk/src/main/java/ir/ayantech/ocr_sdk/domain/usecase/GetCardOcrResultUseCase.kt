package ir.ayantech.ocr_sdk.domain.usecase

import ir.ayantech.ocr_sdk.data.GetCardOcrResult

interface GetCardOcrResultUseCase {
    suspend operator fun invoke(
        requestBody: GetCardOcrResult.GetCardOcrResultRequestBody
    ): Result<GetCardOcrResult.GetCardOcrResultResponseModel>
}

