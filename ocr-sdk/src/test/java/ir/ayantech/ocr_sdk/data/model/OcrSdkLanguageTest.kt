package ir.ayantech.ocr_sdk.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrSdkLanguageTest {
    @Test
    fun `launch configs default to Persian independently of device language`() {
        assertEquals(OcrSdkLanguage.PERSIAN, OcrSdkOcrConfig().language)
        assertEquals(OcrSdkLanguage.PERSIAN, OcrSdkCaptureConfig().language)
    }

    @Test
    fun `language tags match explicit host choice`() {
        assertEquals("en", OcrSdkLanguage.ENGLISH.languageTag)
        assertEquals("fa", OcrSdkLanguage.PERSIAN.languageTag)
    }
}
