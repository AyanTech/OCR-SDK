package ir.ayantech.ocr_sdk.tools

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import ir.ayantech.ocr_sdk.data.model.OcrSdkCaptureConfig
import ir.ayantech.ocr_sdk.data.model.OcrSdkOcrConfig
import ir.ayantech.ocr_sdk.data.model.OcrSdkOcrDataResult
import ir.ayantech.ocr_sdk.data.model.OcrSdkUriDataResult
import ir.ayantech.ocr_sdk.ui.activity.OcrActivity
import kotlinx.serialization.json.Json

// URI result
class CaptureContract :
    ActivityResultContract<OcrSdkCaptureConfig, OcrSdkUriDataResult?>() {

    override fun createIntent(context: Context, input: OcrSdkCaptureConfig) =
        Intent(context, OcrActivity::class.java)
            .setAction(OcrHelper.Actions.CAPTURE_URI)
            .putExtra(OcrHelper.Extras.CONFIG, input)

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

    override fun parseResult(resultCode: Int, intent: Intent?): OcrSdkOcrDataResult? {
        return if (resultCode == Activity.RESULT_OK) {
            intent?.getStringExtra(OcrHelper.Extras.RESULT)?.let {
                try {
                    Json.decodeFromString<OcrSdkOcrDataResult>(it)
                } catch (e: Exception) {
                    null
                }
            }
        } else null
    }
}
