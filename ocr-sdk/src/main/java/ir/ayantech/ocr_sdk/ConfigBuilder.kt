package ir.ayantech.ocr_sdk

import android.content.Context

class ConfigBuilder private constructor() {

    var token: String? = null
    var baseUrl: String? = null
    var uploadImageEndPoint: String? = null
    var getResultEndPoint: String? = null
    var ocrContext: Context? = null

    fun setContext(ocrContext: Context) = apply {
        Constant.context = ocrContext
        this.ocrContext = ocrContext
    }

    @JvmOverloads
    fun setToken(token: String) = apply {
        Constant.Token = token
        this.token = token
    }

    @JvmOverloads
    fun setBaseUrl(baseUrl: String) = apply {
        Constant.Base_URL =  baseUrl
        this.baseUrl = baseUrl
    }

    @JvmOverloads
    fun setUploadImageEndPoint(uploadImageEndPoint: String) = apply {
        Constant.EndPoint_UploadCardOCR = uploadImageEndPoint
        this.uploadImageEndPoint = uploadImageEndPoint
    }

    @JvmOverloads
    fun setGetResultEndPoint(getResultEndPoint: String) = apply {
        Constant.EndPoint_GetCardOcrResult = getResultEndPoint
        this.getResultEndPoint = getResultEndPoint
    }

    fun build(): OCRConfig {
        val missingValue = "A required value for setting configuration wasn't provided: "
        requireNotNull(token) { missingValue + "token" }
        requireNotNull(baseUrl) { missingValue + "baseUrl" }
        requireNotNull(uploadImageEndPoint) { missingValue + "uploadImageEndPoint" }
        requireNotNull(getResultEndPoint) { missingValue + "getResultEndPoint" }
        requireNotNull(ocrContext) { missingValue + "ocrContext" }

        // Consider additional validations or logic as needed

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
    val uploadImageEndPoint: String = builder.uploadImageEndPoint!!
    val getResultEndPoint: String = builder.getResultEndPoint!!
    val ocrContext: Context = builder.ocrContext!!

    companion object {
        @JvmStatic
        fun builder() = ConfigBuilder.create()
    }
}