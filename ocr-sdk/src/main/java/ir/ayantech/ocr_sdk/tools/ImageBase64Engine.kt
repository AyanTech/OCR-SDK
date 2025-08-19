package ir.ayantech.ocr_sdk.tools

import android.app.ActivityManager
import android.content.ContentResolver
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import ir.ayantech.ocr_sdk.model.EncodeImageListenerWithMetrics
import ir.ayantech.ocr_sdk.model.EncodeMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min

internal object ImageBase64Engine {

    // ---------------- Logging ----------------
    private const val PUB_TAG = "OcrEnginge" // لاگ‌های سطح بالا
    private const val ENG_TAG = "OcrEnginge" // لاگ متدهای داخلی
    private val DEBUG_LOG: Boolean = true
    private inline fun logd(msg: () -> String) {
        if (DEBUG_LOG) Log.d(ENG_TAG, msg())
    }

    /** تایمر عمومی (خروجی به ثانیه) */
    private inline fun <T> timed(
        section: String,
        crossinline render: (T) -> String = { "" },
        block: () -> T
    ): T {
        val t0 = SystemClock.elapsedRealtimeNanos()
        try {
            val out = block()
            if (DEBUG_LOG) {
                val sec = (SystemClock.elapsedRealtimeNanos() - t0) / 1_000_000_000.0
                logd { "$section → ${render(out)} (took ${"%.3f".format(sec)} s)" }
            }
            return out
        } catch (e: Throwable) {
            if (DEBUG_LOG) {
                val sec = (SystemClock.elapsedRealtimeNanos() - t0) / 1_000_000_000.0
                logd { "$section ✗ failed after ${"%.3f".format(sec)} s: ${e.message}" }
            }
            throw e
        }
    }

