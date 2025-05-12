package com.ispecs.parent.model

import android.os.Parcel
import android.os.Parcelable

data class LogEntry(
    val battery: Int,
    val status: Int,
    val time: String?,
    val additionalData: Map<String, Any?> // New field to hold all other values
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString(),
        mutableMapOf<String, Any?>().apply {
            val size = parcel.readInt()
            for (i in 0 until size) {
                val key = parcel.readString()
                val value = parcel.readValue(Any::class.java.classLoader)
                key?.let { put(it, value) }
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

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LogEntry> {
        override fun createFromParcel(parcel: Parcel): LogEntry {
            return LogEntry(parcel)
        }

        override fun newArray(size: Int): Array<LogEntry?> {
            return arrayOfNulls(size)
        }
    }
}


