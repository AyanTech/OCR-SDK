package ir.ayantech.ocr_sdk.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import ir.ayantech.networking.repository.OcrRepository
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.model.ApiCallStatus
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
            result = listOf(GetCardOcrResult.OcrResult(key = "Name", value = "John Doe")),
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

    @Test
    fun `invoke should retry when status is Pending and only emit the final result`() = runTest {
        // Arrange
        val requestBody = GetCardOcrResult.GetCardOcrResultRequestBody(fileId = "file123")

        val pendingResponse = GetCardOcrResult.GetCardOcrResultResponseModel(
            result = null,
            cardId = "card123",
            status = "Pending",
            nextCallInterval = 10,
            retryable = false
        )
        val successResponse = GetCardOcrResult.GetCardOcrResultResponseModel(
            result = listOf(GetCardOcrResult.OcrResult(key = "Name", value = "John Doe")),
            cardId = "card123",
            status = "Successful",
            nextCallInterval = 0,
            retryable = false
        )

        val pendingResult = AyanAPIResult.success(pendingResponse)
        val successResult = AyanAPIResult.success(successResponse)

        coEvery { ocrRepository.getCardOcrResult(requestBody) } returnsMany listOf(
            flowOf(pendingResult),
            flowOf(successResult)
        )

        // Act
        val results = mutableListOf<AyanAPIResult<GetCardOcrResult.GetCardOcrResultResponseModel, ApiCallStatus, Exception>>()
        getCardOcrResultUseCase(requestBody).collect { results.add(it) }

        // Assert: pending result is never emitted to the UI layer, only the final one
        assertEquals(1, results.size)
        assertEquals(successResult, results[0])
        coVerify(exactly = 2) { ocrRepository.getCardOcrResult(requestBody) }
    }

    @Test
    fun `invoke should be case-insensitive for pending status`() = runTest {
        // Arrange
        val requestBody = GetCardOcrResult.GetCardOcrResultRequestBody(fileId = "file123")

        val pendingResponse = GetCardOcrResult.GetCardOcrResultResponseModel(
            result = null,
            cardId = "card123",
            status = "pending", // Lowercase
            nextCallInterval = 10,
            retryable = false
        )
        val successResponse = GetCardOcrResult.GetCardOcrResultResponseModel(
            result = listOf(GetCardOcrResult.OcrResult(key = "Name", value = "John Doe")),
            cardId = "card123",
            status = "Successful",
            nextCallInterval = 0,
            retryable = false
        )

        val pendingResult = AyanAPIResult.success(pendingResponse)
        val successResult = AyanAPIResult.success(successResponse)

        coEvery { ocrRepository.getCardOcrResult(requestBody) } returnsMany listOf(
            flowOf(pendingResult),
            flowOf(successResult)
        )

        // Act
        val results = mutableListOf<AyanAPIResult<GetCardOcrResult.GetCardOcrResultResponseModel, ApiCallStatus, Exception>>()
        getCardOcrResultUseCase(requestBody).collect { results.add(it) }

        // Assert
        assertEquals(1, results.size)
        assertEquals(successResult, results[0])
        coVerify(exactly = 2) { ocrRepository.getCardOcrResult(requestBody) }
    }

    @Test
    fun `invoke should emit error when status is Successful but result is empty`() = runTest {
        // Arrange
        val requestBody = GetCardOcrResult.GetCardOcrResultRequestBody(fileId = "file123")
        val emptySuccessResponse = GetCardOcrResult.GetCardOcrResultResponseModel(
            result = emptyList(),
            cardId = "card123",
            status = "Successful",
            nextCallInterval = 0,
            retryable = false
        )
        coEvery { ocrRepository.getCardOcrResult(requestBody) } returns flowOf(
            AyanAPIResult.success(emptySuccessResponse)
        )

        // Act
        val results = mutableListOf<AyanAPIResult<GetCardOcrResult.GetCardOcrResultResponseModel, ApiCallStatus, Exception>>()
        getCardOcrResultUseCase(requestBody).collect { results.add(it) }

        // Assert
        assertEquals(1, results.size)
        assertEquals(true, results[0].isError)
    }

    @Test
    fun `invoke should retry 5 times when status is Pending and then return success`() = runTest {
        // Arrange
        val requestBody = GetCardOcrResult.GetCardOcrResultRequestBody(fileId = "file123")

        val pendingResponse = GetCardOcrResult.GetCardOcrResultResponseModel(
            result = null,
            cardId = "card123",
            status = "Pending",
            nextCallInterval = 100,
            retryable = false
        )
        val successResponse = GetCardOcrResult.GetCardOcrResultResponseModel(
            result = listOf(GetCardOcrResult.OcrResult(key = "Name", value = "John Doe")),
            cardId = "card123",
            status = "Successful",
            nextCallInterval = 0,
            retryable = false
        )

        val pendingResult = AyanAPIResult.success(pendingResponse)
        val successResult = AyanAPIResult.success(successResponse)

        coEvery { ocrRepository.getCardOcrResult(requestBody) } returnsMany listOf(
            flowOf(pendingResult),
            flowOf(pendingResult),
            flowOf(pendingResult),
            flowOf(pendingResult),
            flowOf(pendingResult),
            flowOf(successResult)
        )

        // Act
        val results = mutableListOf<AyanAPIResult<GetCardOcrResult.GetCardOcrResultResponseModel, ApiCallStatus, Exception>>()
        getCardOcrResultUseCase(requestBody).collect { results.add(it) }

        // Assert
        assertEquals(1, results.size)
        assertEquals(successResult, results[0])
        coVerify(exactly = 6) { ocrRepository.getCardOcrResult(requestBody) }
    }
}
