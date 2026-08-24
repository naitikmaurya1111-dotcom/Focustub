package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_lectures")
data class SavedLectureEntity(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val duration: String,
    val category: String = "General",
    val description: String = "",
    val savedAt: Long = System.currentTimeMillis(),
    val lastWatchedAt: Long = 0L,
    val progressSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val isCompleted: Boolean = false,
    val notes: String = ""
)
