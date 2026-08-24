package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.UserPreferencesRepository

enum class ApiKeySource {
    IN_APP_SETTINGS,
    ENVIRONMENT_CONFIG,
    NONE
}

data class ApiKeyStatus(
    val activeKey: String,
    val source: ApiKeySource,
    val isCustomKeySet: Boolean,
    val maskedDisplay: String
)

class ApiKeyProvider(private val preferencesRepository: UserPreferencesRepository) {

    fun getEffectiveApiKey(): String {
        val userCustomKey = preferencesRepository.getCustomApiKey().trim()
        if (userCustomKey.isNotBlank()) {
            return userCustomKey
        }
        val buildConfigKey = try {
            BuildConfig.YOUTUBE_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
        if (buildConfigKey.isNotBlank() && !buildConfigKey.contains("MY_YOUTUBE_API_KEY")) {
            return buildConfigKey
        }
        return ""
    }

    fun getApiKeyStatus(): ApiKeyStatus {
        val userCustomKey = preferencesRepository.getCustomApiKey().trim()
        val buildConfigKey = try {
            BuildConfig.YOUTUBE_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }

        val hasValidBuildConfig = buildConfigKey.isNotBlank() && !buildConfigKey.contains("MY_YOUTUBE_API_KEY")

        return when {
            userCustomKey.isNotBlank() -> {
                ApiKeyStatus(
                    activeKey = userCustomKey,
                    source = ApiKeySource.IN_APP_SETTINGS,
                    isCustomKeySet = true,
                    maskedDisplay = maskKey(userCustomKey)
                )
            }
            hasValidBuildConfig -> {
                ApiKeyStatus(
                    activeKey = buildConfigKey,
                    source = ApiKeySource.ENVIRONMENT_CONFIG,
                    isCustomKeySet = false,
                    maskedDisplay = maskKey(buildConfigKey)
                )
            }
            else -> {
                ApiKeyStatus(
                    activeKey = "",
                    source = ApiKeySource.NONE,
                    isCustomKeySet = false,
                    maskedDisplay = "No Key Configured"
                )
            }
        }
    }

    fun setCustomApiKey(key: String) {
        preferencesRepository.setCustomApiKey(key.trim())
    }

    fun clearCustomApiKey() {
        preferencesRepository.setCustomApiKey("")
    }

    private fun maskKey(key: String): String {
        if (key.length <= 8) return "••••••••"
        val start = key.take(4)
        val end = key.takeLast(4)
        return "$start••••••••$end"
    }
}
