package ir.ayantech.ocr_sdk.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import ir.ayantech.networking.repository.OcrRepository
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.ocr_sdk.data.UploadCardOcr
import ir.ayantech.ocr_sdk.domain.usecase.impl.UploadCardOcrUseCaseImpl
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UploadCardOcrUseCaseImplTest {

    private lateinit var ocrRepository: OcrRepository
    private lateinit var uploadCardOcrUseCase: UploadCardOcrUseCase

    @Before
    fun setup() {
        ocrRepository = mockk()
        uploadCardOcrUseCase = UploadCardOcrUseCaseImpl(ocrRepository)
    }

    @Test
    fun `invoke should call uploadCardOcr on repository and return success`() = runTest {
        // Arrange
        val requestBody = UploadCardOcr.UploadCardOcrRequestBody(
            imageArray = listOf("base64image"),
            type = "bank_card"
        )
        val expectedResponse = UploadCardOcr.UploadCardOcrResponseModel(fileId = "file123")
        val expectedResult = AyanAPIResult.success(expectedResponse)
        coEvery { ocrRepository.uploadCardOcr(requestBody) } returns flowOf(expectedResult)

        // Act
        val resultFlow = uploadCardOcrUseCase(requestBody)

        // Assert
        resultFlow.collect { result ->
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun `invoke should call uploadCardOcr on repository and return failure`() = runTest {
        // Arrange
        val requestBody = UploadCardOcr.UploadCardOcrRequestBody(
            imageArray = listOf("invalid"),
            type = "bank_card"
        )
        val expectedResult = AyanAPIResult.error<UploadCardOcr.UploadCardOcrResponseModel>(Exception("Upload failed"))
        coEvery { ocrRepository.uploadCardOcr(requestBody) } returns flowOf(expectedResult)

        // Act
        val resultFlow = uploadCardOcrUseCase(requestBody)

        // Assert
        resultFlow.collect { result ->
            assertEquals(expectedResult, result)
        }
    }
}
