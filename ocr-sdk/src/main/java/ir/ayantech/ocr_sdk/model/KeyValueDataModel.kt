package ir.ayantech.ocr_sdk.model

import android.os.Parcel
import android.os.Parcelable

data class KeyValueDataModel(
    val key: String?,
    val value: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(key)
        parcel.writeString(value)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<KeyValueDataModel> {
        override fun createFromParcel(parcel: Parcel): KeyValueDataModel {
            return KeyValueDataModel(parcel)
        }

        override fun newArray(size: Int): Array<KeyValueDataModel?> {
            return arrayOfNulls(size)
        }
    }
}