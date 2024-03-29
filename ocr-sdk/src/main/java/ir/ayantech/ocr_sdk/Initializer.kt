package ir.ayantech.ocr_sdk

import android.content.Context
import ir.ayantech.ocr_sdk.Constant.Base_URL
import ir.ayantech.ocr_sdk.Constant.EndPoint_GetCardOcrResult
import ir.ayantech.ocr_sdk.Constant.EndPoint_UploadCardOCR
import ir.ayantech.ocr_sdk.Constant.Token

class OcrInitializer {
    companion object {
        var fileProviderAuthority: String? = null
    }

    fun initializeFileProvider(authority: String) {
        fileProviderAuthority = authority
    }

    @Deprecated(
        "This method deprecated.",
        ReplaceWith("OCRConfig.builder()"),
        DeprecationLevel.ERROR
    )
    fun setConfig(
        token: String,
        baseUrl: String,
        uploadImageEndPoint: String,
        getResultEndPoint: String,
        ocrContext: Context
    ) {
        Constant.context = ocrContext
        Token = token
        Base_URL = baseUrl
        EndPoint_UploadCardOCR = uploadImageEndPoint
        EndPoint_GetCardOcrResult = getResultEndPoint

    }
}