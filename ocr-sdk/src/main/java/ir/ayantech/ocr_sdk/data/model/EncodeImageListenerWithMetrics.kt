package ir.ayantech.ocr_sdk.data.model

import ir.ayantech.ocr_sdk.tools.EncodeImageListener

data class EncodeMetrics(
    val durationMs: Long,
    val finalQuality: Int,
    val finalWidth: Int,
    val finalHeight: Int,
    val base64MegaByte: Double,
    val approxBase64Bytes: Long,
    val inSampleSize: Int,
    val rotationApplied: Int,
    val decodeBudgetBytes: Long,
    val targetLongEdge: Int,
)

interface EncodeImageListenerWithMetrics : EncodeImageListener {
    fun onSuccess(base64: String, metrics: EncodeMetrics)
}