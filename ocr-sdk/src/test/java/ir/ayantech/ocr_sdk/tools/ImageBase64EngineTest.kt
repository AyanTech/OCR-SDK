package ir.ayantech.ocr_sdk.tools

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class ImageBase64EngineTest {
    @Test
    fun `minimum quality remains valid when higher qualities exceed limit`() = runTest {
        val result = selectJpegQuality(40) { quality -> if (quality == 60) 40 else 80 }

        assertEquals(JpegQuality(60, 40), result)
    }

    @Test
    fun `maximum quality avoids unnecessary compression passes`() = runTest {
        val checked = mutableListOf<Int>()

        val result = selectJpegQuality(40) { quality ->
            checked += quality
            40
        }

        assertEquals(JpegQuality(100, 40), result)
        assertEquals(listOf(100), checked)
    }

    @Test
    fun `search selects highest quality within exact base64 budget`() = runTest {
        val result = selectJpegQuality(320) { quality -> quality * 4L }

        assertEquals(JpegQuality(80, 320), result)
    }

    @Test
    fun `oversized minimum returns no quality`() = runTest {
        assertNull(selectJpegQuality(40) { 44 })
    }

    @Test
    fun `measurement failure propagates`() {
        assertThrows(IOException::class.java) {
            runTest { selectJpegQuality(40) { throw IOException() } }
        }
    }

    @Test
    fun `cancellation stops quality search`() {
        val checked = mutableListOf<Int>()

        assertThrows(CancellationException::class.java) {
            runTest {
                selectJpegQuality(40) { quality ->
                    checked += quality
                    throw CancellationException()
                }
            }
        }
        assertEquals(listOf(100), checked)
    }
}
