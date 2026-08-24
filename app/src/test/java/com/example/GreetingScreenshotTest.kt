package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.repository.ApiKeySource
import com.example.data.repository.ApiKeyStatus
import com.example.data.repository.EducationalVideoCatalog
import com.example.ui.screens.MainSearchScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun focustube_main_screen_screenshot() {
        composeTestRule.setContent {
            MyApplicationTheme {
                MainSearchScreen(
                    searchQuery = "",
                    selectedCategory = "all",
                    searchResults = EducationalVideoCatalog.CURATED_LECTURES.take(4),
                    recentLectures = emptyList(),
                    isSearching = false,
                    isLiveApiSearch = false,
                    searchApiErrorMessage = null,
                    apiKeyStatus = ApiKeyStatus(
                        activeKey = "",
                        source = ApiKeySource.NONE,
                        isCustomKeySet = false,
                        maskedDisplay = "No Key Configured"
                    ),
                    onSearchQueryChanged = {},
                    onSearchSubmitted = {},
                    onCategorySelected = {},
                    onClearSearch = {},
                    onRetrySearch = {},
                    onOpenSettings = {},
                    onLectureSelected = {},
                    onToggleSaveLecture = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
