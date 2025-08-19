package ir.ayantech.ocr_sdk.tools

import android.os.SystemClock
import android.view.View

object ExtensionFunction {


    private val TAG_LOCK_RUNNABLE = 0x77AACC11

    /**
     * دکمه را فوراً غیرفعال می‌کند و بعد از minMs میلی‌ثانیه خودکار فعال می‌کند.
     * اگر قبلاً قفلِ معلق داشته باشد، همان را لغو و قفل جدید اعمال می‌شود.
     * block (اختیاری): کاری که می‌خوای همون لحظه انجام بشه (مثل شروع آپلود/نمایش دیالوگ)
     */

        fun View.lockFor(
            minMs: Long = 1000,
            alphaDisabled: Float = 0.6f,
            alphaEnabled: Float = 1f,
            block: (() -> Unit)? = null
        ) {
            // لغو آنلاک قبلی اگر وجود داشته
            (getTag(TAG_LOCK_RUNNABLE) as? Runnable)?.let { removeCallbacks(it) }

            isEnabled = false
            alpha = alphaDisabled

            val startAt = SystemClock.elapsedRealtime()
            val unlock = Runnable {
                isEnabled = true
                alpha = alphaEnabled
                setTag(TAG_LOCK_RUNNABLE, null)
            }
            setTag(TAG_LOCK_RUNNABLE, unlock)

            val elapsed = SystemClock.elapsedRealtime() - startAt
            val remain = (minMs - elapsed).coerceAtLeast(0)
            postDelayed(unlock, remain)

            // اجرای کار اصلی (اختیاری)
            block?.invoke()
        }

        /** اگر لازم شد در onDestroyView فراخوانی کن تا تایمرِ معلق پاک شود. */
        fun View.cancelPendingUnlock() {
            (getTag(TAG_LOCK_RUNNABLE) as? Runnable)?.let {
                removeCallbacks(it)
                setTag(TAG_LOCK_RUNNABLE, null)
            }
        }
    }
