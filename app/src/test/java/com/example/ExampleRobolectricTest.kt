package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserPreferencesRepository
import com.example.data.remote.ChapterParser
import com.example.data.remote.DurationParser
import com.example.data.repository.ApiKeyProvider
import com.example.data.repository.ApiKeySource
import com.example.data.repository.EducationalVideoCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FocusTube", appName)
    }

    @Test
    fun `search returns educational results for mathematics`() {
        val results = EducationalVideoCatalog.search("Linear Algebra")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.title.contains("Linear Algebra", ignoreCase = true) })
    }

    @Test
    fun `extract video ID from youtube url`() {
        val url = "https://www.youtube.com/watch?v=fNk_zzaMoSs"
        val id = EducationalVideoCatalog.extractVideoId(url)
        assertEquals("fNk_zzaMoSs", id)
    }

    @Test
    fun `iso duration parser formats correctly`() {
        assertEquals("1:25:30", DurationParser.parseIsoDurationToDisplay("PT1H25M30S"))
        assertEquals("14:15", DurationParser.parseIsoDurationToDisplay("PT14M15S"))
        assertEquals("0:45", DurationParser.parseIsoDurationToDisplay("PT45S"))
        assertEquals(5130, DurationParser.parseIsoDurationToSeconds("PT1H25M30S"))
    }

    @Test
    fun `chapter parser parses timestamps accurately`() {
        val desc = """
            00:00 Introduction
            03:15 Matrix Multiplication
            1:15:30 Final Exam Review
        """.trimIndent()
        val chapters = ChapterParser.parseChapters(desc)
        assertEquals(3, chapters.size)
        assertEquals(0, chapters[0].startSeconds)
        assertEquals(195, chapters[1].startSeconds)
        assertEquals("Matrix Multiplication", chapters[1].title)
        assertEquals(4530, chapters[2].startSeconds)
    }

    @Test
    fun `custom api key can be stored and retrieved`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = UserPreferencesRepository(context)
        val apiKeyProvider = ApiKeyProvider(prefsRepo)

        apiKeyProvider.setCustomApiKey("AIzaSyTest1234567890")
        val status = apiKeyProvider.getApiKeyStatus()

        assertEquals(ApiKeySource.IN_APP_SETTINGS, status.source)
        assertEquals("AIzaSyTest1234567890", status.activeKey)
        assertTrue(status.isCustomKeySet)

        apiKeyProvider.clearCustomApiKey()
        val clearedStatus = apiKeyProvider.getApiKeyStatus()
        assertEquals(false, clearedStatus.isCustomKeySet)
    }

    @Test
    fun `open in youtube default preference is false by default and mutable`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = UserPreferencesRepository(context)
        prefsRepo.clearAllPreferences()

        assertEquals(false, prefsRepo.getOpenInYouTubeDefault())

        prefsRepo.setOpenInYouTubeDefault(true)
        assertTrue(prefsRepo.getOpenInYouTubeDefault())

        prefsRepo.setOpenInYouTubeDefault(false)
        assertEquals(false, prefsRepo.getOpenInYouTubeDefault())
    }
}
