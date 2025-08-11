package ir.ayantech.ocr_sdk.tools

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import ir.ayantech.ocr_sdk.OcrActivity
import ir.ayantech.ocr_sdk.OcrHelper
import ir.ayantech.ocr_sdk.model.CaptureConfig
import ir.ayantech.ocr_sdk.model.OcrConfig
import ir.ayantech.ocr_sdk.model.OcrDataResult
import ir.ayantech.ocr_sdk.model.UriDataResult

// URI result
class CaptureContract :
    ActivityResultContract<CaptureConfig, UriDataResult?>() {

    override fun createIntent(ctx: Context, input: CaptureConfig) =
        Intent(ctx, OcrActivity::class.java)
            .setAction(OcrHelper.Actions.CAPTURE_URI)
            .putExtra(OcrHelper.Extras.CONFIG, input)

    override fun parseResult(resultCode: Int, intent: Intent?): UriDataResult? =
        if (resultCode == Activity.RESULT_OK)
            intent?.parcelable(OcrHelper.Extras.RESULT)
        else null
}
    class OCRContract :
        ActivityResultContract<OcrConfig, OcrDataResult?>() {

        override fun createIntent(context: Context, input: OcrConfig) =
            Intent(context, OcrActivity::class.java)
                .setAction(OcrHelper.Actions.OCR_RETURN_DATA)
                .putExtra(OcrHelper.Extras.CONFIG, input)

        override fun parseResult(resultCode: Int, intent: Intent?): OcrDataResult? =
            if (resultCode == Activity.RESULT_OK)
                intent?.parcelable(OcrHelper.Extras.RESULT)
            else null
    }
