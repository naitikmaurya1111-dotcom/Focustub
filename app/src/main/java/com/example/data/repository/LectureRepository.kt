package com.example.data.repository

import com.example.data.local.LectureDao
import com.example.data.local.SavedLectureEntity
import com.example.data.local.StudySessionDao
import com.example.data.local.StudySessionEntity
import com.example.data.model.Lecture
import com.example.data.remote.YouTubeApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SearchResultPayload(
    val lectures: List<Lecture>,
    val isLiveApiResult: Boolean,
    val errorMessage: String? = null
)

class LectureRepository(
    private val lectureDao: LectureDao,
    private val studySessionDao: StudySessionDao
) {

    val savedLectures: Flow<List<Lecture>> = lectureDao.getAllSavedLectures().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val recentLectures: Flow<List<Lecture>> = lectureDao.getRecentLectures().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val totalMinutesStudied: Flow<Int> = studySessionDao.getTotalMinutesStudied().map { it ?: 0 }

    fun getLectureFlow(videoId: String): Flow<Lecture?> = lectureDao.getLectureFlow(videoId).map {
        it?.toDomainModel() ?: EducationalVideoCatalog.CURATED_LECTURES.find { l -> l.videoId == videoId }
    }

    suspend fun getLecture(videoId: String, apiKey: String = ""): Lecture? {
        val entity = lectureDao.getLecture(videoId)
        if (entity != null) return entity.toDomainModel()

        val catalogMatch = EducationalVideoCatalog.CURATED_LECTURES.find { it.videoId == videoId }
        if (catalogMatch != null) return catalogMatch

        if (apiKey.isNotBlank()) {
            val liveResult = YouTubeApiClient.fetchVideoDetails(videoId, apiKey)
            if (liveResult.isSuccess && liveResult.getOrNull() != null) {
                return liveResult.getOrNull()
            }
        }

        return EducationalVideoCatalog.extractVideoId(videoId)?.let { extractedId ->
            Lecture(
                videoId = extractedId,
                title = "Study Lecture ($extractedId)",
                channelTitle = "Direct Study Link",
                thumbnailUrl = "https://img.youtube.com/vi/$extractedId/hqdefault.jpg",
                duration = "Lecture",
                category = "academic",
                description = "Direct study lecture."
            )
        }
    }

    suspend fun search(
        query: String,
        categoryId: String,
        apiKey: String
    ): SearchResultPayload {
        val cleanQuery = query.trim()
        val extractedId = EducationalVideoCatalog.extractVideoId(cleanQuery)

        // 1. If direct video ID or URL
        if (extractedId != null) {
            if (apiKey.isNotBlank()) {
                val liveResult = YouTubeApiClient.fetchVideoDetails(extractedId, apiKey)
                if (liveResult.isSuccess && liveResult.getOrNull() != null) {
                    return SearchResultPayload(
                        lectures = listOf(liveResult.getOrNull()!!),
                        isLiveApiResult = true
                    )
                }
            }
            val localResults = EducationalVideoCatalog.search(cleanQuery, categoryId)
            return SearchResultPayload(
                lectures = localResults,
                isLiveApiResult = false
            )
        }

        // 2. If live API key is available and search query is present
        if (apiKey.isNotBlank() && cleanQuery.isNotBlank()) {
            val categoryModifier = when (categoryId) {
                "cs" -> " computer science programming"
                "math" -> " mathematics calculus linear algebra"
                "physics" -> " physics mechanics university"
                "chemistry" -> " chemistry organic chemistry"
                "biology" -> " biology neuroscience"
                "philosophy" -> " philosophy ethics"
                "economics" -> " economics macroeconomics"
                else -> " university lecture course"
            }

            val fullQuery = cleanQuery + categoryModifier
            val liveResult = YouTubeApiClient.searchVideos(fullQuery, apiKey, categoryId)

            if (liveResult.isSuccess) {
                val list = liveResult.getOrDefault(emptyList())
                if (list.isNotEmpty()) {
                    return SearchResultPayload(
                        lectures = list,
                        isLiveApiResult = true
                    )
                }
            } else {
                val err = liveResult.exceptionOrNull()?.localizedMessage ?: "Live search error"
                val fallbackList = EducationalVideoCatalog.search(cleanQuery, categoryId)
                return SearchResultPayload(
                    lectures = fallbackList,
                    isLiveApiResult = false,
                    errorMessage = err
                )
            }
        }

        // 3. Fallback to curated catalog
        val fallbackList = EducationalVideoCatalog.search(cleanQuery, categoryId)
        return SearchResultPayload(
            lectures = fallbackList,
            isLiveApiResult = false
        )
    }

    suspend fun saveLecture(lecture: Lecture) {
        val entity = lecture.toEntity(isSaved = true)
        lectureDao.insertLecture(entity)
    }

    suspend fun removeLecture(videoId: String) {
        lectureDao.deleteLecture(videoId)
    }

    suspend fun updateProgress(
        videoId: String,
        progressSeconds: Int,
        totalSeconds: Int,
        isCompleted: Boolean
    ) {
        val existing = lectureDao.getLecture(videoId)
        if (existing != null) {
            lectureDao.updateProgress(
                videoId = videoId,
                progress = progressSeconds,
                total = totalSeconds,
                lastWatched = System.currentTimeMillis(),
                completed = isCompleted
            )
        } else {
            val catalogMatch = EducationalVideoCatalog.CURATED_LECTURES.find { it.videoId == videoId }
            val newEntity = SavedLectureEntity(
                videoId = videoId,
                title = catalogMatch?.title ?: "Study Lecture",
                channelTitle = catalogMatch?.channelTitle ?: "Educational Channel",
                thumbnailUrl = catalogMatch?.thumbnailUrl ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                duration = catalogMatch?.duration ?: "Lecture",
                category = catalogMatch?.category ?: "general",
                description = catalogMatch?.description ?: "",
                savedAt = System.currentTimeMillis(),
                lastWatchedAt = System.currentTimeMillis(),
                progressSeconds = progressSeconds,
                totalSeconds = totalSeconds,
                isCompleted = isCompleted
            )
            lectureDao.insertLecture(newEntity)
        }
    }

    suspend fun saveNotes(videoId: String, notes: String) {
        val existing = lectureDao.getLecture(videoId)
        if (existing != null) {
            lectureDao.updateNotes(videoId, notes)
        } else {
            val catalogMatch = EducationalVideoCatalog.CURATED_LECTURES.find { it.videoId == videoId }
            val newEntity = SavedLectureEntity(
                videoId = videoId,
                title = catalogMatch?.title ?: "Study Lecture",
                channelTitle = catalogMatch?.channelTitle ?: "Educational Channel",
                thumbnailUrl = catalogMatch?.thumbnailUrl ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                duration = catalogMatch?.duration ?: "Lecture",
                category = catalogMatch?.category ?: "general",
                savedAt = System.currentTimeMillis(),
                notes = notes
            )
            lectureDao.insertLecture(newEntity)
        }
    }

    suspend fun recordStudySession(videoId: String, videoTitle: String, minutes: Int) {
        if (minutes > 0) {
            studySessionDao.insertSession(
                StudySessionEntity(
                    videoId = videoId,
                    videoTitle = videoTitle,
                    durationMinutes = minutes,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun clearAllData() {
        lectureDao.clearAll()
        studySessionDao.clearAllSessions()
    }

    private fun SavedLectureEntity.toDomainModel(): Lecture {
        return Lecture(
            videoId = videoId,
            title = title,
            channelTitle = channelTitle,
            thumbnailUrl = thumbnailUrl,
            duration = duration,
            category = category,
            description = description,
            isSaved = true,
            progressSeconds = progressSeconds,
            totalSeconds = totalSeconds,
            isCompleted = isCompleted,
            notes = notes,
            lastWatchedAt = lastWatchedAt
        )
    }

    private fun Lecture.toEntity(isSaved: Boolean): SavedLectureEntity {
        return SavedLectureEntity(
            videoId = videoId,
            title = title,
            channelTitle = channelTitle,
            thumbnailUrl = thumbnailUrl,
            duration = duration,
            category = category,
            description = description,
            savedAt = System.currentTimeMillis(),
            lastWatchedAt = lastWatchedAt,
            progressSeconds = progressSeconds,
            totalSeconds = totalSeconds,
            isCompleted = isCompleted,
            notes = notes
        )
    }
}

