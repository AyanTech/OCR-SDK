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
import android.util.Base64
import androidx.annotation.StringRes
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import ir.ayantech.ocr_sdk.R
import ir.ayantech.ocr_sdk.data.model.EncodeImageListenerWithMetrics
import ir.ayantech.ocr_sdk.data.model.EncodeMetrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.OutputStream
import kotlin.math.max

internal object ImageBase64Engine {
    private const val BYTES_PER_MB = 1024L * 1024L
    private const val PROBE_EDGE = 512
    private const val PROBE_QUALITY = 95
    private const val MAX_SHRINK_ATTEMPTS = 3

    suspend fun encode(
        context: Context,
        imageUri: Uri?,
        maxBase64Mb: Double = 4.0,
        minBase64Mb: Double? = null,
        maxOriginalFileMb: Double? = null,
        maxOriginalMegaPixels: Double? = null,
        listener: EncodeImageListener,
    ): Unit = withContext(Dispatchers.IO) {
        val reporter = Reporter(context, listener)
        val result = try {
            processImage(
                context,
                imageUri,
                maxBase64Mb,
                minBase64Mb,
                maxOriginalFileMb,
                maxOriginalMegaPixels,
                reporter,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (oom: OutOfMemoryError) {
            currentCoroutineContext().ensureActive()
            reporter.fail(R.string.ocr_encode_out_of_memory, oom)
            return@withContext
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            val error = exception as? EncodingException
            reporter.fail(
                error?.messageResource ?: R.string.ocr_encode_failed,
                exception,
                *(error?.arguments ?: emptyArray())
            )
            return@withContext
        }
        reporter.success(result)
    }

    private suspend fun processImage(
        context: Context,
        imageUri: Uri?,
        maxBase64Mb: Double,
        minBase64Mb: Double?,
        maxOriginalFileMb: Double?,
        maxOriginalMegaPixels: Double?,
        reporter: Reporter,
    ): EncodedImage {
        val uri = imageUri ?: throw EncodingException(R.string.ocr_encode_no_image)
        val limits =
            validateLimits(maxBase64Mb, minBase64Mb, maxOriginalFileMb, maxOriginalMegaPixels)
        val start = SystemClock.elapsedRealtimeNanos()
        val resolver = context.contentResolver
        reporter.progress(0, R.string.ocr_encode_starting)
        val dimensions = inspectImage(resolver, uri, limits, reporter)
        val budget = decodeBudget(context)
        reporter.progress(15, R.string.ocr_encode_sampling)
        reporter.progress(25, R.string.ocr_encode_selecting_scale)
        val sample = chooseSample(resolver, uri, dimensions, limits.maxBase64Bytes, budget)
        return encodeSample(
            resolver,
            uri,
            dimensions,
            sample,
            budget,
            limits.maxBase64Bytes,
            start,
            reporter
        )
    }

    private fun validateLimits(
        maxBase64Mb: Double,
        minBase64Mb: Double?,
        maxOriginalFileMb: Double?,
        maxOriginalMegaPixels: Double?,
    ): Limits {
        val maxBytes = megabytesToBytes(maxBase64Mb)
        require(maxBytes in 4..Int.MAX_VALUE.toLong())
        minBase64Mb?.let { require(megabytesToBytes(it) <= maxBytes) }
        maxOriginalMegaPixels?.let { require(it.isFinite() && it > 0) }
        return Limits(
            maxBytes,
            maxOriginalFileMb?.let(::megabytesToBytes),
            maxOriginalFileMb,
            maxOriginalMegaPixels
        )
    }

    private suspend fun inspectImage(
        resolver: ContentResolver,
        uri: Uri,
        limits: Limits,
        reporter: Reporter,
    ): Dimensions {
        if (limits.maxOriginalBytes != null) {
            val size = fileSize(resolver, uri)
            if (size != null && size > limits.maxOriginalBytes) {
                throw EncodingException(R.string.ocr_encode_file_too_large, limits.maxOriginalMb)
            }
        }
        reporter.progress(10, R.string.ocr_encode_reading_dimensions)
        val dimensions = readDimensions(resolver, uri)
        if (limits.maxOriginalMegaPixels != null &&
            dimensions.pixels / 1_000_000.0 > limits.maxOriginalMegaPixels
        ) {
            throw EncodingException(R.string.ocr_encode_resolution_too_high)
        }
        return dimensions
    }

    private suspend fun chooseSample(
        resolver: ContentResolver,
        uri: Uri,
        dimensions: Dimensions,
        maxBase64Bytes: Long,
        budget: Long,
    ): Int {
        var probeSample = 1
        while (dimensions.longEdge / probeSample > PROBE_EDGE) probeSample *= 2
        val probe = decode(resolver, uri, probeSample)
        val bytesPerPixel = try {
            val pixels = probe.bitmap.width.toLong() * probe.bitmap.height
            (encodedSize(probe.bitmap, PROBE_QUALITY).toDouble() / pixels).coerceAtLeast(1.0)
        } finally {
            probe.bitmap.recycle()
        }
        val desiredPixels = (maxBase64Bytes * 0.98 / bytesPerPixel).toLong()
            .coerceIn(1, dimensions.pixels)
        var sample = 1
        while (dimensions.sampledPixels(sample) > minOf(desiredPixels, budget / 4)) {
            if (sample >= dimensions.longEdge || sample >= (1 shl 30)) break
            sample *= 2
        }
        return sample
    }

    private suspend fun encodeSample(
        resolver: ContentResolver,
        uri: Uri,
        dimensions: Dimensions,
        sample: Int,
        budget: Long,
        maxBase64Bytes: Long,
        start: Long,
        reporter: Reporter,
    ): EncodedImage {
        reporter.progress(35, R.string.ocr_encode_loading)
        val image = BitmapOwner(decode(resolver, uri, sample))
        image.use {
            maybeUseLargerSample(image, resolver, uri, dimensions, maxBase64Bytes, budget)
            reporter.progress(55, R.string.ocr_encode_adjusting_quality)
            val quality = fitWithinLimit(image, maxBase64Bytes, reporter)
            reporter.progress(80, R.string.ocr_encode_creating_output)
            val base64 = encodeBase64(image.bitmap, quality, maxBase64Bytes)
            val duration = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000L
            return EncodedImage(base64, metrics(base64, image, quality, budget, duration))
        }
    }

    private suspend fun maybeUseLargerSample(
        image: BitmapOwner,
        resolver: ContentResolver,
        uri: Uri,
        dimensions: Dimensions,
        maxBase64Bytes: Long,
        budget: Long,
    ) {
        if (image.sample <= 1) return
        val targetBytes = maxBase64Bytes * 0.98
        if (encodedSize(image.bitmap, PROBE_QUALITY) >= targetBytes * 0.70) return
        val nextSample = image.sample / 2
        val needed = dimensions.sampledPixels(nextSample) * 4
        if (needed > budget) return
        image.replace(decode(resolver, uri, nextSample))
    }

    private suspend fun fitWithinLimit(
        image: BitmapOwner,
        maxBase64Bytes: Long,
        reporter: Reporter,
    ): JpegQuality {
        repeat(MAX_SHRINK_ATTEMPTS + 1) { attempt ->
            findQuality(image.bitmap, maxBase64Bytes)?.let { return it }
            if (attempt == MAX_SHRINK_ATTEMPTS || !image.canShrink()) {
                throw EncodingException(R.string.ocr_encode_output_too_large)
            }
            reporter.progress(60, R.string.ocr_encode_resizing)
            currentCoroutineContext().ensureActive()
            image.shrink()
        }
        throw EncodingException(R.string.ocr_encode_output_too_large)
    }

    private suspend fun encodeBase64(bitmap: Bitmap, quality: JpegQuality, limit: Long): String {
        return JpegBuffer(((quality.bytes / 4) * 3).toInt()).use { buffer ->
            compress(bitmap, quality.value, buffer)
            if (base64Size(buffer.size().toLong()) > limit) {
                throw EncodingException(R.string.ocr_encode_output_too_large)
            }
            buffer.toBase64()
        }
    }

    private fun metrics(
        base64: String,
        image: BitmapOwner,
        quality: JpegQuality,
        budget: Long,
        duration: Long,
    ) = EncodeMetrics(
        durationMs = duration,
        finalQuality = quality.value,
        finalWidth = image.bitmap.width,
        finalHeight = image.bitmap.height,
        base64MegaByte = base64.length.toDouble() / BYTES_PER_MB,
        approxBase64Bytes = base64.length.toLong(),
        inSampleSize = image.sample,
        rotationApplied = 0,
        decodeBudgetBytes = budget,
        targetLongEdge = -1,
    )

    private fun megabytesToBytes(value: Double): Long {
        require(value.isFinite() && value > 0 && value <= Long.MAX_VALUE.toDouble() / BYTES_PER_MB)
        return (value * BYTES_PER_MB).toLong().also { require(it > 0) }
    }

    private suspend fun fileSize(resolver: ContentResolver, uri: Uri): Long? {
        try {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) {
                    cursor.getLong(index).takeIf { it >= 0 }?.let { return it }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
        }
        return try {
            resolver.openAssetFileDescriptor(uri, "r")
                ?.use { it.length.takeIf { size -> size >= 0 } }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            null
        }
    }

    private fun readDimensions(resolver: ContentResolver, uri: Uri): Dimensions {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth > 0 && options.outHeight > 0) return Dimensions(
            options.outWidth,
            options.outHeight
        )
        resolver.openInputStream(uri)?.use {
            val exif = ExifInterface(it)
            val width = exif.getAttributeInt(
                ExifInterface.TAG_PIXEL_X_DIMENSION,
                exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            )
            val height = exif.getAttributeInt(
                ExifInterface.TAG_PIXEL_Y_DIMENSION,
                exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
            )
            if (width > 0 && height > 0) return Dimensions(width, height)
        }
        throw EncodingException(R.string.ocr_encode_unreadable)
    }

    private fun decodeBudget(context: Context): Long {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val largeHeap = context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP != 0
        val memoryClass = if (largeHeap) manager.largeMemoryClass else manager.memoryClass
        val runtime = Runtime.getRuntime()
        val available = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()
        return minOf(
            available / 2,
            memoryClass * BYTES_PER_MB / 4,
            96 * BYTES_PER_MB
        ).coerceAtLeast(4)
    }

    private suspend fun decode(
        resolver: ContentResolver,
        uri: Uri,
        initialSample: Int
    ): DecodedImage {
        var sample = initialSample
        repeat(4) {
            currentCoroutineContext().ensureActive()
            try {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                    inScaled = false
                }
                val bitmap = resolver.openInputStream(uri)
                    ?.use { BitmapFactory.decodeStream(it, null, options) }
                    ?: throw EncodingException(R.string.ocr_encode_loading_failed)
                return DecodedImage(bitmap, sample)
            } catch (oom: OutOfMemoryError) {
                if (it == 3 || sample > Int.MAX_VALUE / 2) throw oom
                sample *= 2
            }
        }
        throw EncodingException(R.string.ocr_encode_loading_failed)
    }

