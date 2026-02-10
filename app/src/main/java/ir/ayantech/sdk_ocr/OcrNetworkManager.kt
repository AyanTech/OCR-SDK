package ir.ayantech.sdk_ocr

import android.content.Context
import android.util.Log
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import okhttp3.*
import okio.Buffer
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OcrNetworkManager(private val context: Context) {

    private val customLoggingInterceptor = Interceptor { chain ->
        val request = chain.request()
        val requestLog = StringBuilder()
        requestLog.append("\n--- 🚀 OCR REQUEST START ---\n")
        requestLog.append("URL: ${request.url}\n")

        val buffer = Buffer()
        request.body?.writeTo(buffer)
        val bodyString = buffer.readUtf8()
        val displayBody = if (bodyString.length > 2000) {
            bodyString.take(1000) + "... [⚠️ DATA TOO LARGE]"
        } else {
            bodyString
        }
        requestLog.append("Body: $displayBody\n")
        Log.d("OCR_NETWORK", requestLog.toString())

        val response = chain.proceed(request)
        val responseBody = response.peekBody(Long.MAX_VALUE).string()
        Log.d("OCR_NETWORK", "\n--- ✅ OCR RESPONSE START ---\n$responseBody")
        response
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(customLoggingInterceptor)
        .addInterceptor(ChuckerInterceptor.Builder(context).collector(ChuckerCollector(context)).maxContentLength(250000L).build())
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    interface OcrCallback {
        fun onSuccess(data: OcrResponseModel)
        fun onError(message: String)
    }

    fun sendOcrRequest(
        cardType: String,
        cardFrontBase64: String,
        cardBackBase64: String,
        traceNumber: String,
        token: String,
        callback: OcrCallback
    ): Call { // خروجی Call برای لغو
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
                if (call.isCanceled()) return
                callback.onError("خطا در شبکه: ${e.localizedMessage}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (call.isCanceled()) return
                val rawResponse = response.body?.string() ?: ""
                if (response.isSuccessful && rawResponse.isNotEmpty()) {
                    callback.onSuccess(parseDynamicJson(rawResponse))
                } else {
                    callback.onError("خطای سرور: ${response.code}")
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
        } catch (e: Exception) { Log.e("OCR_NETWORK", "Parsing Error", e) }
        return ocrResponseModel
    }
}