    // ===================== Public API =====================
    @Suppress("DEPRECATION")
    suspend fun encode(
        context: Context,
        imageUri: Uri?,
        maxBase64Mb: Double = 4.0,
        minBase64Mb: Double? = null,
        maxOriginalFileMb: Double? = null,
        maxOriginalMegaPixels: Double? = null,
        listener: EncodeImageListener
    ): Unit = withContext(Dispatchers.IO) {

        suspend fun emitProgress(p: Int, msg: String) =
            withContext(Dispatchers.Main) { listener.onProgress(p, msg) }

        suspend fun emitFail(msg: String, t: Throwable? = null) =
            withContext(Dispatchers.Main) { listener.onFailed(msg, t) }

        suspend fun emitSuccess(base64: String) =
            withContext(Dispatchers.Main) { listener.onSuccess(base64) }

        try {
            val tStart = SystemClock.elapsedRealtimeNanos()
            if (imageUri == null) {
                emitFail("هیچ تصویری در دسترس نیست."); return@withContext
            }
            val cr = context.contentResolver
            emitProgress(0, "شروع پردازش تصویر")

            // --- 1) غربال اندازه فایل (اختیاری)
            if (maxOriginalFileMb != null) {
                val cap = mbToBytes(maxOriginalFileMb)
                getFileSizeBytesSafe(context, cr, imageUri)?.let { size ->
                    if (size > cap) {
                        emitProgress(5, "بررسی اندازهٔ تصویر")
                        emitFail("این تصویر بیش از ${"%.2f".format(maxOriginalFileMb)} مگابایت است. لطفاً تصویر کوچک‌تری انتخاب/ثبت کنید.")
                        return@withContext
                    }
                }
            }

            // --- 2) ابعاد ورودی
            emitProgress(10, "دریافت ابعاد تصویر")
            val size = readImageSize(cr, imageUri) ?: run {
                Log.d(PUB_TAG, "readImageSize -> null")
                emitFail("امکان خواندن این تصویر وجود ندارد.")
                return@withContext
            }
            val (srcW, srcH) = size

            if (maxOriginalMegaPixels != null) {
                val mp = (srcW.toLong() * srcH.toLong()) / 1_000_000.0
                if (mp > maxOriginalMegaPixels) {
                    emitFail("رزولوشن این تصویر بسیار بالاست. لطفاً تصویر کوچک‌تری ثبت/انتخاب کنید.")
                    return@withContext
                }
            }

            val MAX_B64 = mbToBytes(maxBase64Mb)
            val MIN_B64 = minBase64Mb?.let { mbToBytes(it) }

            // --- 3) Probe دقیق‌تر (longEdge=512) در q=95 برای تخمین Base64-per-pixel
            emitProgress(15, "نمونه‌گیری سریع")
            val probeLongEdge = 512
            val probeInSample = run {
                var s = 1
                while (max(srcW / s, srcH / s) > probeLongEdge && s <= 512) s *= 2
                s
            }
            val probeBmp = decodeSafely(cr, imageUri, probeInSample, Bitmap.Config.RGB_565)
                ?: run { emitFail("مشکل در خواندن تصویر."); return@withContext }

            val probeQ = 95
            val probeBytes64 = base64LenAtQuality(probeBmp, probeQ)
            val probePixels = (probeBmp.width * probeBmp.height).toLong().coerceAtLeast(1)
            probeBmp.recycle()

            val b64PerPx_at95 =
                (probeBytes64.toDouble() / probePixels.toDouble()).coerceAtLeast(1.0)
            val targetB64 = (MAX_B64 * 0.98).toLong() // هدف: نزدیکِ سقف
            val desiredPixels =
                (targetB64 / b64PerPx_at95).toLong().coerceIn(64, srcW.toLong() * srcH.toLong())

            // --- 4) انتخاب inSample نهایی با رعایت بودجه RAM
            emitProgress(25, "انتخاب مقیاس بهینه")
            val budget = computeDecodeBudgetBytes(context)
            val bytesPerPixel = 2 // RGB_565

            fun pixelsAtInSample(s: Int): Long {
                val w = (srcW / s).coerceAtLeast(1)
                val h = (srcH / s).coerceAtLeast(1)
                return w.toLong() * h.toLong()
            }

            var inSample = 1
            run {
                var s = 1
                while (pixelsAtInSample(s) > desiredPixels && s <= 512) s *= 2
                inSample = s.coerceAtLeast(1)
            }
            // گارد بودجه RAM
            while (true) {
                val w = (srcW / inSample).coerceAtLeast(1)
                val h = (srcH / inSample).coerceAtLeast(1)
                val need = w.toLong() * h.toLong() * bytesPerPixel
                if (need <= budget || inSample >= 512) break
                inSample *= 2
            }

            // --- 5) Decode نهایی (فقط یک‌بار)
            emitProgress(35, "بارگذاری نهایی تصویر")
            var bmp = decodeSafely(cr, imageUri, inSample, Bitmap.Config.RGB_565)
                ?: run { emitFail("امکان بارگذاری تصویر وجود ندارد."); return@withContext }

            // --- چرخش (طبق خواسته شما فعلاً غیرفعال) ---
            // val rotation = readExifRotation(cr, imageUri)
            // bmp = applyRotationIfNeeded(bmp, rotation)

            // --- 5.5) Underfill جبران: اگر Q=95 خیلی زیرِ هدف بود، یک مرحله inSample را نصف کن (در صورت کافی بودن RAM)
            val sAt95_initial = base64LenAtQuality(bmp, 95)
            val underfillRatio = sAt95_initial.toDouble() / targetB64
            if (underfillRatio < 0.70 && inSample > 1) {
                val nextInSample = (inSample / 2).coerceAtLeast(1)
                val w = (srcW / nextInSample).coerceAtLeast(1)
                val h = (srcH / nextInSample).coerceAtLeast(1)
                val need = w.toLong() * h.toLong() * bytesPerPixel
                if (need <= budget) {
                    decodeSafely(cr, imageUri, nextInSample, Bitmap.Config.RGB_565)?.let { bigger ->
                        bmp.recycle()
                        bmp = bigger
                        inSample = nextInSample
                    }
                }
            }

            // --- 6) انتخاب کیفیت با باینری‌سرچ برای چسبیدن بهتر به سقف Base64
            emitProgress(55, "تنظیم کیفیت خروجی")
            var best = findQualityBinaryByB64(
                bmp = bmp,
                maxB64 = MAX_B64,
                qMin = 60,
                qMax = 100,
                maxIter = 8,
                tolerancePct = 0.03
            )

            // اگر حتی با Q=60 جا نشد → ۱ تا ۳ بار اسکیل‌داون ملایم و تکرار
            var shrinkTries = 0
            while (best == null && shrinkTries < 3) {
                emitProgress(60, "کاهش جزئی ابعاد برای رسیدن به سقف")
                val factor = 0.85f
                val nw = (bmp.width * factor).toInt().coerceAtLeast(640)
                val nh = (bmp.height * factor).toInt().coerceAtLeast(480)
                val scaled = Bitmap.createScaledBitmap(bmp, nw, nh, true)
                if (scaled !== bmp) {
                    bmp.recycle(); bmp = scaled
                }
                best = findQualityBinaryByB64(bmp, MAX_B64, 60, 95, 7, 0.03)
                shrinkTries++
            }

            if (best == null) {
                bmp.recycle()
                emitFail("این تصویر حتی پس از کاهش کیفیت/اندازه هم از سقف مجاز بزرگ‌تر است. لطفاً تصویر کوچک‌تری ثبت/انتخاب کنید.")
                return@withContext
            }

            // (اختیاری) اگر حداقل تعریف شده و خروجی کمی پایین‌تر است، یک تلاش کوچک برای بیشتر کردن Q (تا سقف)
            if (MIN_B64 != null && best!!.second < MIN_B64) {
                val tryHigherQ = (best!!.first + 3).coerceAtMost(95)
                val sAtHigher = base64LenAtQuality(bmp, tryHigherQ)
                if (sAtHigher in MIN_B64..MAX_B64) {
                    best = tryHigherQ to sAtHigher
                }
            }

            // --- 7) ساخت خروجی Base64 (یک‌بار)
            emitProgress(80, "ساخت خروجی")
            val (bestQ, approxB64) = best!!
            val baos = ByteArrayOutputStream(
                min(
                    (((MAX_B64 * 3) / 4).coerceAtMost(8L * 1024 * 1024)),
                    Int.MAX_VALUE.toLong()
                ).toInt()
            )
            val okFinal = bmp.compress(Bitmap.CompressFormat.JPEG, bestQ, baos)
            if (!okFinal) {
                bmp.recycle(); baos.close()
                emitFail("امکان آماده‌سازی خروجی وجود ندارد. لطفاً دوباره تلاش کنید.")
                return@withContext
            }
            var base64 =
                android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)

            // اگر اندکی بالاتر رفت، ۱–۲ پله کیفیت پایین بیاور
            if (base64.length.toLong() > MAX_B64) {
                var q = (bestQ - 2).coerceAtLeast(50)
                repeat(2) {
                    baos.reset()
                    bmp.compress(Bitmap.CompressFormat.JPEG, q, baos)
                    val b64 = android.util.Base64.encodeToString(
                        baos.toByteArray(),
                        android.util.Base64.NO_WRAP
                    )
                    if (b64.length.toLong() <= MAX_B64) {
                        base64 = b64
                        return@repeat
                    }
                    q = (q - 2).coerceAtLeast(50)
                }
                if (base64.length.toLong() > MAX_B64) {
                    bmp.recycle(); baos.close()
                    emitFail("اندازهٔ خروجی کمی بالاتر از سقف مجاز است. لطفاً تصویر کوچک‌تری ثبت/انتخاب کنید.")
                    return@withContext
                }
            }

            // --- 8) لاگ نهایی و متریک‌ها
            val finalW = bmp.width
            val finalH = bmp.height
            val base64Bytes = base64.length.toLong()
            val durationMs = (SystemClock.elapsedRealtimeNanos() - tStart) / 1_000_000L
            val base64Mb = base64Bytes.toDouble() / (1024.0 * 1024.0)

            Log.d(
                PUB_TAG,
                "encodeImageToBase64 → base64=${"%.2f".format(base64Mb)}MB ($base64Bytes B), " +
                        "Q=$bestQ, approxB64=$approxB64 B, inSample=$inSample, final=${finalW}x${finalH}, " +
                        "budget=${"%.1f".format(computeDecodeBudgetBytes(context) / 1024.0 / 1024.0)}MB, total=${durationMs}ms"
            )
            Log.d(PUB_TAG, "\n")

            val metrics = EncodeMetrics(
                durationMs = durationMs,
                finalQuality = bestQ,
                finalWidth = finalW,
                finalHeight = finalH,
                base64MegaByte = base64Mb,
                approxBase64Bytes = approxB64,
                inSampleSize = inSample,
                rotationApplied = 0,               // چرخش غیرفعال
                decodeBudgetBytes = computeDecodeBudgetBytes(context),
                targetLongEdge = -1                // در این نسخه استفاده مستقیم نداریم
            )

            bmp.recycle()
            baos.close()

            emitProgress(100, "آماده شد • زمان کل: ${durationMs}ms")

            withContext(Dispatchers.Main) {
                if (listener is EncodeImageListenerWithMetrics) {
                    listener.onSuccess(base64, metrics)
                } else {
                    listener.onSuccess(base64)
                }
            }

        } catch (oom: OutOfMemoryError) {
            Log.d(PUB_TAG, "encode OOM: ${oom.message}")
            withContext(Dispatchers.Main) {
                listener.onFailed(
                    "حافظهٔ دستگاه برای پردازش این تصویر کافی نیست. لطفاً تصویر با کیفیت پایین‌تری ثبت/انتخاب کنید.",
                    oom
                )
            }
        } catch (t: Throwable) {
            Log.d(PUB_TAG, "encode Throwable: ${t.message}")
            withContext(Dispatchers.Main) {
                listener.onFailed("در پردازش تصویر مشکلی رخ داد. لطفاً دوباره تلاش کنید.", t)
            }
        }
    }

    // ===================== Helpers =====================

    private fun mbToBytes(mb: Double?): Long = timed("mbToBytes(mb=$mb)", { "$it B" }) {
        ((mb ?: 4.0) * 1024.0 * 1024.0).toLong()
    }

    private fun b64LenFromBytes(n: Long): Long =
        timed("b64LenFromBytes(bytes=$n)", { "$it B64-bytes" }) {
            ((n + 2L) / 3L) * 4L
        }

    private fun getFileSizeBytesSafe(ctx: Context, cr: ContentResolver, uri: Uri): Long? =
        timed("getFileSizeBytesSafe(uri=$uri)", { it?.let { "$it B" } ?: "null" }) {
            try {
                cr.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && c.moveToFirst() && !c.isNull(idx)) return@timed c.getLong(idx)
                }
            } catch (_: Exception) { /* ignore */
            }
            try {
                DocumentFile.fromSingleUri(ctx, uri)?.length()?.takeIf { it >= 0 }
                    ?.let { return@timed it }
            } catch (_: Exception) {
            }
            try {
                cr.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    if (afd.length >= 0) return@timed afd.length
                }
            } catch (_: Exception) { /* ignore */
            }
            null
        }

    private fun readImageSize(cr: ContentResolver, uri: Uri): Pair<Int, Int>? =
        timed("readImageSize(uri=$uri)", { it?.let { (w, h) -> "${w}x$h" } ?: "null" }) {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
            var w = o.outWidth;
            var h = o.outHeight
            if (w <= 0 || h <= 0) {
                try {
                    cr.openInputStream(uri)?.use {
                        val exif = ExifInterface(it)
                        w = exif.getAttributeInt(ExifInterface.TAG_PIXEL_X_DIMENSION, 0)
                        h = exif.getAttributeInt(ExifInterface.TAG_PIXEL_Y_DIMENSION, 0)
                        if (w <= 0 || h <= 0) {
                            w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                            h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                        }
                    }
                } catch (_: Throwable) {
                }
            }
            if (w > 0 && h > 0) w to h else null
        }

    private fun computeDecodeBudgetBytes(ctx: Context): Long =
        timed("computeDecodeBudgetBytes()", { "${it / 1024 / 1024} MB" }) {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val hasLargeHeap = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP) != 0
            val memClassMb = if (hasLargeHeap) am.largeMemoryClass else am.memoryClass

            val rt = Runtime.getRuntime()
            val used = rt.totalMemory() - rt.freeMemory()
            val free = rt.maxMemory() - used

            val byFree = (free * 0.5).toLong()                 // 50% از رم آزاد
            val byMemClass = (memClassMb * 1024L * 1024L) / 4  // 25% از MemoryClass
            max(
                2L * 1024 * 1024,
                min(byFree, min(byMemClass, 96L * 1024 * 1024))
            )
        }

    private fun decodeSafely(
        cr: ContentResolver,
        uri: Uri,
        startInSample: Int,
        cfg: Bitmap.Config
    ): Bitmap? = timed("decodeSafely(startInSample=$startInSample,cfg=$cfg)", { bmp ->
        bmp?.let { "${it.width}x${it.height}" } ?: "null"
    }) {
        var s = startInSample
        while (s <= 512) {
            try {
                cr.openInputStream(uri)?.use {
                    val o = BitmapFactory.Options().apply {
                        inSampleSize = s
                        inPreferredConfig = cfg
                    }
                    BitmapFactory.decodeStream(it, null, o)?.let { bmp -> return@timed bmp }
                }
            } catch (_: OutOfMemoryError) { /* try bigger inSample */
            }
            s *= 2
        }
        null
    }

    private class CountingOutputStream : OutputStream() {
        var count = 0
        override fun write(b: Int) {
            count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            count += len
        }

        override fun write(b: ByteArray) {
            count += b.size
        }
    }

    private fun base64LenAtQuality(bmp: Bitmap, q: Int): Long =
        timed("base64LenAtQuality(q=$q)", { "$it B64-bytes" }) {
            val cos = CountingOutputStream()
            val ok = bmp.compress(Bitmap.CompressFormat.JPEG, q, cos)
            if (!ok) Long.MAX_VALUE else b64LenFromBytes(cos.count.toLong())
        }

    /** جست‌وجوی دودویی کیفیت برای چسبیدن بهتر به سقف Base64 */
    private fun findQualityBinaryByB64(
        bmp: Bitmap,
        maxB64: Long,
        qMin: Int = 60,
        qMax: Int = 95,
        maxIter: Int = 7,
        tolerancePct: Double = 0.015
    ): Pair<Int, Long>? = timed(
        "findQualityBinaryByB64(cap=${maxB64}B,qMin=$qMin,qMax=$qMax)",
        { it?.let { (q, b64) -> "q=$q, base64=$b64" } ?: "null" }
    ) {
        var lo = qMin
        var hi = qMax
        var bestQ = -1
        var bestSize = -1L

        val sAtMin = base64LenAtQuality(bmp, qMin)
        if (sAtMin > maxB64) return@timed null

        var iter = 0
        while (iter++ < maxIter && hi >= lo) {
            val mid = (lo + hi + 1) / 2
            val s = base64LenAtQuality(bmp, mid)

            if (s <= maxB64) {
                bestQ = mid
                bestSize = s
                lo = mid
            } else {
                hi = mid - 1
            }

            if (bestSize > 0 && (maxB64 - bestSize).toDouble() / maxB64 <= tolerancePct) break
            if (hi - lo <= 1) break
        }
        if (bestQ >= 0) bestQ to bestSize else null
    }

    // ====== (در آینده اگر لازم شد) ======
    @Suppress("unused")
    private fun readExifRotation(cr: ContentResolver, uri: Uri): Int =
        timed("readExifRotation(uri=$uri)", { "$it°" }) {
            try {
                cr.openInputStream(uri)?.use { input ->
                    val exif = ExifInterface(input)
                    when (exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                } ?: 0
            } catch (_: Throwable) {
                0
            }
        }

    @Suppress("unused")
    private fun applyRotationIfNeeded(bmp: Bitmap, deg: Int): Bitmap =
        timed("applyRotationIfNeeded(deg=$deg,in=${bmp.width}x${bmp.height})", { out ->
            "${out.width}x${out.height}"
        }) {
            if (deg == 0) return@timed bmp
            val m = android.graphics.Matrix().apply { postRotate(deg.toFloat()) }
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (rotated !== bmp) {
                bmp.recycle(); rotated
            } else bmp
        }
}
