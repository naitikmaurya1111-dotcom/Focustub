package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FocusTubeDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.Lecture
import com.example.data.remote.ChapterParser
import com.example.data.remote.YouTubeApiClient
import com.example.data.repository.ApiKeyProvider
import com.example.data.repository.ApiKeySource
import com.example.data.repository.ApiKeyStatus
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
    SETTINGS,
    PLAYER
}

data class StudyTimerState(
    val initialMinutes: Int = 25,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
) {
    val secondsRemaining: Int
        get() = remainingSeconds

    val formattedTime: String
        get() = ChapterParser.formatSecondsToDisplay(remainingSeconds)

    val progressFraction: Float
        get() {
            val totalSec = initialMinutes * 60
            if (totalSec <= 0) return 0f
            return 1f - (remainingSeconds.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
        }
}

class FocusTubeViewModel(
    application: Application,
    private val repository: LectureRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val apiKeyProvider: ApiKeyProvider
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        userPreferencesRepository = UserPreferencesRepository(application),
        apiKeyProvider = ApiKeyProvider(UserPreferencesRepository(application)),
        repository = LectureRepository(
            FocusTubeDatabase.getInstance(application).lectureDao(),
            FocusTubeDatabase.getInstance(application).studySessionDao()
        )
    )

    // Screen Navigation
    private val _currentScreen = MutableStateFlow(FocusScreen.SEARCH)
    val currentScreen: StateFlow<FocusScreen> = _currentScreen.asStateFlow()

    // Search & Catalog State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isLiveApiSearch = MutableStateFlow(false)
    val isLiveApiSearch: StateFlow<Boolean> = _isLiveApiSearch.asStateFlow()

    private val _searchApiErrorMessage = MutableStateFlow<String?>(null)
    val searchApiErrorMessage: StateFlow<String?> = _searchApiErrorMessage.asStateFlow()

    // Persistent Search History
    private val _searchHistory = MutableStateFlow<List<String>>(userPreferencesRepository.getSearchHistory())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    // API Key State
    private val _apiKeyStatus = MutableStateFlow(apiKeyProvider.getApiKeyStatus())
    val apiKeyStatus: StateFlow<ApiKeyStatus> = _apiKeyStatus.asStateFlow()

    private val _isTestingApiKey = MutableStateFlow(false)
    val isTestingApiKey: StateFlow<Boolean> = _isTestingApiKey.asStateFlow()

    private val _apiKeyValidationResult = MutableStateFlow<String?>(null)
    val apiKeyValidationResult: StateFlow<String?> = _apiKeyValidationResult.asStateFlow()

    // Search results combined with saved status (empty by default on clean home screen)
    val savedLectures: StateFlow<List<Lecture>> = repository.savedLectures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLectures: StateFlow<List<Lecture>> = repository.recentLectures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMinutesStudied: StateFlow<Int> = repository.totalMinutesStudied
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _searchResults = MutableStateFlow<List<Lecture>>(emptyList())

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun navigateTo(screen: FocusScreen) {
        if (screen != FocusScreen.PLAYER) {
            _isFocusMode.value = false
        }
        _currentScreen.value = screen
    }

    private var searchJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _searchResults.value = emptyList()
            _searchApiErrorMessage.value = null
            _isSearching.value = false
        } else {
            // Instant real-time debounced search as the user types
            searchJob = viewModelScope.launch {
                delay(300)
                performSearch(trimmed, _selectedCategory.value, immediate = false)
            }
        }
    }

    fun onSearchSubmitted(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        _searchQuery.value = trimmed
        if (trimmed.isNotBlank()) {
            userPreferencesRepository.saveSearchQuery(trimmed)
            _searchHistory.value = userPreferencesRepository.getSearchHistory()
            performSearch(trimmed, _selectedCategory.value, immediate = true)
        } else {
            _searchResults.value = emptyList()
            _searchApiErrorMessage.value = null
            _isSearching.value = false
        }
    }

    fun deleteSearchHistoryItem(query: String) {
        userPreferencesRepository.deleteSearchQuery(query)
        _searchHistory.value = userPreferencesRepository.getSearchHistory()
    }

    fun clearAllSearchHistory() {
        userPreferencesRepository.clearSearchHistory()
        _searchHistory.value = emptyList()
    }

    fun onCategorySelected(categoryId: String) {
        _selectedCategory.value = categoryId
        val trimmed = _searchQuery.value.trim()
        if (trimmed.isNotBlank()) {
            performSearch(trimmed, categoryId, immediate = true)
        } else {
            // Load category curated list so user sees instant results
            viewModelScope.launch {
                _isSearching.value = true
                _searchApiErrorMessage.value = null
                val results = repository.search("", categoryId, apiKey = apiKeyProvider.getEffectiveApiKey())
                _searchResults.value = results.lectures
                _isLiveApiSearch.value = results.isLiveApiResult
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _searchApiErrorMessage.value = null
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    fun retrySearch() {
        if (_searchQuery.value.isNotBlank()) {
            performSearch(_searchQuery.value.trim(), _selectedCategory.value, immediate = true)
        }
    }

    private fun performSearch(query: String, categoryId: String, immediate: Boolean) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
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
        val browserTest = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val resolveList = try {
            pm.queryIntentActivities(browserTest, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        } catch (e: Exception) {
            emptyList()
        }
        val browserPkg = resolveList.firstOrNull { info ->
            val pkg = info.activityInfo.packageName.lowercase()
            (pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("firefox") ||
             pkg.contains("opera") || pkg.contains("brave") || pkg.contains("edge")) && !pkg.contains("youtube")
        }?.activityInfo?.packageName ?: resolveList.firstOrNull { info ->
            !info.activityInfo.packageName.lowercase().contains("youtube")
        }?.activityInfo?.packageName

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addCategory(Intent.CATEGORY_BROWSABLE)
            if (browserPkg != null) {
                setPackage(browserPkg)
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (_: Exception) {}
        }
    }

    fun onNotesChanged(notes: String) {
        _currentNotes.value = notes
        val lecture = _currentLecture.value ?: return
        viewModelScope.launch {
            repository.updateNotes(lecture.videoId, notes)
        }
    }

    fun updateProgress(currentSec: Int, totalSec: Int) {
        val lecture = _currentLecture.value ?: return
        if (currentSec <= 0 && totalSec <= 0) return
        val isCompleted = totalSec > 0 && currentSec >= (totalSec * 0.90f)
        viewModelScope.launch {
            repository.updateProgress(lecture.videoId, currentSec, totalSec, isCompleted)
        }
    }

    fun updatePlaybackProgress(currentSec: Int, totalSec: Int) = updateProgress(currentSec, totalSec)

    fun startStudyTimer(minutes: Int) {
        timerJob?.cancel()
        _timerState.value = StudyTimerState(
            initialMinutes = minutes,
            remainingSeconds = minutes * 60,
            isRunning = true,
            isFinished = false
        )
        timerJob = viewModelScope.launch {
            while (_timerState.value.remainingSeconds > 0 && _timerState.value.isRunning) {
                delay(1000)
                val newSec = _timerState.value.remainingSeconds - 1
                if (newSec <= 0) {
                    _timerState.value = _timerState.value.copy(
                        remainingSeconds = 0,
                        isRunning = false,
                        isFinished = true
                    )
                    val lecture = _currentLecture.value
                    repository.recordStudySession(
                        durationSeconds = minutes * 60,
                        videoId = lecture?.videoId,
                        videoTitle = lecture?.title
                    )
                    break
                } else {
                    _timerState.value = _timerState.value.copy(remainingSeconds = newSec)
                }
            }
        }
    }

    fun pauseStudyTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(isRunning = false)
    }

    fun resumeStudyTimer() {
        if (_timerState.value.remainingSeconds <= 0) return
        _timerState.value = _timerState.value.copy(isRunning = true)
        timerJob = viewModelScope.launch {
            while (_timerState.value.remainingSeconds > 0 && _timerState.value.isRunning) {
                delay(1000)
                val newSec = _timerState.value.remainingSeconds - 1
                if (newSec <= 0) {
                    _timerState.value = _timerState.value.copy(
                        remainingSeconds = 0,
                        isRunning = false,
                        isFinished = true
                    )
                    val lecture = _currentLecture.value
                    repository.recordStudySession(
                        durationSeconds = _timerState.value.initialMinutes * 60,
                        videoId = lecture?.videoId,
                        videoTitle = lecture?.title
                    )
                    break
                } else {
                    _timerState.value = _timerState.value.copy(remainingSeconds = newSec)
                }
            }
        }
    }

    fun resetStudyTimer(minutes: Int) {
        timerJob?.cancel()
        _timerState.value = StudyTimerState(
            initialMinutes = minutes,
            remainingSeconds = minutes * 60,
            isRunning = false,
            isFinished = false
        )
    }

    fun saveCustomApiKey(key: String) {
        userPreferencesRepository.setCustomApiKey(key)
        _apiKeyStatus.value = apiKeyProvider.getApiKeyStatus()
        _apiKeyValidationResult.value = if (key.isNotBlank()) "API Key saved successfully." else "API Key cleared."
        if (_searchQuery.value.isNotBlank()) {
            performSearch(_searchQuery.value, _selectedCategory.value, immediate = true)
        }
    }

    fun clearCustomApiKey() {
        userPreferencesRepository.setCustomApiKey("")
        _apiKeyStatus.value = apiKeyProvider.getApiKeyStatus()
        _apiKeyValidationResult.value = "Custom API Key removed. Reverted to built-in academic catalog."
        if (_searchQuery.value.isNotBlank()) {
            performSearch(_searchQuery.value, _selectedCategory.value, immediate = true)
        }
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
            _apiKeyValidationResult.value = null
            _searchHistory.value = emptyList()
            _searchResults.value = emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
