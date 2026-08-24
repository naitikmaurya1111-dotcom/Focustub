package com.example.data.model

data class Lecture(
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val duration: String,
    val category: String = "General",
    val description: String = "",
    val isSaved: Boolean = false,
    val progressSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val isCompleted: Boolean = false,
    val notes: String = "",
    val lastWatchedAt: Long = 0L
) {
    val progressPercent: Float
        get() = if (totalSeconds > 0) (progressSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f) else 0f
}
