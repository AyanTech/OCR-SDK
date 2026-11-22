package ir.ayantech.ocr_sdk.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import ir.ayantech.networking.repository.OcrRepository
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.domain.usecase.impl.GetCardOcrResultUseCaseImpl
import ir.ayantech.ocr_sdk.domain.usecase.impl.OcrResultFailedException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GetCardOcrResultUseCaseImplTest {

    private lateinit var ocrRepository: OcrRepository
    private lateinit var getCardOcrResultUseCase: GetCardOcrResultUseCase

    @Test
    fun pendingUsesMinimumAndServerDelaysThenStopsOnFailedStatus() = runTest {
        val input = GetCardOcrResult.GetCardOcrResultRequestBody("fixture")
        fun response(status: String, interval: Long): Result<GetCardOcrResult.GetCardOcrResultResponseModel> {
            return Result.success(GetCardOcrResult.GetCardOcrResultResponseModel(status = status, nextCallInterval = interval))
        }
        coEvery { ocrRepository.getCardOcrResult(input) } returnsMany listOf(
            response("pending", -1),
            response("Pending", 900),
            response("Failed", 0)
        )

        val work = async { getCardOcrResultUseCase(input) }
        runCurrent()
        advanceTimeBy(499)
        runCurrent()
        coVerify(exactly = 1) { ocrRepository.getCardOcrResult(input) }
        advanceTimeBy(1)
        runCurrent()
        advanceTimeBy(899)
        runCurrent()
        coVerify(exactly = 2) { ocrRepository.getCardOcrResult(input) }
        advanceTimeBy(1)
        runCurrent()
        assertEquals(OcrResultFailedException::class.java, work.await().exceptionOrNull()?.javaClass)
        coVerify(exactly = 3) { ocrRepository.getCardOcrResult(input) }
    }

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
        val expectedResult = Result.success(expectedResponse)
        coEvery { ocrRepository.getCardOcrResult(requestBody) } returns expectedResult

        // Act
        val result = getCardOcrResultUseCase(requestBody)

        // Assert
        assertEquals(expectedResult, result)
    }

    @Test
    fun `invoke should call getCardOcrResult on repository and return failure`() = runTest {
        // Arrange
        val requestBody = GetCardOcrResult.GetCardOcrResultRequestBody(fileId = "invalid")
        val expectedResult =
            Result.failure<GetCardOcrResult.GetCardOcrResultResponseModel>(Exception("Not found"))
        coEvery { ocrRepository.getCardOcrResult(requestBody) } returns expectedResult

        // Act
        val result = getCardOcrResultUseCase(requestBody)

        // Assert
        assertEquals(expectedResult, result)
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

        val pendingResult = Result.success(pendingResponse)
        val successResult = Result.success(successResponse)

        coEvery { ocrRepository.getCardOcrResult(requestBody) } returnsMany listOf(
            pendingResult,
            successResult
        )

        // Act
        val result = getCardOcrResultUseCase(requestBody)

        // Assert: pending result is never emitted to the UI layer, only the final one
        assertEquals(successResult, result)
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

        val pendingResult = Result.success(pendingResponse)
        val successResult = Result.success(successResponse)

        coEvery { ocrRepository.getCardOcrResult(requestBody) } returnsMany listOf(
            pendingResult,
            successResult
        )

        // Act
        val result = getCardOcrResultUseCase(requestBody)

        // Assert
        assertEquals(successResult, result)
        coVerify(exactly = 2) { ocrRepository.getCardOcrResult(requestBody) }
    }

    @Test
    fun `cancelling a pending lookup prevents another request`() = runTest {
        val requestBody = GetCardOcrResult.GetCardOcrResultRequestBody(fileId = "file123")
        val pending = GetCardOcrResult.GetCardOcrResultResponseModel(
            result = null,
            cardId = "card123",
            status = "Pending",
            nextCallInterval = 500,
            retryable = false
        )
        coEvery { ocrRepository.getCardOcrResult(requestBody) } returns Result.success(pending)

        val job = backgroundScope.launch {
            getCardOcrResultUseCase(requestBody)
        }
        runCurrent()
        coVerify(exactly = 1) { ocrRepository.getCardOcrResult(requestBody) }
        job.cancelAndJoin()
        advanceTimeBy(1_000)
        runCurrent()

        coVerify(exactly = 1) { ocrRepository.getCardOcrResult(requestBody) }
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
        coEvery { ocrRepository.getCardOcrResult(requestBody) } returns Result.success(emptySuccessResponse)

        // Act
        val result = getCardOcrResultUseCase(requestBody)

        // Assert
        assertEquals(true, result.isFailure)
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

        val pendingResult = Result.success(pendingResponse)
        val successResult = Result.success(successResponse)

        coEvery { ocrRepository.getCardOcrResult(requestBody) } returnsMany listOf(
            pendingResult,
            pendingResult,
            pendingResult,
            pendingResult,
            pendingResult,
            successResult
        )

        // Act
        val result = getCardOcrResultUseCase(requestBody)

        // Assert
        assertEquals(successResult, result)
        coVerify(exactly = 6) { ocrRepository.getCardOcrResult(requestBody) }
    }
}
