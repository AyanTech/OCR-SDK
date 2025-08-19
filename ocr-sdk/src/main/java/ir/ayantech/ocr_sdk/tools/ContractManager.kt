package ir.ayantech.ocr_sdk.tools

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import ir.ayantech.ocr_sdk.ui.OcrActivity
import ir.ayantech.ocr_sdk.model.OcrSdkCaptureConfig
import ir.ayantech.ocr_sdk.model.OcrSdkOcrConfig
import ir.ayantech.ocr_sdk.model.OcrSdkOcrDataResult
import ir.ayantech.ocr_sdk.model.OcrSdkUriDataResult
import kotlin.jvm.java

// URI result
class CaptureContract() :
    ActivityResultContract<OcrSdkCaptureConfig, OcrSdkUriDataResult?>() {

    override fun createIntent(ctx: Context, input: OcrSdkCaptureConfig) =
        Intent(ctx, OcrActivity::class.java)
            .setAction(OcrHelper.Actions.CAPTURE_URI)
            .putExtra(OcrHelper.Extras.CONFIG, input)
            .putExtra(OcrHelper.Extras.TEXTS, input.textBlock)

    override fun parseResult(resultCode: Int, intent: Intent?): OcrSdkUriDataResult? =
        if (resultCode == Activity.RESULT_OK)
            intent?.parcelable(OcrHelper.Extras.RESULT)
        else null
}
class OCRContract() :
        ActivityResultContract<OcrSdkOcrConfig, OcrSdkOcrDataResult?>() {

        override fun createIntent(context: Context, input: OcrSdkOcrConfig) =
            Intent(context, OcrActivity::class.java)
                .setAction(OcrHelper.Actions.OCR_RETURN_DATA)
                .putExtra(OcrHelper.Extras.CONFIG, input)
                .putExtra(OcrHelper.Extras.TEXTS, input.textBlock )

        override fun parseResult(resultCode: Int, intent: Intent?): OcrSdkOcrDataResult? =
            if (resultCode == Activity.RESULT_OK)
                intent?.parcelable(OcrHelper.Extras.RESULT)
            else null
    }
