package com.example.data.remote

import android.os.Build
import android.text.Html
import com.example.data.model.Lecture
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object YouTubeApiClient {

    private const val BASE_URL = "https://www.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: YouTubeApiService = retrofit.create(YouTubeApiService::class.java)

    /**
     * Decodes HTML entities commonly returned by YouTube API (e.g. &amp;, &#39;, &quot;)
     */
    fun decodeHtml(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(input, Html.FROM_HTML_MODE_LEGACY).toString()
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(input).toString()
            }
        } catch (_: Exception) {
            input
        }
    }

    /**
     * Searches educational YouTube videos with the provided API key.
     * Fetches video duration in a secondary batch call to `videos` endpoint.
     */
    suspend fun searchVideos(
        query: String,
        apiKey: String,
        categoryId: String = "all"
    ): Result<List<Lecture>> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("YouTube API key is blank"))
        }

        return try {
            // Append educational keywords if query is brief to focus on educational content
            val educationalQuery = if (query.isBlank()) {
                "lecture full course academic"
            } else {
                query
            }

            val searchResponse = apiService.searchVideos(
                query = educationalQuery,
                apiKey = apiKey,
                maxResults = 25
            )

            if (!searchResponse.isSuccessful) {
                val errorBody = searchResponse.errorBody()?.string() ?: ""
                return Result.failure(Exception("YouTube API Error ${searchResponse.code()}: $errorBody"))
            }

            val items = searchResponse.body()?.items ?: emptyList()
            val videoIds = items.mapNotNull { it.id?.videoId }.filter { it.isNotBlank() }

            if (videoIds.isEmpty()) {
                return Result.success(emptyList())
            }

            // Fetch video details (durations) for all returned IDs in a single batch
            val detailsMap = try {
                val detailsResponse = apiService.getVideoDetails(
                    ids = videoIds.joinToString(","),
                    apiKey = apiKey
                )
                if (detailsResponse.isSuccessful) {
                    detailsResponse.body()?.items?.associateBy { it.id } ?: emptyMap()
                } else {
                    emptyMap()
                }
            } catch (_: Exception) {
                emptyMap()
            }

            val lectures = items.mapNotNull { item ->
                val videoId = item.id?.videoId ?: return@mapNotNull null
                val snippet = item.snippet
                val detail = detailsMap[videoId]

                val title = decodeHtml(snippet?.title ?: "Educational Lecture")
                val channelTitle = decodeHtml(snippet?.channelTitle ?: "Academic Channel")
                val description = decodeHtml(snippet?.description ?: "")
                val thumbnailUrl = snippet?.thumbnails?.high?.url
                    ?: snippet?.thumbnails?.medium?.url
                    ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                val isoDuration = detail?.contentDetails?.duration
                val displayDuration = DurationParser.parseIsoDurationToDisplay(isoDuration)
                val totalSeconds = DurationParser.parseIsoDurationToSeconds(isoDuration)

                Lecture(
                    videoId = videoId,
                    title = title,
                    channelTitle = channelTitle,
                    thumbnailUrl = thumbnailUrl,
                    duration = displayDuration,
                    category = if (categoryId != "all") categoryId else "academic",
                    description = description,
                    totalSeconds = totalSeconds
                )
            }

            Result.success(lectures)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches metadata for a single video ID from YouTube API
     */
    suspend fun fetchVideoDetails(videoId: String, apiKey: String): Result<Lecture?> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("YouTube API key is blank"))
        }

        return try {
            val response = apiService.getVideoDetails(ids = videoId, apiKey = apiKey)
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code()}"))
            }

            val item = response.body()?.items?.firstOrNull()
            if (item == null) {
                return Result.success(null)
            }

            val snippet = item.snippet
            val title = decodeHtml(snippet?.title ?: "Study Lecture")
            val channelTitle = decodeHtml(snippet?.channelTitle ?: "Educational Channel")
            val description = decodeHtml(snippet?.description ?: "")
            val thumbnailUrl = snippet?.thumbnails?.high?.url
                ?: snippet?.thumbnails?.medium?.url
                ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

            val isoDuration = item.contentDetails?.duration
            val displayDuration = DurationParser.parseIsoDurationToDisplay(isoDuration)
            val totalSeconds = DurationParser.parseIsoDurationToSeconds(isoDuration)

            Result.success(
                Lecture(
                    videoId = videoId,
                    title = title,
                    channelTitle = channelTitle,
                    thumbnailUrl = thumbnailUrl,
                    duration = displayDuration,
                    category = "academic",
                    description = description,
                    totalSeconds = totalSeconds
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validates if a provided API key is active by issuing a minimal test search
     */
    suspend fun validateApiKey(apiKey: String): Result<Boolean> {
        if (apiKey.isBlank()) return Result.failure(IllegalArgumentException("Key is empty"))
        return try {
            val response = apiService.searchVideos(
                query = "math",
                apiKey = apiKey,
                maxResults = 1
            )
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val code = response.code()
                val message = when (code) {
                    400 -> "Invalid API Key format (400)"
                    403 -> "API Key quota exceeded or YouTube Data API v3 service not enabled in Google Cloud Console (403)"
                    else -> "API Error (Code $code)"
                }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
