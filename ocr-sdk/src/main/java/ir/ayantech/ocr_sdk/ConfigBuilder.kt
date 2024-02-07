package ir.ayantech.ocr_sdk

import android.content.Context

class ConfigBuilder private constructor() {

   var token: String? = null
   var baseUrl: String? = null
   var uploadImageEndPoint: String? = null
   var getResultEndPoint: String? = null
    var ocrContext: Context? = null

    @JvmOverloads
    fun token(token: String) = apply { this.token = token }

    @JvmOverloads
    fun baseUrl(baseUrl: String) = apply { this.baseUrl = baseUrl }

    @JvmOverloads
    fun uploadImageEndPoint(uploadImageEndPoint: String) = apply { this.uploadImageEndPoint = uploadImageEndPoint }

    @JvmOverloads
    fun getResultEndPoint(getResultEndPoint: String) = apply { this.getResultEndPoint = getResultEndPoint }

    fun ocrContext(ocrContext: Context) = apply { this.ocrContext = ocrContext }

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