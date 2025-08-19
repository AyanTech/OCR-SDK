package ir.ayantech.ocr_sdk.tools

interface EncodeImageListener {
    fun onSuccess(base64: String)
    fun onFailed(reason: String, throwable: Throwable? = null)
    fun onProgress(percent: Int = -1, message: String = "")
}
