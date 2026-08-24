package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureDao {

    @Query("SELECT * FROM saved_lectures ORDER BY savedAt DESC")
    fun getAllSavedLectures(): Flow<List<SavedLectureEntity>>

    @Query("SELECT * FROM saved_lectures WHERE lastWatchedAt > 0 ORDER BY lastWatchedAt DESC LIMIT 10")
    fun getRecentLectures(): Flow<List<SavedLectureEntity>>

    @Query("SELECT * FROM saved_lectures WHERE videoId = :videoId LIMIT 1")
    fun getLectureFlow(videoId: String): Flow<SavedLectureEntity?>

    @Query("SELECT * FROM saved_lectures WHERE videoId = :videoId LIMIT 1")
    suspend fun getLecture(videoId: String): SavedLectureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecture(lecture: SavedLectureEntity)

    @Update
    suspend fun updateLecture(lecture: SavedLectureEntity)

    @Query("DELETE FROM saved_lectures WHERE videoId = :videoId")
    suspend fun deleteLecture(videoId: String)

    @Query("""
        UPDATE saved_lectures 
        SET progressSeconds = :progress, 
            totalSeconds = :total, 
            lastWatchedAt = :lastWatched,
            isCompleted = :completed 
        WHERE videoId = :videoId
    """)
    suspend fun updateProgress(
        videoId: String,
        progress: Int,
        total: Int,
        lastWatched: Long,
        completed: Boolean
    )

    @Query("UPDATE saved_lectures SET notes = :notes WHERE videoId = :videoId")
    suspend fun updateNotes(videoId: String, notes: String)

    @Query("DELETE FROM saved_lectures")
    suspend fun clearAll()
}
