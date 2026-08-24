package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class UserPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("focustube_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_YOUTUBE_API_KEY = "custom_youtube_api_key"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_DEFAULT_TIMER_MINUTES = "default_timer_minutes"
        private const val KEY_OPEN_IN_YOUTUBE_DEFAULT = "open_in_youtube_default"
    }

    fun getOpenInYouTubeDefault(): Boolean {
        return prefs.getBoolean(KEY_OPEN_IN_YOUTUBE_DEFAULT, false)
    }

    fun setOpenInYouTubeDefault(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OPEN_IN_YOUTUBE_DEFAULT, enabled).apply()
    }

    fun getCustomApiKey(): String {
        return prefs.getString(KEY_CUSTOM_YOUTUBE_API_KEY, "") ?: ""
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM_YOUTUBE_API_KEY, key.trim()).apply()
    }

    fun getKeepScreenOn(): Boolean {
        return prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
    }

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
    }

    fun getDefaultTimerMinutes(): Int {
        return prefs.getInt(KEY_DEFAULT_TIMER_MINUTES, 25)
    }

    fun setDefaultTimerMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DEFAULT_TIMER_MINUTES, minutes).apply()
    }

    fun clearAllPreferences() {
        prefs.edit().clear().apply()
    }
}
