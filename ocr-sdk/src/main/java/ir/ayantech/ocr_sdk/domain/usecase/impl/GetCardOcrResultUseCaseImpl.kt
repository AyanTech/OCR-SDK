package ir.ayantech.ocr_sdk.domain.usecase.impl

import ir.ayantech.networking.repository.OcrRepository
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.model.ApiCallStatus
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.data.model.OcrSdkHookApiCallStatusEnum
import ir.ayantech.ocr_sdk.domain.usecase.GetCardOcrResultUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

class GetCardOcrResultUseCaseImpl(private val ocrRepository: OcrRepository) :
    GetCardOcrResultUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend operator fun invoke(
        requestBody: GetCardOcrResult.GetCardOcrResultRequestBody
    ): Flow<AyanAPIResult<GetCardOcrResult.GetCardOcrResultResponseModel, ApiCallStatus, Exception>> {
        return flow {
            ocrRepository.getCardOcrResult(requestBody).collect { result ->
                when (result) {
                    is AyanAPIResult.Success if result.value.status == OcrSdkHookApiCallStatusEnum.Pending.name -> {
                        val duration = maxOf(result.value.nextCallInterval, 500L).milliseconds
                        delay(duration)
                        emitAll(invoke(requestBody))
                    }

                    is AyanAPIResult.Success if result.value.status == OcrSdkHookApiCallStatusEnum.Successful.name && result.value.result.isNullOrEmpty() -> {
                        emit(AyanAPIResult.error(Exception("OCR result is empty")))
                    }

                    is AyanAPIResult.Success if result.value.status == OcrSdkHookApiCallStatusEnum.Failed.name -> {
                        emit(AyanAPIResult.error(Exception(result.value.description)))
                    }

                    else -> emit(result)
                }
            }
        }

    }
}
