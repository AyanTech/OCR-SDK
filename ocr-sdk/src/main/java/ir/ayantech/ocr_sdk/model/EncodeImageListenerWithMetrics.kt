package ir.ayantech.ocr_sdk.model

import ir.ayantech.ocr_sdk.tools.EncodeImageListener

data class EncodeMetrics(
    val durationMs: Long,          // مدت‌زمان کل
    val finalQuality: Int,         // کیفیت JPEG انتخاب‌شده
    val finalWidth: Int,           // عرض نهایی
    val finalHeight: Int,          // ارتفاع نهایی
    val base64MegaByte: Double,         // طول واقعی Base64 (بایت)
    val approxBase64Bytes: Long,   // تخمین طول Base64 از مرحلهٔ انتخاب کیفیت
    val inSampleSize: Int,         // inSampleSize استفاده‌شده در دیکود
    val rotationApplied: Int,      // چرخش اعمال‌شده (درجه)
    val decodeBudgetBytes: Long,   // بودجهٔ RAM هنگام دیکود
    val targetLongEdge: Int        // لبهٔ بلند هدف برای پیش‌اسکیل
)

interface EncodeImageListenerWithMetrics : EncodeImageListener {
    fun onSuccess(base64: String, metrics: EncodeMetrics)
}