package ir.ayantech.ocr_sdk.model

import android.os.Parcel
import android.os.Parcelable

class GetCardOcrResult {
    data class Input(
        val FileID: String,

    )

     data class Output(
         val Result: List<Result>?,
        val CardID: String,
        val Status: String,
        val NextCallInterval: Long,
    )

    data class Result(
        val Key: String?,
        val Value: String?
    ):Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString()
        ) {
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(Key)
            parcel.writeString(Value)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Result> {
            override fun createFromParcel(parcel: Parcel): Result {
                return Result(parcel)
            }

            override fun newArray(size: Int): Array<Result?> {
                return arrayOfNulls(size)
            }
        }
    }
}