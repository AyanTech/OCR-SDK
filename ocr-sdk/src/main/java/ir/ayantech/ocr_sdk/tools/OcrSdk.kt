package ir.ayantech.ocr_sdk.tools

import android.content.Context
import ir.ayantech.ocr_sdk.di.ocrModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.GlobalContext

object OcrSdk {
    fun init(context: Context) {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(context.applicationContext)
                modules(ocrModule)
            }
        }
    }
}
