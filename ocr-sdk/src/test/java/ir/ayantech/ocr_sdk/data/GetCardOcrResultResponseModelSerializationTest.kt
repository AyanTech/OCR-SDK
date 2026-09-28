package ir.ayantech.ocr_sdk.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetCardOcrResultResponseModelSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Test
    fun `decodes real pending payload with null CardID and missing Retryable`() {
        val rawParameters = """
            {"CardID":null,"Description":null,"NextCallInterval":3000,"Result":null,"Status":"Pending"}
        """.trimIndent()

        val model = json.decodeFromString(
            GetCardOcrResult.GetCardOcrResultResponseModel.serializer(),
            rawParameters
        )

        assertEquals("Pending", model.status)
        assertEquals(3000L, model.nextCallInterval)
        assertNull(model.cardId)
        assertNull(model.description)
        assertNull(model.result)
        assertEquals(false, model.retryable)
    }
}
