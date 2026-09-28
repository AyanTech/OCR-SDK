package ir.ayantech.ocr_sdk.domain.usecase.impl

import ir.ayantech.networking.repository.OcrRepository
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.model.ApiCallStatus
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.data.model.OcrSdkHookApiCallStatusEnum
import ir.ayantech.ocr_sdk.domain.usecase.GetCardOcrResultUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetCardOcrResultUseCaseImpl(private val ocrRepository: OcrRepository) :
    GetCardOcrResultUseCase {
    override suspend operator fun invoke(
        requestBody: GetCardOcrResult.GetCardOcrResultRequestBody
    ): Flow<AyanAPIResult<GetCardOcrResult.GetCardOcrResultResponseModel, ApiCallStatus, Exception>> =
        flow {
            while (true) {
                var nextPollDelay: Long? = null
                ocrRepository.getCardOcrResult(requestBody).collect { result ->
                    when (result) {
                        is AyanAPIResult.Success -> {
                            val response = result.value
                            when {
                                response.status.equals(
                                    OcrSdkHookApiCallStatusEnum.Pending.name,
                                    ignoreCase = true
                                ) -> {
                                    nextPollDelay = maxOf(response.nextCallInterval, 500L)
                                }

                                response.status.equals(
                                    OcrSdkHookApiCallStatusEnum.Successful.name,
                                    ignoreCase = true
                                ) &&
                                        response.result.isNullOrEmpty() -> {
                                    emit(AyanAPIResult.error(EmptyOcrResultException()))
                                }

                                response.status.equals(
                                    OcrSdkHookApiCallStatusEnum.Failed.name,
                                    ignoreCase = true
                                ) -> {
                                    emit(AyanAPIResult.error(OcrResultFailedException()))
                                }

                                else -> emit(result)
                            }
                        }

                        else -> emit(result)
                    }
                }
                val delayMillis = nextPollDelay ?: break
                delay(delayMillis)
            }
        }
}

class EmptyOcrResultException : Exception()

class OcrResultFailedException : Exception()
