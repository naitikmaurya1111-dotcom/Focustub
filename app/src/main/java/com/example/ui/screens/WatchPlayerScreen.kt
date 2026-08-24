package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lecture
import com.example.data.model.LectureChapter
import com.example.data.remote.ChapterParser
import com.example.ui.components.NotesSection
import com.example.ui.components.StudyTimerWidget
import com.example.ui.components.YouTubePlayerController
import com.example.ui.components.YouTubePlayerView
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.StudyTimerState
import kotlinx.coroutines.delay

enum class StudyStudioTab {
    NOTES,
    CHAPTERS,
    TIMER,
    SYLLABUS
}

@Composable
fun WatchPlayerScreen(
    lecture: Lecture,
    isFocusMode: Boolean,
    playbackSpeed: Float,
    keepScreenOn: Boolean,
    openInYouTubeDefault: Boolean,
    timerState: StudyTimerState,
    notes: String,
    onBack: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onToggleSave: () -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onProgressUpdate: (Int, Int) -> Unit,
    onNotesChanged: (String) -> Unit,
    onStartTimer: (Int) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onResetTimer: (Int) -> Unit,
    onOpenInYouTube: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val playerController = remember { YouTubePlayerController() }
    var currentPlaybackSec by remember { mutableIntStateOf(lecture.progressSeconds) }
    var totalDurationSec by remember { mutableIntStateOf(lecture.totalSeconds) }
    var selectedTab by remember { mutableStateOf(StudyStudioTab.NOTES) }
    var showControlsInFocusMode by remember { mutableStateOf(false) }

    // Auto-hide controls in Fullscreen after 2.5 seconds
    LaunchedEffect(showControlsInFocusMode) {
        if (showControlsInFocusMode) {
            delay(2500)
            showControlsInFocusMode = false
        }
    }

    // A/B Looper State
    var isLoopActive by remember { mutableStateOf(false) }
    var loopStartSec by remember { mutableIntStateOf(0) }
    var loopEndSec by remember { mutableIntStateOf(0) }

    // Parse lecture chapters from description
    val chapters = remember(lecture.description) {
        ChapterParser.parseChapters(lecture.description)
    }

    // Keep screen awake during study
    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Restore portrait when leaving
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler {
        if (isFocusMode) {
            onToggleFocusMode()
        } else {
            onBack()
        }
    }

    // FULLSCREEN FOCUS MODE (Crystal Clean Fullscreen - Zero Dimming, Zero Permanent Buttons)
    if (isFocusMode) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showControlsInFocusMode = !showControlsInFocusMode }
                .testTag("focus_mode_container")
        ) {
            YouTubePlayerView(
                videoId = lecture.videoId,
                startSeconds = lecture.progressSeconds,
                playbackSpeed = playbackSpeed,
                controller = playerController,
                onProgressUpdate = { cur, tot ->
                    currentPlaybackSec = cur
                    totalDurationSec = tot
                    onProgressUpdate(cur, tot)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top Control Bar Overlay (Only appears on tap and auto-fades out in 2.5s)
            AnimatedVisibility(
                visible = showControlsInFocusMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x99000000),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color(0x99000000)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onToggleFocusMode,
                                modifier = Modifier
                                    .background(Color(0x881E293B), CircleShape)
                                    .testTag("exit_focus_mode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Exit Fullscreen",
                                    tint = Color.White
                                )
                            }

                            Text(
                                text = lecture.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Compact timer badge in focus mode if active
                            if (timerState.isRunning) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x88090D16), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = FocusAmber,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = timerState.formattedTime,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }

                            // Orientation toggle
                            IconButton(
                                onClick = {
                                    activity?.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    }
                                },
                                modifier = Modifier.background(Color(0x881E293B), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Toggle Orientation",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // STANDARD STUDY STUDIO VIEW
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("study_player_container")
    ) {
        val isWideScreen = maxWidth >= 760.dp

        if (isWideScreen) {
            // Adaptive Tablet Dual-Pane Study Studio
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Left pane: Video & Core controls
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    StudioPlayerHeader(
                        lecture = lecture,
                        onBack = onBack,
                        onToggleFocusMode = onToggleFocusMode,
                        onToggleSave = onToggleSave
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Player Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.dp, Color(0x336366F1), RoundedCornerShape(16.dp))
                    ) {
                        YouTubePlayerView(
                            videoId = lecture.videoId,
                            startSeconds = lecture.progressSeconds,
                            playbackSpeed = playbackSpeed,
                            controller = playerController,
                            onProgressUpdate = { cur, tot ->
                                currentPlaybackSec = cur
                                totalDurationSec = tot
                                onProgressUpdate(cur, tot)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Study Controls Bar
                    QuickStudyControlsBar(
                        currentSpeed = playbackSpeed,
                        isLoopActive = isLoopActive,
                        currentSec = currentPlaybackSec,
                        totalSec = totalDurationSec,
                        onRewind10 = { playerController.seekRelative?.invoke(-10) },
                        onForward10 = { playerController.seekRelative?.invoke(10) },
                        onSpeedSelected = onSpeedChanged,
                        onToggleLoop = {
                            if (isLoopActive) {
                                isLoopActive = false
                                playerController.clearLoop?.invoke()
                            } else {
                                isLoopActive = true
                                loopStartSec = (currentPlaybackSec - 10).coerceAtLeast(0)
                                loopEndSec = currentPlaybackSec + 20
                                playerController.setLoopRange?.invoke(loopStartSec, loopEndSec)
                            }
                        },
                        onAddTimestampNote = {
                            val timeStr = ChapterParser.formatSecondsToDisplay(currentPlaybackSec)
                            val stamp = "[$timeStr] "
                            val newNotes = if (notes.isBlank()) stamp else "$notes\n$stamp"
                            onNotesChanged(newNotes)
                            selectedTab = StudyStudioTab.NOTES
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Lecture Metadata Overview
                    LectureMetadataBox(lecture = lecture)
                }

                // Right pane: Tabs (Notes, Chapters, Study Timer, Syllabus)
                Column(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                        .background(Color(0xFF0C101A))
                        .padding(16.dp)
                ) {
                    StudyStudioTabsBar(
                        selectedTab = selectedTab,
                        hasChapters = chapters.isNotEmpty(),
                        onTabSelected = { selectedTab = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (selectedTab) {
                            StudyStudioTab.NOTES -> {
                                NotesSection(
                                    notes = notes,
                                    currentPlaybackSeconds = currentPlaybackSec,
                                    onNotesChanged = onNotesChanged,
                                    onSeekTo = { playerController.seekTo?.invoke(it) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            StudyStudioTab.CHAPTERS -> {
                                ChaptersListView(
                                    chapters = chapters,
                                    currentSeconds = currentPlaybackSec,
                                    onChapterClick = { sec -> playerController.seekTo?.invoke(sec) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            StudyStudioTab.TIMER -> {
                                StudyTimerWidget(
                                    timerState = timerState,
                                    onStart = onStartTimer,
                                    onPause = onPauseTimer,
                                    onResume = onResumeTimer,
                                    onReset = onResetTimer,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            StudyStudioTab.SYLLABUS -> {
                                LectureSyllabusInfoView(
                                    lecture = lecture,
                                    onOpenInBrowser = { onOpenInYouTube(lecture.videoId) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Phone Single Column Scroll
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StudioPlayerHeader(
                    lecture = lecture,
                    onBack = onBack,
                    onToggleFocusMode = onToggleFocusMode,
                    onToggleSave = onToggleSave
                )

                // Video Player Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(1.dp, Color(0x336366F1), RoundedCornerShape(16.dp))
                ) {
                    YouTubePlayerView(
                        videoId = lecture.videoId,
                        startSeconds = lecture.progressSeconds,
                        playbackSpeed = playbackSpeed,
                        controller = playerController,
                        onProgressUpdate = { cur, tot ->
                            currentPlaybackSec = cur
                            totalDurationSec = tot
                            onProgressUpdate(cur, tot)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Quick Study Controls Toolbar (Rewind, Forward, Speed, Loop, Timestamp Note)
                QuickStudyControlsBar(
                    currentSpeed = playbackSpeed,
                    isLoopActive = isLoopActive,
                    currentSec = currentPlaybackSec,
                    totalSec = totalDurationSec,
                    onRewind10 = { playerController.seekRelative?.invoke(-10) },
                    onForward10 = { playerController.seekRelative?.invoke(10) },
                    onSpeedSelected = onSpeedChanged,
                    onToggleLoop = {
                        if (isLoopActive) {
                            isLoopActive = false
                            playerController.clearLoop?.invoke()
                        } else {
                            isLoopActive = true
                            loopStartSec = (currentPlaybackSec - 10).coerceAtLeast(0)
                            loopEndSec = currentPlaybackSec + 20
                            playerController.setLoopRange?.invoke(loopStartSec, loopEndSec)
                        }
                    },
                    onAddTimestampNote = {
                        val timeStr = ChapterParser.formatSecondsToDisplay(currentPlaybackSec)
                        val stamp = "[$timeStr] "
                        val newNotes = if (notes.isBlank()) stamp else "$notes\n$stamp"
                        onNotesChanged(newNotes)
                        selectedTab = StudyStudioTab.NOTES
                    }
                )

                // Segmented Study Studio Tabs
                StudyStudioTabsBar(
                    selectedTab = selectedTab,
                    hasChapters = chapters.isNotEmpty(),
                    onTabSelected = { selectedTab = it }
                )

                // Tab Content
                when (selectedTab) {
                    StudyStudioTab.NOTES -> {
                        NotesSection(
                            notes = notes,
                            currentPlaybackSeconds = currentPlaybackSec,
                            onNotesChanged = onNotesChanged,
                            onSeekTo = { playerController.seekTo?.invoke(it) }
                        )
                    }
                    StudyStudioTab.CHAPTERS -> {
                        ChaptersListView(
                            chapters = chapters,
                            currentSeconds = currentPlaybackSec,
                            onChapterClick = { sec -> playerController.seekTo?.invoke(sec) }
                        )
                    }
                    StudyStudioTab.TIMER -> {
                        StudyTimerWidget(
                            timerState = timerState,
                            onStart = onStartTimer,
                            onPause = onPauseTimer,
                            onResume = onResumeTimer,
                            onReset = onResetTimer
                        )
                    }
                    StudyStudioTab.SYLLABUS -> {
                        LectureSyllabusInfoView(
                            lecture = lecture,
                            onOpenInBrowser = { onOpenInYouTube(lecture.videoId) }
                        )
                    }
                }

                // Lecture Information Card
                LectureMetadataBox(lecture = lecture)

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun StudioPlayerHeader(
    lecture: Lecture,
    onBack: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onToggleSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("player_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to list",
                tint = TextPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Fullscreen Focus Mode Button
            Card(
                onClick = onToggleFocusMode,
                colors = CardDefaults.cardColors(containerColor = Color(0x336366F1)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FocusIndigo),
                modifier = Modifier.testTag("enter_focus_mode_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Focus Mode",
                        tint = FocusIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Focus Mode",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = FocusIndigo
                        )
                    )
                }
            }

            // Save / Bookmark Button
            IconButton(
                onClick = onToggleSave,
                modifier = Modifier.testTag("player_save_button")
            ) {
                Icon(
                    imageVector = if (lecture.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save lecture",
                    tint = if (lecture.isSaved) FocusAmber else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun QuickStudyControlsBar(
    currentSpeed: Float,
    isLoopActive: Boolean,
    currentSec: Int,
    totalSec: Int,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onToggleLoop: () -> Unit,
    onAddTimestampNote: () -> Unit
) {
    val timeLabel = ChapterParser.formatSecondsToDisplay(currentSec)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateCard)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rewind 10s & Forward 10s
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onRewind10, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "Rewind 10 seconds",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onForward10, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Forward 10 seconds",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Quick Timestamp Note Button
        Card(
            onClick = onAddTimestampNote,
            colors = CardDefaults.cardColors(containerColor = Color(0x22F59E0B)),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, FocusAmber)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = FocusAmber,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "+ $timeLabel",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocusAmber,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // A/B Loop Button
        Card(
            onClick = onToggleLoop,
            colors = CardDefaults.cardColors(
                containerColor = if (isLoopActive) Color(0x336366F1) else Color(0xFF131B2E)
            ),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isLoopActive) FocusIndigo else SlateBorder
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Loop,
                    contentDescription = null,
                    tint = if (isLoopActive) FocusIndigo else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isLoopActive) "Loop Active" else "A/B Loop",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isLoopActive) FocusIndigo else TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Speed Selector Pills
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                val isSelected = currentSpeed == speed
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) FocusIndigo else Color.Transparent)
                        .clickable { onSpeedSelected(speed) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${speed}x",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) Color.White else TextMuted,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyStudioTabsBar(
    selectedTab: StudyStudioTab,
    hasChapters: Boolean,
    onTabSelected: (StudyStudioTab) -> Unit
) {
    val tabs = listOf(
        StudyStudioTab.NOTES to "Notes",
        StudyStudioTab.CHAPTERS to if (hasChapters) "Chapters" else "Topics",
        StudyStudioTab.TIMER to "Focus Timer",
        StudyStudioTab.SYLLABUS to "Syllabus"
    )

    TabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab },
        containerColor = Color.Transparent,
        contentColor = FocusIndigo,
        indicator = { tabPositions ->
            val index = tabs.indexOfFirst { it.first == selectedTab }
            if (index in tabPositions.indices) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                    color = FocusIndigo,
                    height = 2.dp
                )
            }
        },
        divider = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SlateBorder)
            )
        }
    ) {
        tabs.forEach { (tab, title) ->
            val isSelected = selectedTab == tab
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) FocusIndigo else TextSecondary
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun ChaptersListView(
    chapters: List<LectureChapter>,
    currentSeconds: Int,
    onChapterClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = FocusIndigo,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Lecture Topics & Timestamps",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (chapters.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No topic timestamps in this lecture description.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You can add your own custom timestamps in the Notes tab!",
                        style = MaterialTheme.typography.labelSmall.copy(color = FocusIndigo)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chapters.forEach { chapter ->
                        val isCurrent = currentSeconds >= chapter.startSeconds &&
                                (chapters.getOrNull(chapters.indexOf(chapter) + 1)?.startSeconds ?: Int.MAX_VALUE) > currentSeconds

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) Color(0x336366F1) else Color(0xFF131B2E))
                                .clickable { onChapterClick(chapter.startSeconds) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chapter.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isCurrent) Color.White else TextPrimary,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isCurrent) FocusIndigo else Color(0xFF1E293B))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = chapter.displayTimestamp,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isCurrent) Color.White else FocusAmber,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LectureSyllabusInfoView(
    lecture: Lecture,
    onOpenInBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = FocusIndigo,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Course & Lecture Details",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = lecture.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Instructor / Channel: ${lecture.channelTitle}",
                style = MaterialTheme.typography.bodySmall.copy(color = FocusAmber)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Duration: ${lecture.duration}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )

            if (lecture.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = lecture.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onOpenInBrowser,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Video in External Browser", color = TextSecondary)
            }
        }
    }
}

@Composable
private fun LectureMetadataBox(lecture: Lecture) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lecture_metadata_card"),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = lecture.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = lecture.channelTitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = FocusAmber,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
                Text(
                    text = lecture.duration,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }
    }
}
