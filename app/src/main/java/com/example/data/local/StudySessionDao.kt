package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity)

    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC LIMIT 20")
    fun getRecentSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT SUM(durationMinutes) FROM study_sessions")
    fun getTotalMinutesStudied(): Flow<Int?>

    @Query("DELETE FROM study_sessions")
    suspend fun clearAllSessions()
}
