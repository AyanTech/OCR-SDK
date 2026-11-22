package ir.ayantech.ocr_sdk.ui.viewmodel

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import ir.ayantech.ocr_sdk.MainDispatcherRule
import ir.ayantech.ocr_sdk.R
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.data.UploadCardOcr
import ir.ayantech.ocr_sdk.domain.usecase.GetCardOcrResultUseCase
import ir.ayantech.ocr_sdk.domain.usecase.UploadCardOcrUseCase
import ir.ayantech.ocr_sdk.domain.usecase.impl.EmptyOcrResultException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class OcrViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var uploadCardOcrUseCase: UploadCardOcrUseCase
    private lateinit var getCardOcrResultUseCase: GetCardOcrResultUseCase
    private lateinit var viewModel: OcrViewModel

    @Before
    fun setup() {
        uploadCardOcrUseCase = mockk()
        getCardOcrResultUseCase = mockk()
        viewModel = OcrViewModel(
            uploadCardOcrUseCase, getCardOcrResultUseCase, mainDispatcherRule.testDispatcher
        )
    }

    @Test
    fun `uploadCardOcr should update uiState to UploadSuccess on success`() = runTest {
        // Arrange
        val images = listOf("image1")
        val cardType = "bank_card"
        val response = UploadCardOcr.UploadCardOcrResponseModel(fileId = "file123")
        coEvery { uploadCardOcrUseCase(any()) } returns Result.success(response)

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(OcrUiState.Idle, awaitItem())

            viewModel.uploadCardOcr(images, cardType)

            val finalState = expectMostRecentItem()
            assertTrue(finalState is OcrUiState.UploadSuccess)
            assertEquals("file123", (finalState as OcrUiState.UploadSuccess).fileId)
        }
    }

    @Test
    fun `uploadCardOcr should update uiState to Error on failure`() = runTest {
        // Arrange
        val images = listOf("image1")
        val cardType = "bank_card"
        val errorMessage = "Network Error"
        val failureResult =
            Result.failure<UploadCardOcr.UploadCardOcrResponseModel>(Exception(errorMessage))
        coEvery { uploadCardOcrUseCase(any()) } returns failureResult

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(OcrUiState.Idle, awaitItem())

            viewModel.uploadCardOcr(images, cardType)

            val finalState = expectMostRecentItem()
            assertTrue(finalState is OcrUiState.Error)
            assertEquals(R.string.ocr_upload_failed, (finalState as OcrUiState.Error).messageRes)
        }
    }

    @Test
    fun `getCardOcrResult should update uiState to ResultSuccess on success`() = runTest {
        // Arrange
        val fileId = "file123"
        val response = GetCardOcrResult.GetCardOcrResultResponseModel(
            result = emptyList(),
            cardId = "card123",
            status = "Successful",
            nextCallInterval = 0,
            retryable = false
        )
        coEvery { getCardOcrResultUseCase(any()) } returns Result.success(response)

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(OcrUiState.Idle, awaitItem())

            viewModel.getCardOcrResult(fileId)

            val finalState = expectMostRecentItem()
            assertTrue(finalState is OcrUiState.ResultSuccess)
            assertEquals(response, (finalState as OcrUiState.ResultSuccess).response)
        }
    }

    @Test
    fun `getCardOcrResult should update uiState to Error on failure`() = runTest {
        // Arrange
        val fileId = "file123"
        val errorMessage = "Result not ready"
        val failureResult = Result.failure<GetCardOcrResult.GetCardOcrResultResponseModel>(
            Exception(errorMessage)
        )
        coEvery { getCardOcrResultUseCase(any()) } returns failureResult

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(OcrUiState.Idle, awaitItem())

            viewModel.getCardOcrResult(fileId)

            val finalState = expectMostRecentItem()
            assertTrue(finalState is OcrUiState.Error)
            assertEquals(R.string.ocr_result_failed, (finalState as OcrUiState.Error).messageRes)
        }
    }

    @Test
    fun `empty OCR result uses the localized empty-result message`() = runTest {
        val failure = Result.failure<GetCardOcrResult.GetCardOcrResultResponseModel>(
            EmptyOcrResultException()
        )
        coEvery { getCardOcrResultUseCase(any()) } returns failure

        viewModel.getCardOcrResult("file123")
        runCurrent()

        assertEquals(OcrUiState.Error(R.string.ocr_result_empty), viewModel.uiState.value)
    }

    @Test
    fun `upload shows loading before work and ignores a second tap`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val model = OcrViewModel(uploadCardOcrUseCase, getCardOcrResultUseCase, dispatcher)
        val response = UploadCardOcr.UploadCardOcrResponseModel(fileId = "file123")
        var calls = 0
        coEvery { uploadCardOcrUseCase(any()) } coAnswers {
            calls++
            gate.await()
            Result.success(response)
        }

        model.uploadCardOcr(listOf("image1"), "bank_card")
        assertEquals(OcrUiState.Loading, model.uiState.value)
        model.uploadCardOcr(listOf("image1"), "bank_card")
        runCurrent()
        assertEquals(1, calls)
        assertEquals(OcrUiState.Loading, model.uiState.value)

        gate.complete(Unit)
        runCurrent()
        assertEquals(OcrUiState.UploadSuccess("file123"), model.uiState.value)
    }

    @Test
    fun `reset cancels an in-flight upload without replacing idle state`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val model = OcrViewModel(uploadCardOcrUseCase, getCardOcrResultUseCase, dispatcher)
        coEvery { uploadCardOcrUseCase(any()) } coAnswers {
            gate.await()
            Result.success(UploadCardOcr.UploadCardOcrResponseModel(fileId = "file123"))
        }

        model.uploadCardOcr(listOf("image1"), "bank_card")
        runCurrent()
        model.resetState()
        gate.complete(Unit)
        runCurrent()

        assertEquals(OcrUiState.Idle, model.uiState.value)
    }

    @Test
    fun `result lookup shows loading before work and ignores repeat requests`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val model = OcrViewModel(uploadCardOcrUseCase, getCardOcrResultUseCase, dispatcher)
        val response = GetCardOcrResult.GetCardOcrResultResponseModel(
            result = emptyList(), cardId = "card123", status = "Successful"
        )
        var calls = 0
        coEvery { getCardOcrResultUseCase(any()) } coAnswers {
            calls++
            gate.await()
            Result.success(response)
        }

        model.getCardOcrResult("file123")
        assertEquals(OcrUiState.Loading, model.uiState.value)
        model.getCardOcrResult("file123")
        runCurrent()
        assertEquals(1, calls)

        gate.complete(Unit)
        runCurrent()
        assertEquals(OcrUiState.ResultSuccess(response), model.uiState.value)
    }

    @Test
    fun `unexpected upload error exits loading`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val model = OcrViewModel(uploadCardOcrUseCase, getCardOcrResultUseCase, dispatcher)
        coEvery { uploadCardOcrUseCase(any()) } throws IllegalStateException("Upload failed")

        model.uploadCardOcr(listOf("image1"), "bank_card")
        runCurrent()

        assertEquals(OcrUiState.Error(R.string.ocr_upload_failed), model.uiState.value)
    }

    @Test
    fun `resetState should set uiState to Idle`() = runTest {
        // Arrange

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(OcrUiState.Idle, awaitItem())
            viewModel.resetState()
        }
    }
}
