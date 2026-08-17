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
        OCRConstant.Application_ID = applicationID
        this.applicationID = applicationID
    }
    @JvmOverloads
    fun setToken(token: String) = apply {
        OCRConstant.Token = token
        this.token = token
    }

    @JvmOverloads
    fun setBaseUrl(baseUrl: String) = apply {
        OCRConstant.Base_URL = baseUrl
        this.baseUrl = baseUrl
    }

    fun build(): OCRConfig {
        val missingValue = "A required value for setting configuration wasn't provided: "
        requireNotNull(token) { missingValue + "token" }
        requireNotNull(baseUrl) { missingValue + "baseUrl" }
        requireNotNull(ocrContext) { missingValue + "ocrContext" }

        // Consider additional validations or logic as needed

        OcrSdk.init(ocrContext!!)
        return OCRConfig(this)
    }

    companion object {
        @JvmStatic
        fun create() = ConfigBuilder()
    }
}
data class OCRConfig(private val builder: ConfigBuilder) {

    val token: String = builder.token!!
    val baseUrl: String = builder.baseUrl!!
    val ocrContext: Context = builder.ocrContext!!

    companion object {
        @JvmStatic
        fun builder() = ConfigBuilder.create()
    }
}
