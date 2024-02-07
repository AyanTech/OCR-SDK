package ir.ayantech.ocr_sdk

import android.annotation.SuppressLint
import android.content.Context
import ir.ayantech.whygoogle.helper.PreferencesManager

@SuppressLint("StaticFieldLeak")
object Constant {
        lateinit var context: Context
         var EndPoint_UploadCardOCR: String
                get() = PreferencesManager.getInstance(context).read("EndPoint_UploadCardOCR")
                set(value) = PreferencesManager.getInstance(context)
                        .save("EndPoint_UploadCardOCR", value)

        var EndPoint_GetCardOcrResult: String
                get() = PreferencesManager.getInstance(context).read("EndPoint_GetCardOcrResult")
                set(value) = PreferencesManager.getInstance(context)
                        .save("EndPoint_GetCardOcrResult", value)

        var Token: String
                get() = PreferencesManager.getInstance(context).read("Token")
                set(value) = PreferencesManager.getInstance(context).save("Token", value)

        var Base_URL: String
                get() = PreferencesManager.getInstance(context).read("Base_URL")
                set(value) = PreferencesManager.getInstance(context).save("Base_URL", value)


}