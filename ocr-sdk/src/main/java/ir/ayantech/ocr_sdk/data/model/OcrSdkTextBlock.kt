package ir.ayantech.ocr_sdk.data.model

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class OcrSdkTextBlock(
    @StringRes val title: Int? = null,
    @StringRes val firstImageHolderText: Int? = null,
    @StringRes val secondImageHolderText: Int? = null,
    @StringRes val buttonText: Int? = null,
    @StringRes val secondTitle: Int? = null
) : Parcelable
