package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Lecture
import com.example.data.repository.ApiKeyStatus
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MainSearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WatchPlayerScreen
import com.example.ui.theme.DeepSlateSurface
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.FocusScreen
import com.example.ui.viewmodel.FocusTubeViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FocusTubeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FocusTubeApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FocusTubeApp(viewModel: FocusTubeViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchResults by viewModel.displayedSearchResults.collectAsStateWithLifecycle()
    val savedLectures by viewModel.savedLectures.collectAsStateWithLifecycle()
    val recentLectures by viewModel.recentLectures.collectAsStateWithLifecycle()
    val totalMinutesStudied by viewModel.totalMinutesStudied.collectAsStateWithLifecycle()

    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val isLiveApiSearch by viewModel.isLiveApiSearch.collectAsStateWithLifecycle()
    val searchApiErrorMessage by viewModel.searchApiErrorMessage.collectAsStateWithLifecycle()

    val apiKeyStatus by viewModel.apiKeyStatus.collectAsStateWithLifecycle()
    val isTestingApiKey by viewModel.isTestingApiKey.collectAsStateWithLifecycle()
    val apiKeyValidationResult by viewModel.apiKeyValidationResult.collectAsStateWithLifecycle()

    val currentLecture by viewModel.currentLecture.collectAsStateWithLifecycle()
    val isFocusMode by viewModel.isFocusMode.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val openInYouTubeDefault by viewModel.openInYouTubeDefault.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val currentNotes by viewModel.currentNotes.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp >= 760

    if (currentScreen == FocusScreen.PLAYER && currentLecture != null) {
        // Dedicated Lecture Player (No bottom/rail nav)
        WatchPlayerScreen(
            lecture = currentLecture!!,
            isFocusMode = isFocusMode,
            playbackSpeed = playbackSpeed,
            keepScreenOn = keepScreenOn,
            openInYouTubeDefault = openInYouTubeDefault,
            timerState = timerState,
            notes = currentNotes,
            onBack = { viewModel.navigateTo(FocusScreen.SEARCH) },
            onToggleFocusMode = { viewModel.toggleFocusMode() },
            onToggleSave = { viewModel.toggleSaveLecture(currentLecture!!) },
            onSpeedChanged = { viewModel.setPlaybackSpeed(it) },
            onProgressUpdate = { cur, tot -> viewModel.updatePlaybackProgress(cur, tot) },
            onNotesChanged = { viewModel.onNotesChanged(it) },
            onStartTimer = { viewModel.startStudyTimer(it) },
            onPauseTimer = { viewModel.pauseStudyTimer() },
            onResumeTimer = { viewModel.resumeStudyTimer() },
            onResetTimer = { viewModel.resetStudyTimer(it) },
            onOpenInYouTube = { videoId -> viewModel.openInBrowser(context, videoId) },
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(if (isFocusMode) WindowInsets(0.dp) else WindowInsets.safeDrawing)
        )
    } else {
        if (isExpanded) {
            // Tablet Navigation Rail Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                NavigationRail(
                    containerColor = DeepSlateSurface,
                    contentColor = TextPrimary,
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("tablet_navigation_rail")
                ) {
                    NavigationRailItem(
                        selected = currentScreen == FocusScreen.SEARCH,
                        onClick = { viewModel.navigateTo(FocusScreen.SEARCH) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == FocusScreen.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                                contentDescription = "Search"
                            )
                        },
                        label = { Text("Search") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = FocusIndigo,
                            indicatorColor = FocusIndigo,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("rail_nav_search")
                    )

                    NavigationRailItem(
                        selected = currentScreen == FocusScreen.LIBRARY,
                        onClick = { viewModel.navigateTo(FocusScreen.LIBRARY) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == FocusScreen.LIBRARY) Icons.Filled.Bookmarks else Icons.Outlined.Bookmarks,
                                contentDescription = "Library"
                            )
                        },
                        label = { Text("Library") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = FocusIndigo,
                            indicatorColor = FocusIndigo,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("rail_nav_library")
                    )

                    NavigationRailItem(
                        selected = currentScreen == FocusScreen.SETTINGS,
                        onClick = { viewModel.navigateTo(FocusScreen.SETTINGS) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == FocusScreen.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = FocusIndigo,
                            indicatorColor = FocusIndigo,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("rail_nav_settings")
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    ScreenContent(
                        currentScreen = currentScreen,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        searchResults = searchResults,
                        recentLectures = recentLectures,
                        savedLectures = savedLectures,
                        totalMinutesStudied = totalMinutesStudied,
                        isSearching = isSearching,
                        isLiveApiSearch = isLiveApiSearch,
                        searchApiErrorMessage = searchApiErrorMessage,
                        apiKeyStatus = apiKeyStatus,
                        isTestingApiKey = isTestingApiKey,
                        apiKeyValidationResult = apiKeyValidationResult,
                        keepScreenOn = keepScreenOn,
                        openInYouTubeDefault = openInYouTubeDefault,
                        defaultTimerMinutes = timerState.initialMinutes,
                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        onSearchSubmitted = { viewModel.onSearchSubmitted(it) },
                        onCategorySelected = { viewModel.onCategorySelected(it) },
                        onClearSearch = { viewModel.clearSearch() },
                        onRetrySearch = { viewModel.retrySearch() },
                        onOpenSettings = { viewModel.navigateTo(FocusScreen.SETTINGS) },
                        onLectureSelected = { viewModel.selectLectureToWatch(it) },
                        onToggleSaveLecture = { viewModel.toggleSaveLecture(it) },
                        onNavigateToSearch = { viewModel.navigateTo(FocusScreen.SEARCH) },
                        onSaveApiKey = { viewModel.saveCustomApiKey(it) },
                        onClearApiKey = { viewModel.clearCustomApiKey() },
                        onTestApiKey = { viewModel.testApiKey(it) },
                        onClearValidationResult = { viewModel.clearApiKeyValidationResult() },
                        onToggleKeepScreenOn = { viewModel.toggleKeepScreenOn() },
                        onToggleOpenInYouTubeDefault = { viewModel.toggleOpenInYouTubeDefault() },
                        onDefaultTimerChanged = { viewModel.resetStudyTimer(it) },
                        onClearAllData = { viewModel.clearAllUserData() }
                    )
                }
            }
        } else {
            // Phone Bottom Navigation Layout
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = ObsidianBg,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(
                        containerColor = DeepSlateSurface,
                        contentColor = TextPrimary,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_navigation_bar")
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == FocusScreen.SEARCH,
                            onClick = { viewModel.navigateTo(FocusScreen.SEARCH) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == FocusScreen.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                                    contentDescription = "Search"
                                )
                            },
                            label = { Text("Search") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = FocusIndigo,
                                indicatorColor = FocusIndigo,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_search")
                        )

                        NavigationBarItem(
                            selected = currentScreen == FocusScreen.LIBRARY,
                            onClick = { viewModel.navigateTo(FocusScreen.LIBRARY) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == FocusScreen.LIBRARY) Icons.Filled.Bookmarks else Icons.Outlined.Bookmarks,
                                    contentDescription = "Library"
                                )
                            },
                            label = { Text("Library") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = FocusIndigo,
                                indicatorColor = FocusIndigo,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_library")
                        )

                        NavigationBarItem(
                            selected = currentScreen == FocusScreen.SETTINGS,
                            onClick = { viewModel.navigateTo(FocusScreen.SETTINGS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == FocusScreen.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Settings"
                                )
                            },
                            label = { Text("Settings") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = FocusIndigo,
                                indicatorColor = FocusIndigo,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ScreenContent(
                        currentScreen = currentScreen,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        searchResults = searchResults,
                        recentLectures = recentLectures,
                        savedLectures = savedLectures,
                        totalMinutesStudied = totalMinutesStudied,
                        isSearching = isSearching,
                        isLiveApiSearch = isLiveApiSearch,
                        searchApiErrorMessage = searchApiErrorMessage,
                        apiKeyStatus = apiKeyStatus,
                        isTestingApiKey = isTestingApiKey,
                        apiKeyValidationResult = apiKeyValidationResult,
                        keepScreenOn = keepScreenOn,
                        openInYouTubeDefault = openInYouTubeDefault,
                        defaultTimerMinutes = timerState.initialMinutes,
                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        onSearchSubmitted = { viewModel.onSearchSubmitted(it) },
                        onCategorySelected = { viewModel.onCategorySelected(it) },
                        onClearSearch = { viewModel.clearSearch() },
                        onRetrySearch = { viewModel.retrySearch() },
                        onOpenSettings = { viewModel.navigateTo(FocusScreen.SETTINGS) },
                        onLectureSelected = { viewModel.selectLectureToWatch(it) },
                        onToggleSaveLecture = { viewModel.toggleSaveLecture(it) },
                        onNavigateToSearch = { viewModel.navigateTo(FocusScreen.SEARCH) },
                        onSaveApiKey = { viewModel.saveCustomApiKey(it) },
                        onClearApiKey = { viewModel.clearCustomApiKey() },
                        onTestApiKey = { viewModel.testApiKey(it) },
                        onClearValidationResult = { viewModel.clearApiKeyValidationResult() },
                        onToggleKeepScreenOn = { viewModel.toggleKeepScreenOn() },
                        onToggleOpenInYouTubeDefault = { viewModel.toggleOpenInYouTubeDefault() },
                        onDefaultTimerChanged = { viewModel.resetStudyTimer(it) },
                        onClearAllData = { viewModel.clearAllUserData() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenContent(
    currentScreen: FocusScreen,
    searchQuery: String,
    selectedCategory: String,
    searchResults: List<Lecture>,
    recentLectures: List<Lecture>,
    savedLectures: List<Lecture>,
    totalMinutesStudied: Int,
    isSearching: Boolean,
    isLiveApiSearch: Boolean,
    searchApiErrorMessage: String?,
    apiKeyStatus: ApiKeyStatus,
    isTestingApiKey: Boolean,
    apiKeyValidationResult: String?,
    keepScreenOn: Boolean,
    openInYouTubeDefault: Boolean,
    defaultTimerMinutes: Int,
    onSearchQueryChanged: (String) -> Unit,
    onSearchSubmitted: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onClearSearch: () -> Unit,
    onRetrySearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onLectureSelected: (Lecture) -> Unit,
    onToggleSaveLecture: (Lecture) -> Unit,
    onNavigateToSearch: () -> Unit,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onTestApiKey: (String) -> Unit,
    onClearValidationResult: () -> Unit,
    onToggleKeepScreenOn: () -> Unit,
    onToggleOpenInYouTubeDefault: () -> Unit,
    onDefaultTimerChanged: (Int) -> Unit,
    onClearAllData: () -> Unit
) {
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { targetScreen ->
        when (targetScreen) {
            FocusScreen.SEARCH -> {
                MainSearchScreen(
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    searchResults = searchResults,
                    recentLectures = recentLectures,
                    isSearching = isSearching,
                    isLiveApiSearch = isLiveApiSearch,
                    searchApiErrorMessage = searchApiErrorMessage,
                    apiKeyStatus = apiKeyStatus,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onSearchSubmitted = onSearchSubmitted,
                    onCategorySelected = onCategorySelected,
                    onClearSearch = onClearSearch,
                    onRetrySearch = onRetrySearch,
                    onOpenSettings = onOpenSettings,
                    onLectureSelected = onLectureSelected,
                    onToggleSaveLecture = onToggleSaveLecture
                )
            }
            FocusScreen.LIBRARY -> {
                LibraryScreen(
                    savedLectures = savedLectures,
                    onLectureSelected = onLectureSelected,
                    onToggleSaveLecture = onToggleSaveLecture,
                    onNavigateToSearch = onNavigateToSearch
                )
            }
            FocusScreen.SETTINGS -> {
                SettingsScreen(
                    apiKeyStatus = apiKeyStatus,
                    isTestingApiKey = isTestingApiKey,
                    apiKeyValidationResult = apiKeyValidationResult,
                    keepScreenOn = keepScreenOn,
                    openInYouTubeDefault = openInYouTubeDefault,
                    totalMinutesStudied = totalMinutesStudied,
                    savedCount = savedLectures.size,
                    defaultTimerMinutes = defaultTimerMinutes,
                    onSaveApiKey = onSaveApiKey,
                    onClearApiKey = onClearApiKey,
                    onTestApiKey = onTestApiKey,
                    onClearValidationResult = onClearValidationResult,
                    onToggleKeepScreenOn = onToggleKeepScreenOn,
                    onToggleOpenInYouTubeDefault = onToggleOpenInYouTubeDefault,
                    onDefaultTimerChanged = onDefaultTimerChanged,
                    onClearAllData = onClearAllData
                )
            }
            FocusScreen.PLAYER -> {
                // Handled at top-level
            }
        }
    }
}
