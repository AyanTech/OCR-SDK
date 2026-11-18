package ir.ayantech.ocr_sdk.tools

import android.content.Context
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.Runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ConfigBuilderTest {
    private val context = mockk<Context>()

    @Before
    fun setup() {
        mockkObject(OCRConstant, OcrSdk)
        every { OCRConstant.context = any() } just Runs
        every { OCRConstant.token = any() } just Runs
        every { OCRConstant.baseURL = any() } just Runs
        every { OCRConstant.uploadPath = any() } just Runs
        every { OCRConstant.getResultPath = any() } just Runs
        every { OcrSdk.init(any()) } just Runs
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `custom paths are applied at build and can be set before context`() {
        val builder = OCRConfig.builder()
            .setUploadPath(" custom/upload ")
            .setGetResultPath(" custom/result ")
        verify(exactly = 0) { OCRConstant.uploadPath = any() }
        verify(exactly = 0) { OCRConstant.getResultPath = any() }

        builder.setContext(context).setToken("token").setBaseUrl("https://example.com/").build()

        assertEquals("custom/upload", builder.uploadPath)
        assertEquals("custom/result", builder.getResultPath)
        verify { OCRConstant.uploadPath = "custom/upload" }
        verify { OCRConstant.getResultPath = "custom/result" }
    }

    @Test
    fun `missing upload path fails before saving either path or initializing SDK`() {
        val builder = OCRConfig.builder().setContext(context).setToken("token")
            .setBaseUrl("https://example.com/").setGetResultPath("custom/result")

        val error = assertThrows(IllegalArgumentException::class.java) { builder.build() }

        assertEquals("A required value for setting configuration wasn't provided: uploadPath", error.message)
        verify(exactly = 0) { OCRConstant.uploadPath = any() }
        verify(exactly = 0) { OCRConstant.getResultPath = any() }
        verify(exactly = 0) { OcrSdk.init(any()) }
    }

    @Test
    fun `missing result path fails before saving either path or initializing SDK`() {
        val builder = OCRConfig.builder().setContext(context).setToken("token")
            .setBaseUrl("https://example.com/").setUploadPath("custom/upload")

        val error = assertThrows(IllegalArgumentException::class.java) { builder.build() }

        assertEquals("A required value for setting configuration wasn't provided: getResultPath", error.message)
        verify(exactly = 0) { OCRConstant.uploadPath = any() }
        verify(exactly = 0) { OCRConstant.getResultPath = any() }
        verify(exactly = 0) { OcrSdk.init(any()) }
    }

    @Test
    fun `previous configuration does not supply paths to a new builder`() {
        OCRConfig.builder().setContext(context).setToken("token").setBaseUrl("https://example.com/")
            .setUploadPath("custom/upload").setGetResultPath("custom/result").build()

        val builder = OCRConfig.builder().setContext(context).setToken("token")
            .setBaseUrl("https://example.com/")

        assertThrows(IllegalArgumentException::class.java) { builder.build() }
        verify(exactly = 1) { OcrSdk.init(any()) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank upload path is rejected`() {
        OCRConfig.builder().setUploadPath(" \t")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank result path is rejected`() {
        OCRConfig.builder().setGetResultPath("")
    }
}
