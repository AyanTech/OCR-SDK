package ir.ayantech.sdk_ocr

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ir.ayantech.ocr_sdk.R as OcrR
import ir.ayantech.ocr_sdk.data.model.OcrSdkCaptureConfig
import ir.ayantech.ocr_sdk.data.model.OcrSdkLanguage
import ir.ayantech.ocr_sdk.data.model.OcrSdkOcrConfig
import ir.ayantech.ocr_sdk.data.model.OcrSdkTextBlock
import ir.ayantech.ocr_sdk.tools.OCRConstant
import ir.ayantech.ocr_sdk.tools.OcrHelper
import ir.ayantech.ocr_sdk.tools.OcrSdk
import ir.ayantech.ocr_sdk.ui.activity.OcrActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OcrLanguageInstrumentedTest {
    @Test
    fun sdkUsesLaunchLanguageForOcrAndCapture() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        OCRConstant.context = context
        OCRConstant.Base_URL = "https://example.invalid/"
        OCRConstant.Token = "test-token"
        OcrSdk.init(context)

        val customText = OcrSdkTextBlock(
            secondTitle = R.string.ocr_sample_second_title,
            firstImageHolderText = R.string.ocr_sample_front,
            secondImageHolderText = R.string.ocr_sample_back,
            buttonText = R.string.ocr_sample_confirm
        )
        val englishIntent = Intent(context, OcrActivity::class.java)
            .setAction(OcrHelper.Actions.OCR_RETURN_DATA)
            .putExtra(OcrHelper.Extras.CONFIG, OcrSdkOcrConfig(language = OcrSdkLanguage.ENGLISH, singlePhoto = false, textBlock = customText))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val english = instrumentation.startActivitySync(englishIntent) as OcrActivity
        try {
            instrumentation.runOnMainSync { english.supportFragmentManager.executePendingTransactions() }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertEquals("Please wait", english.getString(OcrR.string.ocr_loading_description))
                assertEquals("Send", english.getString(OcrR.string.ocr_send))
                assertEquals("Confirm", english.findViewById<Button>(OcrR.id.btnSendImages).text.toString())
                assertEquals("Take a photo of your bank card.", english.findViewById<TextView>(OcrR.id.tvVin).text.toString())
                assertEquals(View.LAYOUT_DIRECTION_LTR, english.window.decorView.layoutDirection)
                assertTrue(arrowInkCenterX(english.findViewById(OcrR.id.backIv)) < 36f)
                val front = english.findViewById<View>(OcrR.id.captureA)
                val back = english.findViewById<View>(OcrR.id.captureB)
                val frontBounds = Rect()
                val backBounds = Rect()
                assertEquals(View.VISIBLE, front.visibility)
                assertEquals(View.VISIBLE, back.visibility)
                assertTrue(front.getGlobalVisibleRect(frontBounds))
                assertTrue(back.getGlobalVisibleRect(backBounds))
                assertTrue(frontBounds.right <= backBounds.left)
            }
        } finally {
            instrumentation.runOnMainSync { english.finish() }
        }

        val persianIntent = Intent(context, OcrActivity::class.java)
            .setAction(OcrHelper.Actions.CAPTURE_URI)
            .putExtra(OcrHelper.Extras.CONFIG, OcrSdkCaptureConfig(language = OcrSdkLanguage.PERSIAN, textBlock = customText))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val persian = instrumentation.startActivitySync(persianIntent) as OcrActivity
        try {
            instrumentation.runOnMainSync {
                persian.supportFragmentManager.executePendingTransactions()
                assertEquals("لطفا کمی صبر کنید", persian.getString(OcrR.string.ocr_loading_description))
                assertEquals("ارسال", persian.getString(OcrR.string.ocr_send))
                assertEquals("تایید", persian.findViewById<Button>(OcrR.id.btnSendImages).text.toString())
                assertEquals("از کارت بانکی خود عکس بگیرید.", persian.findViewById<TextView>(OcrR.id.tvVin).text.toString())
                assertEquals(View.LAYOUT_DIRECTION_RTL, persian.window.decorView.layoutDirection)
                assertTrue(arrowInkCenterX(persian.findViewById(OcrR.id.backIv)) > 36f)
            }
        } finally {
            instrumentation.runOnMainSync { persian.finish() }
        }

        val singleIntent = Intent(context, OcrActivity::class.java)
            .setAction(OcrHelper.Actions.OCR_RETURN_DATA)
            .putExtra(OcrHelper.Extras.CONFIG, OcrSdkOcrConfig(singlePhoto = true))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val single = instrumentation.startActivitySync(singleIntent) as OcrActivity
        try {
            instrumentation.runOnMainSync { single.supportFragmentManager.executePendingTransactions() }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertEquals(View.VISIBLE, single.findViewById<View>(OcrR.id.captureA).visibility)
                assertEquals(View.GONE, single.findViewById<View>(OcrR.id.captureB).visibility)
                assertEquals(View.GONE, single.findViewById<View>(OcrR.id.tvDescB).visibility)
            }
        } finally {
            instrumentation.runOnMainSync { single.finish() }
        }
    }

    private fun arrowInkCenterX(image: ImageView): Float {
        val drawable = image.drawable.constantState!!.newDrawable().mutate()
        drawable.layoutDirection = image.layoutDirection
        drawable.setBounds(0, 0, 72, 72)
        val bitmap = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888)
        drawable.draw(Canvas(bitmap))
        var weightedX = 0L
        var weight = 0L
        for (y in 12 until 30) {
            for (x in 0 until 72) {
                val alpha = bitmap.getPixel(x, y) ushr 24
                weightedX += x * alpha
                weight += alpha
            }
        }
        assertTrue(weight > 0)
        return weightedX.toFloat() / weight
    }
}
