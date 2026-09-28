package ir.ayantech.ocr_sdk.tools

import android.content.Context

class ConfigBuilder private constructor() {

    var token: String? = null
    var baseUrl: String? = null
    var applicationID: String? = null
    var ocrContext: Context? = null

    fun setContext(ocrContext: Context) = apply {
        OCRConstant.context = ocrContext
        this.ocrContext = ocrContext
    }

    fun setApplicationID(applicationID: String) = apply {
        OCRConstant.application_ID = applicationID
        this.applicationID = applicationID
    }

    fun setToken(token: String) = apply {
        OCRConstant.token = token
        this.token = token
    }

    fun setBaseUrl(baseUrl: String) = apply {
        OCRConstant.baseURL = baseUrl
        this.baseUrl = baseUrl
    }

    fun build(): OCRConfig {
        val missingValue = "A required value for setting configuration wasn't provided: "
        requireNotNull(token) { missingValue + "token" }
        requireNotNull(baseUrl) { missingValue + "baseUrl" }
        requireNotNull(ocrContext) { missingValue + "ocrContext" }

        OcrSdk.init(ocrContext!!)
        return OCRConfig(this)
    }

    companion object {
        @JvmStatic
        fun create() = ConfigBuilder()
    }
}

data class OCRConfig(private val builder: ConfigBuilder) {
    companion object {
        @JvmStatic
        fun builder() = ConfigBuilder.create()
    }
}
