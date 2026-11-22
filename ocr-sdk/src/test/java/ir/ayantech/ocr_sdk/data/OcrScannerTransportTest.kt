package ir.ayantech.ocr_sdk.data

import com.sun.net.httpserver.HttpServer
import ir.ayantech.networking.ayanModel.LogLevel
import ir.ayantech.networking.datasource.impl.OcrRemoteDataSourceImpl
import ir.ayantech.networking.helper.AppSignatureHelper
import ir.ayantech.networking.repository.impl.OcrRepositoryImpl
import ir.ayantech.networking.v2.AyanApi
import ir.ayantech.ocr_sdk.domain.usecase.impl.GetCardOcrResultUseCaseImpl
import ir.ayantech.ocr_sdk.domain.usecase.impl.UploadCardOcrUseCaseImpl
import ir.ayantech.ocr_sdk.enums.OcrSdkOcrCardTypesEnum
import ir.ayantech.ocr_sdk.tools.OCRConstant
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.net.InetSocketAddress
import java.util.concurrent.LinkedBlockingQueue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE, shadows = [ScannerSignatureShadow::class])
class OcrScannerTransportTest {
    @Test(timeout = 30_000)
    fun configuredUploadAndPollHandleVehicleAndBankCardsAndStopOnServerFailure() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        OCRConstant.context = context
        OCRConstant.uploadPath = "scanner/upload"
        OCRConstant.getResultPath = "scanner/result"
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val requests = LinkedBlockingQueue<Pair<String, String>>()
        val replies = LinkedBlockingQueue<String>()
        server.createContext("/") { exchange ->
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            requests.add(exchange.requestURI.path to body)
            val reply = replies.poll()?.toByteArray(Charsets.UTF_8)
            if (reply == null) {
                exchange.sendResponseHeaders(500, -1)
                exchange.close()
            } else {
                exchange.responseHeaders.set("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, reply.size.toLong())
                exchange.responseBody.use { it.write(reply) }
            }
        }
        server.start()
        val client = AyanApi.Builder(context, "http://127.0.0.1:${server.address.port}/")
            .setInvokeUserToken { "fixture-token" }
            .setLogLevel(LogLevel.DO_NOT_LOG)
            .build()
        try {
            val repository = OcrRepositoryImpl(OcrRemoteDataSourceImpl(client))
            val upload = UploadCardOcrUseCaseImpl(repository)
            val poll = GetCardOcrResultUseCaseImpl(repository)
            for (card in listOf(OcrSdkOcrCardTypesEnum.VehicleCard, OcrSdkOcrCardTypesEnum.BankCard)) {
                val id = "fixture-${card.value}"
                replies.add(success("""{"FileID":"$id"}"""))
                replies.add(success("""{"Status":"Pending","NextCallInterval":1}"""))
                replies.add(success("""{"Status":"Successful","Result":[{"Key":"field","Value":"sample"}]}"""))
                val fileId = upload(UploadCardOcr.UploadCardOcrRequestBody(listOf("synthetic-image"), card.value)).getOrThrow().fileId
                val result = poll(GetCardOcrResult.GetCardOcrResultRequestBody(fileId)).getOrThrow()

                assertEquals(id, fileId)
                assertEquals("sample", result.result?.single()?.value)
                val (uploadPath, raw) = requests.remove()
                assertEquals("/scanner/upload", uploadPath)
                val envelope = Json.parseToJsonElement(raw).jsonObject
                assertEquals("fixture-token", envelope.getValue("Identity").jsonObject.getValue("Token").jsonPrimitive.content)
                val parameters = envelope.getValue("Parameters").jsonObject
                assertEquals(card.value, parameters.getValue("Type").jsonPrimitive.content)
                assertEquals("synthetic-image", parameters.getValue("ImageArray").jsonArray.single().jsonPrimitive.content)
                repeat(2) {
                    val (path, input) = requests.remove()
                    assertEquals("/scanner/result", path)
                    val query = Json.parseToJsonElement(input).jsonObject.getValue("Parameters").jsonObject
                    assertEquals(id, query.getValue("FileID").jsonPrimitive.content)
                }
            }
            replies.add("""{"Status":{"Code":"G00001","Description":"fixture rejection"},"Parameters":null}""")
            assertTrue(poll(GetCardOcrResult.GetCardOcrResultRequestBody("rejected")).isFailure)
            assertEquals("/scanner/result", requests.remove().first)
            assertTrue(requests.isEmpty())
        } finally {
            server.stop(0)
        }
    }

    private fun success(parameters: String): String {
        return """{"Status":{"Code":"G00000"},"Parameters":$parameters}"""
    }
}

@Implements(value = AppSignatureHelper::class, isInAndroidSdk = false)
class ScannerSignatureShadow {
    @Implementation
    fun getAppSignatures(): ArrayList<String> {
        return arrayListOf("fixture-signature")
    }
}
