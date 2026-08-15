package ir.ayantech.ocr_sdk.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import ir.ayantech.networking.repository.OcrRepository
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.domain.usecase.impl.GetCardOcrResultUseCaseImpl
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetCardOcrResultUseCaseImplTest {

    private lateinit var ocrRepository: OcrRepository
    private lateinit var getCardOcrResultUseCase: GetCardOcrResultUseCase

    @Before
    fun setup() {
        ocrRepository = mockk()
        getCardOcrResultUseCase = GetCardOcrResultUseCaseImpl(ocrRepository)
    }

    @Test
    fun `invoke should call getCardOcrResult on repository and return success`() = runTest {
        // Arrange
        val requestBody = GetCardOcrResult.GetCardOcrResultRequestBody(fileId = "file123")
        val expectedResponse = GetCardOcrResult.GetCardOcrResultResponseModel(
            result = emptyList(),
            cardId = "card123",
            status = "Successful",
            nextCallInterval = 0,
            retryable = false
        )
        val expectedResult = AyanAPIResult.success(expectedResponse)
        coEvery { ocrRepository.getCardOcrResult(requestBody) } returns flowOf(expectedResult)

        // Act
        val resultFlow = getCardOcrResultUseCase(requestBody)

        // Assert
        resultFlow.collect { result ->
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun `invoke should call getCardOcrResult on repository and return failure`() = runTest {
        // Arrange
        val requestBody = GetCardOcrResult.GetCardOcrResultRequestBody(fileId = "invalid")
        val expectedResult = AyanAPIResult.error<GetCardOcrResult.GetCardOcrResultResponseModel>(Exception("Not found"))
        coEvery { ocrRepository.getCardOcrResult(requestBody) } returns flowOf(expectedResult)

        // Act
        val resultFlow = getCardOcrResultUseCase(requestBody)

        // Assert
        resultFlow.collect { result ->
            assertEquals(expectedResult, result)
        }
    }
}
