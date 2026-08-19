package ir.ayantech.ocr_sdk.ui.viewmodel

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.ocr_sdk.MainDispatcherRule
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.data.UploadCardOcr
import ir.ayantech.ocr_sdk.domain.usecase.GetCardOcrResultUseCase
import ir.ayantech.ocr_sdk.domain.usecase.UploadCardOcrUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
        viewModel = OcrViewModel(uploadCardOcrUseCase, getCardOcrResultUseCase)
    }

    @Test
    fun `uploadCardOcr should update uiState to UploadSuccess on success`() = runTest {
        // Arrange
        val images = listOf("image1")
        val cardType = "bank_card"
        val response = UploadCardOcr.UploadCardOcrResponseModel(fileId = "file123")
        coEvery { uploadCardOcrUseCase(any()) } returns flowOf(AyanAPIResult.success(response))

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
        val failureResult = AyanAPIResult.error<UploadCardOcr.UploadCardOcrResponseModel>(Exception(errorMessage))
        coEvery { uploadCardOcrUseCase(any()) } returns flowOf(failureResult)

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(OcrUiState.Idle, awaitItem())
            
            viewModel.uploadCardOcr(images, cardType)
            
            val finalState = expectMostRecentItem()
            assertTrue(finalState is OcrUiState.Error)
            assertEquals(errorMessage, (finalState as OcrUiState.Error).message)
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
        coEvery { getCardOcrResultUseCase(any()) } returns flowOf(AyanAPIResult.success(response))

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
        val failureResult = AyanAPIResult.error<GetCardOcrResult.GetCardOcrResultResponseModel>(Exception(errorMessage))
        coEvery { getCardOcrResultUseCase(any()) } returns flowOf(failureResult)

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(OcrUiState.Idle, awaitItem())
            
            viewModel.getCardOcrResult(fileId)
            
            val finalState = expectMostRecentItem()
            assertTrue(finalState is OcrUiState.Error)
            assertEquals(errorMessage, (finalState as OcrUiState.Error).message)
        }
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
