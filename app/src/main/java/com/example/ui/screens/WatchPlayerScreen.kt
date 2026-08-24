package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val isTablet = configuration.screenWidthDp >= 600

    val playerController = remember { YouTubePlayerController() }
    var currentPlaybackSec by remember { mutableIntStateOf(lecture.progressSeconds) }
    var totalDurationSec by remember { mutableIntStateOf(lecture.totalSeconds) }
    var selectedTab by remember { mutableStateOf(StudyStudioTab.NOTES) }
    var showControlsInFocusMode by remember { mutableStateOf(true) }
    var currentQuality by remember { mutableStateOf("auto") }

    // Settings Bottom Sheet (Speed, Quality, Looper)
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Double tap feedback
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(doubleTapFeedback) {
        if (doubleTapFeedback != null) {
            delay(700)
            doubleTapFeedback = null
        }
    }

    // Auto-hide controls in fullscreen after 3.5s
    LaunchedEffect(showControlsInFocusMode) {
        if (showControlsInFocusMode) {
            delay(3500)
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

    // Restore orientation when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Hardware/Gesture Back Handler
    BackHandler(enabled = true) {
        if (isFocusMode) {
            onToggleFocusMode()
        } else {
            onBack()
        }
    }

    // Playback Settings Modal Sheet (Quality, Speed, Looper)
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = Color(0xFF0F172A),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Playback & Stream Settings",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    IconButton(onClick = { showSettingsSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Playback Speed
                Text(
                    text = "PLAYBACK SPEED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocusAmber,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                        val isSelected = playbackSpeed == speed
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onSpeedChanged(speed)
                                playerController.setPlaybackSpeed?.invoke(speed)
                            },
                            label = { Text("${speed}x", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FocusIndigo,
                                selectedLabelColor = Color.White,
                                containerColor = SlateCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) FocusIndigo else SlateBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Video Quality
                Text(
                    text = "VIDEO QUALITY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocusAmber,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "auto" to "Auto",
                        "hd1080" to "1080p Full HD",
                        "hd720" to "720p HD",
                        "large" to "480p SD",
                        "medium" to "360p Saver"
                    ).forEach { (code, label) ->
                        val isSelected = currentQuality == code
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                currentQuality = code
                                playerController.setPlaybackQuality?.invoke(code)
                            },
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FocusIndigo,
                                selectedLabelColor = Color.White,
                                containerColor = SlateCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) FocusIndigo else SlateBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // FULLSCREEN CINEMATIC MODE (100% Edge-to-Edge, Speed & Quality controls, Scrubber, Double-Tap)
    if (isFocusMode) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("focus_mode_container")
        ) {
            // Video Layer
            YouTubePlayerView(
                videoId = lecture.videoId,
                startSeconds = lecture.progressSeconds,
                playbackSpeed = playbackSpeed,
                playbackQuality = currentQuality,
                controller = playerController,
                onProgressUpdate = { cur, tot ->
                    currentPlaybackSec = cur
                    totalDurationSec = tot
                    onProgressUpdate(cur, tot)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Transparent Gesture Interceptor Layer (Tap to toggle HUD, Double Tap to Seek)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showControlsInFocusMode = !showControlsInFocusMode
                            },
                            onDoubleTap = { offset ->
                                if (offset.x < size.width / 2) {
                                    playerController.seekRelative?.invoke(-10)
                                    doubleTapFeedback = "⏪ -10s"
                                } else {
                                    playerController.seekRelative?.invoke(10)
                                    doubleTapFeedback = "⏩ +10s"
                                }
                                showControlsInFocusMode = true
                            }
                        )
                    }
            )

            // Double Tap Visual Pill Animation
            AnimatedVisibility(
                visible = doubleTapFeedback != null,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                doubleTapFeedback?.let { feedback ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color(0xEE090D16))
                            .border(1.dp, FocusIndigo, RoundedCornerShape(30.dp))
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = feedback,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Cinematic Floating HUD (Auto-fading)
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
                                    Color(0xAA000000),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color(0xAA000000)
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Top Bar: Exit Fullscreen Pill + Lecture Title + Study Timer + Settings + Rotate
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Prominent 1-Tap Exit Fullscreen Button
                        Card(
                            onClick = onToggleFocusMode,
                            colors = CardDefaults.cardColors(containerColor = Color(0xDD1E293B)),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, FocusIndigo),
                            modifier = Modifier.testTag("exit_focus_mode_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "Exit Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Exit Fullscreen",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        // Lecture Title in Header
                        Text(
                            text = lecture.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Active Focus Timer Pill
                            if (timerState.isRunning) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xDD090D16), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0x44F59E0B), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = FocusAmber,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = timerState.formattedTime,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }

                            // Playback Speed & Quality Button
                            IconButton(
                                onClick = { showSettingsSheet = true },
                                modifier = Modifier
                                    .background(Color(0xDD1E293B), CircleShape)
                                    .size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Speed and Quality",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Orientation Flip Button
                            IconButton(
                                onClick = {
                                    activity?.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    }
                                },
                                modifier = Modifier
                                    .background(Color(0xDD1E293B), CircleShape)
                                    .size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "Rotate Screen",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Bottom Floating Scrubber HUD in Fullscreen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xEE090D16))
                            .border(1.dp, Color(0x336366F1), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Interactive Progress Scrubber
                        if (totalDurationSec > 0) {
                            Slider(
                                value = currentPlaybackSec.toFloat(),
                                onValueChange = { targetSec ->
                                    currentPlaybackSec = targetSec.toInt()
                                    playerController.seekTo?.invoke(targetSec.toInt())
                                },
                                valueRange = 0f..totalDurationSec.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = FocusIndigo,
                                    activeTrackColor = FocusIndigo,
                                    inactiveTrackColor = SlateBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Quick 10s Rewind & Forward + Time
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { playerController.seekRelative?.invoke(-10) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "Rewind 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { playerController.seekRelative?.invoke(10) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "Forward 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Text(
                                    text = "${ChapterParser.formatSecondsToDisplay(currentPlaybackSec)} / ${ChapterParser.formatSecondsToDisplay(totalDurationSec)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            // Speed & Quality Quick Pills
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Quality Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1E293B))
                                        .clickable { showSettingsSheet = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (currentQuality == "auto") "Auto" else currentQuality.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = FocusAmber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                // Speed Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(FocusIndigo)
                                        .clickable { showSettingsSheet = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${playbackSpeed}x",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
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
        return
    }

    // STANDARD STUDY STUDIO VIEW (Adaptive Tablet Dual-Pane vs Phone Column)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("study_player_container")
    ) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Adaptive Tablet Dual-Pane Cinematic Study Studio
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Left pane: Video Player & Core Study Controls
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
                        onToggleSave = onToggleSave,
                        onOpenSettings = { showSettingsSheet = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 16:9 Video Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.dp, Color(0x446366F1), RoundedCornerShape(16.dp))
                    ) {
                        YouTubePlayerView(
                            videoId = lecture.videoId,
                            startSeconds = lecture.progressSeconds,
                            playbackSpeed = playbackSpeed,
                            playbackQuality = currentQuality,
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

                    // Quick Study Action Bar
                    QuickStudyControlsBar(
                        currentSpeed = playbackSpeed,
                        currentQuality = currentQuality,
                        isLoopActive = isLoopActive,
                        currentSec = currentPlaybackSec,
                        totalSec = totalDurationSec,
                        onRewind10 = { playerController.seekRelative?.invoke(-10) },
                        onForward10 = { playerController.seekRelative?.invoke(10) },
                        onOpenSettings = { showSettingsSheet = true },
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

                    // Lecture Info Box
                    LectureMetadataBox(lecture = lecture)
                }

                // Right pane: Study Studio Tabs (Notes, Chapters, Pomodoro, Syllabus)
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
                    onToggleSave = onToggleSave,
                    onOpenSettings = { showSettingsSheet = true }
                )

                // Video Player Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(1.dp, Color(0x446366F1), RoundedCornerShape(16.dp))
                ) {
                    YouTubePlayerView(
                        videoId = lecture.videoId,
                        startSeconds = lecture.progressSeconds,
                        playbackSpeed = playbackSpeed,
                        playbackQuality = currentQuality,
                        controller = playerController,
                        onProgressUpdate = { cur, tot ->
                            currentPlaybackSec = cur
                            totalDurationSec = tot
                            onProgressUpdate(cur, tot)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Quick Study Controls Toolbar
                QuickStudyControlsBar(
                    currentSpeed = playbackSpeed,
                    currentQuality = currentQuality,
                    isLoopActive = isLoopActive,
                    currentSec = currentPlaybackSec,
                    totalSec = totalDurationSec,
                    onRewind10 = { playerController.seekRelative?.invoke(-10) },
                    onForward10 = { playerController.seekRelative?.invoke(10) },
                    onOpenSettings = { showSettingsSheet = true },
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
    onToggleSave: () -> Unit,
    onOpenSettings: () -> Unit
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
            // Settings button
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Speed and Quality",
                    tint = TextSecondary
                )
            }

            // Fullscreen Focus Mode Button
            Card(
                onClick = onToggleFocusMode,
                colors = CardDefaults.cardColors(containerColor = Color(0x336366F1)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, FocusIndigo),
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
                        text = "Fullscreen Studio",
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
    currentQuality: String,
    isLoopActive: Boolean,
    currentSec: Int,
    totalSec: Int,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onOpenSettings: () -> Unit,
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
            border = BorderStroke(1.dp, FocusAmber)
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
            border = BorderStroke(
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

        // Speed & Quality Settings pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(FocusIndigo)
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${playbackSpeed}x",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
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
        border = BorderStroke(1.dp, SlateBorder)
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
        border = BorderStroke(1.dp, SlateBorder)
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
                border = BorderStroke(1.dp, SlateBorder),
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
