package com.ispecs.parent.model

import android.os.Parcel
import android.os.Parcelable

data class LogEntry(
    val battery: Int,
    val status: Int,
    val time: String?,
    val additionalData: Map<String, Any?>
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString(),
        mutableMapOf<String, Any?>().apply {
            val size = parcel.readInt()
            repeat(size) {
                val key = parcel.readString()
                val value = parcel.readValue(Any::class.java.classLoader)
                if (key != null) put(key, value)
            }
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(battery)
        parcel.writeInt(status)
        parcel.writeString(time)
        parcel.writeInt(additionalData.size)
        additionalData.forEach { (key, value) ->
            parcel.writeString(key)
            parcel.writeValue(value)
        }
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<LogEntry> {
        override fun createFromParcel(parcel: Parcel) = LogEntry(parcel)
        override fun newArray(size: Int): Array<LogEntry?> = arrayOfNulls(size)
    }
}
