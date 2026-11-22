package ir.ayantech.ocr_sdk.domain.usecase.impl

import ir.ayantech.networking.repository.OcrRepository
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.data.model.OcrSdkHookApiCallStatusEnum
import ir.ayantech.ocr_sdk.domain.usecase.GetCardOcrResultUseCase
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class GetCardOcrResultUseCaseImpl(private val ocrRepository: OcrRepository) :
    GetCardOcrResultUseCase {
    override suspend operator fun invoke(
        requestBody: GetCardOcrResult.GetCardOcrResultRequestBody
    ): Result<GetCardOcrResult.GetCardOcrResultResponseModel> {
        while (true) {
            val result = ocrRepository.getCardOcrResult(requestBody)
            val response = result.getOrElse { return result }
            when {
                response.status.equals(OcrSdkHookApiCallStatusEnum.Pending.name, ignoreCase = true) ->
                    delay(response.nextCallInterval.coerceAtLeast(500L).milliseconds)

                response.status.equals(OcrSdkHookApiCallStatusEnum.Failed.name, ignoreCase = true) ->
                    return Result.failure(OcrResultFailedException())

                response.status.equals(OcrSdkHookApiCallStatusEnum.Successful.name, ignoreCase = true) &&
                    response.result.isNullOrEmpty() -> return Result.failure(EmptyOcrResultException())

                else -> return result
            }
        }
    }
}

class EmptyOcrResultException : Exception()

class OcrResultFailedException : Exception()
