package ir.ayantech.ocr_sdk.tools

import android.content.Context

class ConfigBuilder private constructor() {

    var uploadPath: String? = null
        private set
    var getResultPath: String? = null
        private set

    fun setUploadPath(uploadPath: String) = apply {
        require(uploadPath.isNotBlank()) { "uploadPath must not be blank" }
        this.uploadPath = uploadPath.trim()
    }

    fun setGetResultPath(getResultPath: String) = apply {
        require(getResultPath.isNotBlank()) { "getResultPath must not be blank" }
        this.getResultPath = getResultPath.trim()
    }

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

        val configuredUploadPath = requireNotNull(uploadPath) { missingValue + "uploadPath" }
        val configuredGetResultPath = requireNotNull(getResultPath) { missingValue + "getResultPath" }

        OCRConstant.uploadPath = configuredUploadPath
        OCRConstant.getResultPath = configuredGetResultPath
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
