package ir.ayantech.ocr_sdk.model


class UploadNewCardOcrImage {
    data class Input(
        val ImageArray: List<String>,
        val Type : String
    )

    data class Output(
        val FileID: String,

    )

}