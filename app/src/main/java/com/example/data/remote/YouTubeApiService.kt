package com.example.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {

    @GET("youtube/v3/search")
    suspend fun searchVideos(
        @Query("q") query: String,
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 30
    ): Response<YouTubeSearchResponse>

    @GET("youtube/v3/videos")
    suspend fun getVideoDetails(
        @Query("id") ids: String,
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet,contentDetails"
    ): Response<YouTubeVideoListResponse>
}
