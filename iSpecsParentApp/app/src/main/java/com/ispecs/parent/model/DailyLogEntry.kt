package com.ispecs.parent.model

data class DailyLogEntry(
    val startTime: String,
    val endTime: String,
    val duration: String,
    val status: String,
    val date: String // <-- Add this line
)
