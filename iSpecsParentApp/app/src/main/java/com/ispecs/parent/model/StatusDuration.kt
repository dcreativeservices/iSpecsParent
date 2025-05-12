package com.ispecs.parent.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StatusDuration(
    val state: String, // "On" or "Off"
    val startTime: String?,
    val endTime: String?,
    val durationInSeconds: Long
) : Parcelable