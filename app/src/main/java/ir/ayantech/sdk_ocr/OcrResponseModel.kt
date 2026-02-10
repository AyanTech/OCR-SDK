package ir.ayantech.sdk_ocr


data class OcrResponseModel(
    var statusCode: String? = null,
    var description: String? = null,
    var timeSpent: String? = null,
    val ocrRowModelList: MutableList<OcrRowModel>? = mutableListOf()
)