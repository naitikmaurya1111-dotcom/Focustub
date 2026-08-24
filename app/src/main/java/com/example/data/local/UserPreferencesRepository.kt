package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

class UserPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("focustube_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_YOUTUBE_API_KEY = "custom_youtube_api_key"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_DEFAULT_TIMER_MINUTES = "default_timer_minutes"
        private const val KEY_OPEN_IN_YOUTUBE_DEFAULT = "open_in_youtube_default"
        private const val KEY_SEARCH_HISTORY = "search_history_json"
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

    fun getSearchHistory(): List<String> {
        val jsonStr = prefs.getString(KEY_SEARCH_HISTORY, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optString(i)
                if (!item.isNullOrBlank()) {
                    list.add(item)
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSearchQuery(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        val current = getSearchHistory().toMutableList()
        current.remove(clean) // remove old position
        current.add(0, clean) // add to top
        val trimmed = current.take(15) // limit to last 15
        val jsonArray = JSONArray()
        trimmed.forEach { jsonArray.put(it) }
        prefs.edit().putString(KEY_SEARCH_HISTORY, jsonArray.toString()).apply()
    }

    fun deleteSearchQuery(query: String) {
        val clean = query.trim()
        val current = getSearchHistory().toMutableList()
        current.remove(clean)
        val jsonArray = JSONArray()
        current.forEach { jsonArray.put(it) }
        prefs.edit().putString(KEY_SEARCH_HISTORY, jsonArray.toString()).apply()
    }

    fun clearSearchHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply()
    }

    fun clearAllPreferences() {
        prefs.edit().clear().apply()
    }
}
