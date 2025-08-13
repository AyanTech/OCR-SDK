package ir.ayantech.ocr_sdk.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UriDataResult(
    val uri: Uri?,
    val extraInfo: String? = null
) : Parcelable