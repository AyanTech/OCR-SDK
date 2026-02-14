package ir.ayantech.sdk_ocr

import android.content.Context
import android.util.Log
import com.chuckerteam.chucker.api.BodyDecoder
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import okhttp3.*
import okio.Buffer
import okio.ByteString
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OcrNetworkManager(context: Context, var onLogReceived: (() -> Unit)? = null) {

    private val appContext = context.applicationContext

    private fun logLongString(tag: String, content: String) {
        val maxLength = 3500
        var i = 0
        while (i < content.length) {
            val end = Math.min(content.length, i + maxLength)
            Log.d(tag, content.substring(i, end))
            i += maxLength
        }
    }

    private val customLoggingInterceptor = Interceptor { chain ->
        val request = chain.request()
        onLogReceived?.invoke()

        val sb = StringBuilder("\n--- 🚀 OCR REQUEST ---")
        val body = request.body
        if (body is FormBody) {
            for (i in 0 until body.size) {
                val value = body.value(i)
                sb.append("\n${body.name(i)}: ${if (value.length > 100) value.take(100) + "..." else value}")
            }
        }
        Log.d("OCR_NETWORK_REQ", sb.toString())

        val response = chain.proceed(request)

        val responseBodyCopy = response.peekBody(Long.MAX_VALUE).string()
        Log.d("OCR_NETWORK_RES", "--- ✅ RESPONSE RECEIVED: ${response.code} ---")
        try {
            val prettyJson = JSONObject(responseBodyCopy).toString(4)
            logLongString("OCR_NETWORK_RES", prettyJson)
        } catch (e: Exception) {
            logLongString("OCR_NETWORK_RES", responseBodyCopy)
        }

        response
    }

    private val ocrBodyDecoder = object : BodyDecoder {

        override fun decodeRequest(request: Request, body: ByteString): String? {
            val originalBody = body.utf8()

            // اگر درخواست حاوی فیلد عکس بود، آن را سانسور کن
            return if (originalBody.contains("cardFrontImage") || originalBody.contains("cardBackImage")) {
                "⚠️ Request body contains large image data. Content hidden to prevent UI lag.\n\n" +
                        originalBody.split("&").joinToString("\n") {
                            if (it.contains("Image")) {
                                val key = it.split("=").firstOrNull() ?: it
                                "$key=[IMAGE_BASE64_HIDDEN]"
                            } else {
                                it
                            }
                        }
            } else {
                originalBody
            }
        }

        override fun decodeResponse(response: Response, body: ByteString): String? {
            // برای پاسخ (Response) هیچ تغییری نمی‌دهیم تا نتایج OCR کامل نمایش داده شود
            return body.utf8()
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(customLoggingInterceptor)
        .addInterceptor(
            ChuckerInterceptor.Builder(appContext)
                .collector(ChuckerCollector(appContext, showNotification = false))
                .maxContentLength(10_240L)
                .addBodyDecoder(ocrBodyDecoder) // اضافه شدن دیکودر
                .build()
        )
        .build()

    fun sendOcrRequest(
        cardType: String,
        cardFrontBase64: String,
        cardBackBase64: String,
        traceNumber: String,
        token: String,
        callback: OcrCallback
    ): Call {
        val formBody = FormBody.Builder()
            .add("token", token)
            .add("cardType", cardType)
            .add("traceNumber", traceNumber)
            .add("cardFrontImage", cardFrontBase64)
            .add("cardBackImage", cardBackBase64)
            .build()

        val request = Request.Builder()
            .url("http://185.173.105.138:8000/request-process/")
            .post(formBody)
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!call.isCanceled()) callback.onError("Network Error: ${e.localizedMessage}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (call.isCanceled()) return
                val rawResponse = response.body?.string() ?: ""
                if (response.isSuccessful && rawResponse.isNotEmpty()) {
                    callback.onSuccess(parseDynamicJson(rawResponse))
                } else {
                    callback.onError("Server Error: ${response.code}")
                }
            }
        })
        return call
    }

    private fun parseDynamicJson(jsonString: String): OcrResponseModel {
        val ocrResponseModel = OcrResponseModel(ocrRowModelList = mutableListOf())
        try {
            val root = JSONObject(jsonString)
            root.optJSONObject("status")?.let {
                ocrResponseModel.statusCode = it.optString("code", "-")
                ocrResponseModel.description = it.optString("description", "-")
            }
            root.optJSONObject("response")?.let { resp ->
                ocrResponseModel.timeSpent = resp.optString("time_spent", "-")
                val keys = resp.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key == "time_spent" || key == "trace_number") continue
                    val value = resp.optJSONObject(key)
                    if (value != null) {
                        val ocr = if (value.isNull("ocr") || value.optString("ocr").isEmpty()) "-" else value.getString("ocr")
                        val cleaned = if (value.isNull("cleaned") || value.optString("cleaned").isEmpty()) "-" else value.getString("cleaned")
                        ocrResponseModel.ocrRowModelList?.add(OcrRowModel(key.replace("_", " ").uppercase(), ocr, cleaned))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OCR_NETWORK", "Parsing Error", e)
        }
        return ocrResponseModel
    }

    fun clearChuckerLogs() {
        try {
            val databases = appContext.databaseList()
            databases.forEach { dbName ->
                if (dbName.contains("chucker", ignoreCase = true)) {
                    appContext.deleteDatabase(dbName)
                }
            }
        } catch (e: Exception) { }
    }

    interface OcrCallback {
        fun onSuccess(data: OcrResponseModel)
        fun onError(message: String)
    }
}