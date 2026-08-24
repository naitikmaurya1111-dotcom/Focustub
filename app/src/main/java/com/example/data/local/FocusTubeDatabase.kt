package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedLectureEntity::class, StudySessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FocusTubeDatabase : RoomDatabase() {

    abstract fun lectureDao(): LectureDao
    abstract fun studySessionDao(): StudySessionDao

    companion object {
        @Volatile
        private var INSTANCE: FocusTubeDatabase? = null

        fun getInstance(context: Context): FocusTubeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusTubeDatabase::class.java,
                    "focustube_database"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