    private suspend fun compress(bitmap: Bitmap, quality: Int, output: OutputStream) {
        currentCoroutineContext().ensureActive()
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
            throw EncodingException(R.string.ocr_encode_compression_failed)
        }
        currentCoroutineContext().ensureActive()
    }

    private suspend fun encodedSize(bitmap: Bitmap, quality: Int): Long {
        val counter = CountingOutputStream()
        compress(bitmap, quality, counter)
        return base64Size(counter.count)
    }

    private fun base64Size(bytes: Long): Long = ((bytes + 2) / 3) * 4

    private suspend fun findQuality(bitmap: Bitmap, limit: Long): JpegQuality? =
        selectJpegQuality(limit) { quality -> encodedSize(bitmap, quality) }

    private class Reporter(
        private val context: Context,
        private val listener: EncodeImageListener
    ) {
        suspend fun progress(percent: Int, @StringRes message: Int, vararg arguments: Any) {
            withContext(Dispatchers.Main) {
                listener.onProgress(percent, context.getString(message, *arguments))
            }
        }

        suspend fun fail(@StringRes message: Int, cause: Throwable, vararg arguments: Any?) {
            withContext(Dispatchers.Main) {
                listener.onFailed(context.getString(message, *arguments), cause)
            }
        }

        suspend fun success(image: EncodedImage) {
            progress(100, R.string.ocr_encode_complete, image.metrics.durationMs)
            withContext(Dispatchers.Main) {
                if (listener is EncodeImageListenerWithMetrics) {
                    listener.onSuccess(image.base64, image.metrics)
                } else {
                    listener.onSuccess(image.base64)
                }
            }
        }

    }

    private class BitmapOwner(decoded: DecodedImage) : Closeable {
        var bitmap: Bitmap = decoded.bitmap
            private set
        var sample: Int = decoded.sample
            private set

        fun replace(decoded: DecodedImage) {
            if (decoded.bitmap !== bitmap) bitmap.recycle()
            bitmap = decoded.bitmap
            sample = decoded.sample
        }

        fun canShrink(): Boolean = smallerWidth() < bitmap.width || smallerHeight() < bitmap.height

        fun shrink() {
            val width = smallerWidth()
            val height = smallerHeight()
            if (width == bitmap.width && height == bitmap.height) return
            val smaller = bitmap.scale(width, height)
            replace(DecodedImage(smaller, sample))
        }

        private fun smallerWidth() = (bitmap.width * 0.85).toInt().coerceAtLeast(1)
        private fun smallerHeight() = (bitmap.height * 0.85).toInt().coerceAtLeast(1)

        override fun close() = bitmap.recycle()
    }

    private class EncodingException(
        @param:StringRes val messageResource: Int,
        vararg val arguments: Any?,
    ) : IOException()

    private data class Limits(
        val maxBase64Bytes: Long,
        val maxOriginalBytes: Long?,
        val maxOriginalMb: Double?,
        val maxOriginalMegaPixels: Double?,
    )

    private data class Dimensions(val width: Int, val height: Int) {
        val pixels: Long get() = width.toLong() * height
        val longEdge: Int get() = max(width, height)
        fun sampledPixels(sample: Int): Long =
            ((width.toLong() + sample - 1) / sample) * ((height.toLong() + sample - 1) / sample)
    }

    private data class DecodedImage(val bitmap: Bitmap, val sample: Int)
    private data class EncodedImage(val base64: String, val metrics: EncodeMetrics)

    private class CountingOutputStream : OutputStream() {
        var count = 0L
            private set

        override fun write(value: Int) {
            count++
        }

        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            count += length
        }

        override fun write(bytes: ByteArray) {
            count += bytes.size
        }
    }

    private class JpegBuffer(capacity: Int) : ByteArrayOutputStream(capacity) {
        fun toBase64(): String = Base64.encodeToString(buf, 0, count, Base64.NO_WRAP)
    }
}

internal data class JpegQuality(val value: Int, val bytes: Long)

internal suspend fun selectJpegQuality(
    limit: Long,
    measure: suspend (Int) -> Long,
): JpegQuality? {
    val maximum = measure(100)
    if (maximum <= limit) return JpegQuality(100, maximum)
    val minimum = measure(60)
    if (minimum > limit) return null
    var best = JpegQuality(60, minimum)
    var low = 61
    var high = 99
    while (low <= high) {
        val quality = low + (high - low) / 2
        val bytes = measure(quality)
        if (bytes <= limit) {
            best = JpegQuality(quality, bytes)
            low = quality + 1
        } else {
            high = quality - 1
        }
    }
    return best
}
