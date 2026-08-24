package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FocusTubeDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.Lecture
import com.example.data.remote.YouTubeApiClient
import com.example.data.repository.ApiKeyProvider
import com.example.data.repository.ApiKeyStatus
import com.example.data.repository.EducationalVideoCatalog
import com.example.data.repository.LectureRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FocusScreen {
    SEARCH,
    LIBRARY,
    PLAYER,
    SETTINGS
}

data class StudyTimerState(
    val initialMinutes: Int = 25,
    val secondsRemaining: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
) {
    val formattedTime: String
        get() {
            val m = secondsRemaining / 60
            val s = secondsRemaining % 60
            return String.format("%02d:%02d", m, s)
        }
}

class FocusTubeViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    val apiKeyProvider = ApiKeyProvider(userPreferencesRepository)
    private val repository: LectureRepository

    init {
        val db = FocusTubeDatabase.getInstance(application)
        repository = LectureRepository(db.lectureDao(), db.studySessionDao())
    }

    // Navigation & View State
    private val _currentScreen = MutableStateFlow(FocusScreen.SEARCH)
    val currentScreen: StateFlow<FocusScreen> = _currentScreen.asStateFlow()

    // API Key State
    private val _apiKeyStatus = MutableStateFlow(apiKeyProvider.getApiKeyStatus())
    val apiKeyStatus: StateFlow<ApiKeyStatus> = _apiKeyStatus.asStateFlow()

    private val _isTestingApiKey = MutableStateFlow(false)
    val isTestingApiKey: StateFlow<Boolean> = _isTestingApiKey.asStateFlow()

    private val _apiKeyValidationResult = MutableStateFlow<String?>(null)
    val apiKeyValidationResult: StateFlow<String?> = _apiKeyValidationResult.asStateFlow()

    private val _isLiveApiSearch = MutableStateFlow(false)
    val isLiveApiSearch: StateFlow<Boolean> = _isLiveApiSearch.asStateFlow()

    private val _searchApiErrorMessage = MutableStateFlow<String?>(null)
    val searchApiErrorMessage: StateFlow<String?> = _searchApiErrorMessage.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Search results combined with saved status
    val savedLectures: StateFlow<List<Lecture>> = repository.savedLectures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLectures: StateFlow<List<Lecture>> = repository.recentLectures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMinutesStudied: StateFlow<Int> = repository.totalMinutesStudied
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _searchResults = MutableStateFlow<List<Lecture>>(
        EducationalVideoCatalog.CURATED_LECTURES
    )

    val displayedSearchResults: StateFlow<List<Lecture>> = combine(
        _searchResults,
        savedLectures
    ) { results, savedList ->
        val savedIds = savedList.associateBy { it.videoId }
        results.map { lecture ->
            val saved = savedIds[lecture.videoId]
            if (saved != null) {
                lecture.copy(
                    isSaved = true,
                    progressSeconds = saved.progressSeconds,
                    totalSeconds = saved.totalSeconds,
                    isCompleted = saved.isCompleted,
                    notes = saved.notes
                )
            } else {
                lecture.copy(isSaved = false)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EducationalVideoCatalog.CURATED_LECTURES)

    // Player State
    private val _currentLecture = MutableStateFlow<Lecture?>(null)
    val currentLecture: StateFlow<Lecture?> = _currentLecture.asStateFlow()

    private val _isFocusMode = MutableStateFlow(false)
    val isFocusMode: StateFlow<Boolean> = _isFocusMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(userPreferencesRepository.getKeepScreenOn())
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _openInYouTubeDefault = MutableStateFlow(userPreferencesRepository.getOpenInYouTubeDefault())
    val openInYouTubeDefault: StateFlow<Boolean> = _openInYouTubeDefault.asStateFlow()

    // Notes State
    private val _currentNotes = MutableStateFlow("")
    val currentNotes: StateFlow<String> = _currentNotes.asStateFlow()

    // Study Timer State
    private val _timerState = MutableStateFlow(StudyTimerState(initialMinutes = userPreferencesRepository.getDefaultTimerMinutes()))
    val timerState: StateFlow<StudyTimerState> = _timerState.asStateFlow()
    private var timerJob: Job? = null

    init {
        performSearch("", "all", immediate = true)
    }

    fun navigateTo(screen: FocusScreen) {
        if (screen != FocusScreen.PLAYER) {
            _isFocusMode.value = false
        }
        _currentScreen.value = screen
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        // Only reset to curated catalog if query is cleared completely
        if (query.isBlank()) {
            performSearch("", _selectedCategory.value, immediate = true)
        }
    }

    fun onSearchSubmitted(query: String) {
        _searchQuery.value = query
        performSearch(query.trim(), _selectedCategory.value, immediate = true)
    }

    fun onCategorySelected(categoryId: String) {
        _selectedCategory.value = categoryId
        performSearch(_searchQuery.value.trim(), categoryId, immediate = true)
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchApiErrorMessage.value = null
        performSearch("", _selectedCategory.value, immediate = true)
    }

    fun retrySearch() {
        performSearch(_searchQuery.value, _selectedCategory.value, immediate = true)
    }

    private fun performSearch(query: String, categoryId: String, immediate: Boolean) {
        viewModelScope.launch {
            _isSearching.value = true
            _searchApiErrorMessage.value = null
            val apiKey = apiKeyProvider.getEffectiveApiKey()
            val result = repository.search(query = query, categoryId = categoryId, apiKey = apiKey)

            _searchResults.value = result.lectures
            _isLiveApiSearch.value = result.isLiveApiResult
            _searchApiErrorMessage.value = result.errorMessage
            _isSearching.value = false
        }
    }

    fun selectLectureToWatch(lecture: Lecture) {
        _currentLecture.value = lecture
        _currentNotes.value = lecture.notes
        _currentScreen.value = FocusScreen.PLAYER
    }

    fun selectLecture(lecture: Lecture) = selectLectureToWatch(lecture)
    fun searchLectures(query: String) = onSearchSubmitted(query)
    fun retryLastSearch() = retrySearch()

    fun toggleSaveLecture(lecture: Lecture) {
        viewModelScope.launch {
            if (lecture.isSaved) {
                repository.removeLecture(lecture.videoId)
            } else {
                repository.saveLecture(lecture)
            }
        }
    }

    fun toggleFocusMode() {
        _isFocusMode.value = !_isFocusMode.value
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun toggleKeepScreenOn() {
        val newVal = !_keepScreenOn.value
        _keepScreenOn.value = newVal
        userPreferencesRepository.setKeepScreenOn(newVal)
    }

    fun toggleOpenInBrowserDefault() {
        val newVal = !_openInYouTubeDefault.value
        _openInYouTubeDefault.value = newVal
        userPreferencesRepository.setOpenInYouTubeDefault(newVal)
    }

    fun toggleOpenInYouTubeDefault() = toggleOpenInBrowserDefault()

    fun setOpenInBrowserDefault(enabled: Boolean) {
        _openInYouTubeDefault.value = enabled
        userPreferencesRepository.setOpenInYouTubeDefault(enabled)
    }

    fun setOpenInYouTubeDefault(enabled: Boolean) = setOpenInBrowserDefault(enabled)

    fun openInBrowser(context: Context, videoId: String) {
        val url = "https://www.youtube.com/watch?v=$videoId"
        val pm = context.packageManager

        // Create browser intent
        val browserTestIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        val resolveList = try {
            pm.queryIntentActivities(browserTestIntent, PackageManager.MATCH_DEFAULT_ONLY)
        } catch (e: Exception) {
            emptyList()
        }

        // Specifically find a browser that is NOT the native YouTube application
        val browserPkg = resolveList.firstOrNull { info ->
            val pkg = info.activityInfo.packageName.lowercase()
            (pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("firefox") ||
             pkg.contains("opera") || pkg.contains("brave") || pkg.contains("edge") ||
             pkg.contains("duckduckgo")) && !pkg.contains("youtube")
        }?.activityInfo?.packageName ?: resolveList.firstOrNull { info ->
            val pkg = info.activityInfo.packageName.lowercase()
            !pkg.contains("youtube")
        }?.activityInfo?.packageName

        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addCategory(Intent.CATEGORY_BROWSABLE)
            if (browserPkg != null) {
                setPackage(browserPkg)
            }
        }

        try {
            context.startActivity(webIntent)
        } catch (e: Exception) {
            try {
                // Fallback to general intent
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                // ignore
            }
        }
    }

    fun updatePlaybackProgress(currentSec: Int, totalSec: Int) {
        val lecture = _currentLecture.value ?: return
        val isCompleted = totalSec > 0 && currentSec >= (totalSec * 0.9)
        viewModelScope.launch {
            repository.updateProgress(
                videoId = lecture.videoId,
                progressSeconds = currentSec,
                totalSeconds = totalSec,
                isCompleted = isCompleted
            )
            _currentLecture.value = lecture.copy(
                progressSeconds = currentSec,
                totalSeconds = totalSec,
                isCompleted = isCompleted
            )
        }
    }

    fun onNotesChanged(newNotes: String) {
        _currentNotes.value = newNotes
        val lecture = _currentLecture.value ?: return
        viewModelScope.launch {
            repository.saveNotes(lecture.videoId, newNotes)
            _currentLecture.value = lecture.copy(notes = newNotes)
        }
    }

    // Study Timer controls
    fun startStudyTimer(minutes: Int = _timerState.value.initialMinutes) {
        timerJob?.cancel()
        _timerState.value = StudyTimerState(
            initialMinutes = minutes,
            secondsRemaining = minutes * 60,
            isRunning = true,
            isFinished = false
        )

        timerJob = viewModelScope.launch {
            while (_timerState.value.secondsRemaining > 0 && _timerState.value.isRunning) {
                delay(1000)
                val remaining = _timerState.value.secondsRemaining - 1
                if (remaining <= 0) {
                    _timerState.value = _timerState.value.copy(
                        secondsRemaining = 0,
                        isRunning = false,
                        isFinished = true
                    )
                    // Record completed study session
                    val currentVid = _currentLecture.value
                    repository.recordStudySession(
                        videoId = currentVid?.videoId ?: "general_study",
                        videoTitle = currentVid?.title ?: "Study Session",
                        minutes = minutes
                    )
                    break
                } else {
                    _timerState.value = _timerState.value.copy(secondsRemaining = remaining)
                }
            }
        }
    }

    fun pauseStudyTimer() {
        _timerState.value = _timerState.value.copy(isRunning = false)
        timerJob?.cancel()
    }

    fun resumeStudyTimer() {
        if (_timerState.value.secondsRemaining <= 0) {
            startStudyTimer(_timerState.value.initialMinutes)
            return
        }
        _timerState.value = _timerState.value.copy(isRunning = true)
        timerJob = viewModelScope.launch {
            while (_timerState.value.secondsRemaining > 0 && _timerState.value.isRunning) {
                delay(1000)
                val remaining = _timerState.value.secondsRemaining - 1
                if (remaining <= 0) {
                    _timerState.value = _timerState.value.copy(
                        secondsRemaining = 0,
                        isRunning = false,
                        isFinished = true
                    )
                    val currentVid = _currentLecture.value
                    repository.recordStudySession(
                        videoId = currentVid?.videoId ?: "general_study",
                        videoTitle = currentVid?.title ?: "Study Session",
                        minutes = _timerState.value.initialMinutes
                    )
                    break
                } else {
                    _timerState.value = _timerState.value.copy(secondsRemaining = remaining)
                }
            }
        }
    }

    fun resetStudyTimer(minutes: Int = 25) {
        timerJob?.cancel()
        userPreferencesRepository.setDefaultTimerMinutes(minutes)
        _timerState.value = StudyTimerState(
            initialMinutes = minutes,
            secondsRemaining = minutes * 60,
            isRunning = false,
            isFinished = false
        )
    }

    // API Key Management Actions
    fun saveCustomApiKey(key: String) {
        apiKeyProvider.setCustomApiKey(key)
        _apiKeyStatus.value = apiKeyProvider.getApiKeyStatus()
        _apiKeyValidationResult.value = if (key.isNotBlank()) "API Key saved successfully." else "API Key cleared."
        // Refresh search with new key
        performSearch(_searchQuery.value, _selectedCategory.value, immediate = true)
    }

    fun clearCustomApiKey() {
        apiKeyProvider.clearCustomApiKey()
        _apiKeyStatus.value = apiKeyProvider.getApiKeyStatus()
        _apiKeyValidationResult.value = "Custom API Key removed. Reverted to built-in academic catalog."
        performSearch(_searchQuery.value, _selectedCategory.value, immediate = true)
    }

    fun testApiKey(key: String) {
        val testKey = key.trim().ifBlank { apiKeyProvider.getEffectiveApiKey() }
        if (testKey.isBlank()) {
            _apiKeyValidationResult.value = "Please enter an API Key to test."
            return
        }

        viewModelScope.launch {
            _isTestingApiKey.value = true
            _apiKeyValidationResult.value = null
            val result = YouTubeApiClient.validateApiKey(testKey)
            if (result.isSuccess) {
                _apiKeyValidationResult.value = "Success: API Key is active and verified with YouTube Data API v3!"
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to connect with this API key."
                _apiKeyValidationResult.value = "Validation Error: $err"
            }
            _isTestingApiKey.value = false
        }
    }

    fun clearApiKeyValidationResult() {
        _apiKeyValidationResult.value = null
    }

    fun clearAllUserData() {
        viewModelScope.launch {
            repository.clearAllData()
            userPreferencesRepository.clearAllPreferences()
            _apiKeyStatus.value = apiKeyProvider.getApiKeyStatus()
            _searchResults.value = EducationalVideoCatalog.CURATED_LECTURES
        }
    }
}
