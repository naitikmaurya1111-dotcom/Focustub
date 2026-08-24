package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val videoId: String,
    val videoTitle: String,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)
