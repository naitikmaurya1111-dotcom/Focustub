package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponse(
    @field:Json(name = "items") val items: List<YouTubeSearchResultItem>? = null,
    @field:Json(name = "nextPageToken") val nextPageToken: String? = null,
    @field:Json(name = "error") val error: YouTubeApiError? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchResultItem(
    @field:Json(name = "id") val id: YouTubeId? = null,
    @field:Json(name = "snippet") val snippet: YouTubeSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeId(
    @field:Json(name = "kind") val kind: String? = null,
    @field:Json(name = "videoId") val videoId: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSnippet(
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "description") val description: String? = null,
    @field:Json(name = "channelTitle") val channelTitle: String? = null,
    @field:Json(name = "thumbnails") val thumbnails: YouTubeThumbnails? = null,
    @field:Json(name = "publishedAt") val publishedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnails(
    @field:Json(name = "default") val default: YouTubeThumbnail? = null,
    @field:Json(name = "medium") val medium: YouTubeThumbnail? = null,
    @field:Json(name = "high") val high: YouTubeThumbnail? = null,
    @field:Json(name = "standard") val standard: YouTubeThumbnail? = null,
    @field:Json(name = "maxres") val maxres: YouTubeThumbnail? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnail(
    @field:Json(name = "url") val url: String? = null,
    @field:Json(name = "width") val width: Int? = null,
    @field:Json(name = "height") val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoListResponse(
    @field:Json(name = "items") val items: List<YouTubeVideoItem>? = null,
    @field:Json(name = "error") val error: YouTubeApiError? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoItem(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "snippet") val snippet: YouTubeSnippet? = null,
    @field:Json(name = "contentDetails") val contentDetails: YouTubeContentDetails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeContentDetails(
    @field:Json(name = "duration") val duration: String? = null,
    @field:Json(name = "dimension") val dimension: String? = null,
    @field:Json(name = "definition") val definition: String? = null,
    @field:Json(name = "caption") val caption: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeApiError(
    @field:Json(name = "code") val code: Int? = null,
    @field:Json(name = "message") val message: String? = null
)
