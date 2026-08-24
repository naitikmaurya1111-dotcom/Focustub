package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.draw.scale
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
    var isVideoPlaying by remember { mutableStateOf(true) }
    var currentPlaybackSec by remember { mutableIntStateOf(lecture.progressSeconds) }
    var totalDurationSec by remember { mutableIntStateOf(lecture.totalSeconds) }
    var selectedTab by remember { mutableStateOf(StudyStudioTab.NOTES) }
    var showControlsInFocusMode by remember { mutableStateOf(true) }
    var currentQuality by remember { mutableStateOf("auto") }

    // Settings Bottom Sheet (Speed, Quality, Looper)
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Double tap feedback
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) }
    var doubleTapSide by remember { mutableStateOf(0) } // -1 for left, 1 for right
    LaunchedEffect(doubleTapFeedback) {
        if (doubleTapFeedback != null) {
            delay(700)
            doubleTapFeedback = null
            doubleTapSide = 0
        }
    }

    // Auto-hide controls in fullscreen after 4s
    LaunchedEffect(showControlsInFocusMode) {
        if (showControlsInFocusMode) {
            delay(4000)
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
            containerColor = ObsidianBg,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Playback Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    IconButton(
                        onClick = { showSettingsSheet = false },
                        modifier = Modifier.background(SlateCard, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Playback Speed
                Text(
                    text = "PLAYBACK SPEED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocusAmber,
                        letterSpacing = 1.2.sp
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                        val isSelected = playbackSpeed == speed
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onSpeedChanged(speed)
                                playerController.setPlaybackSpeed?.invoke(speed)
                            },
                            label = { 
                                Text(
                                    "${speed}x", 
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FocusIndigo,
                                selectedLabelColor = Color.White,
                                containerColor = SlateCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) FocusIndigo else SlateBorder,
                                borderWidth = if (isSelected) 2.dp else 1.dp
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Video Quality
                Text(
                    text = "VIDEO QUALITY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocusAmber,
                        letterSpacing = 1.2.sp
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "auto" to "Auto",
                        "hd1080" to "1080p FHD",
                        "hd720" to "720p HD",
                        "large" to "480p SD",
                        "medium" to "360p"
                    ).forEach { (code, label) ->
                        val isSelected = currentQuality == code
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                currentQuality = code
                                playerController.setPlaybackQuality?.invoke(code)
                            },
                            label = { 
                                Text(
                                    label, 
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FocusIndigo,
                                selectedLabelColor = Color.White,
                                containerColor = SlateCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) FocusIndigo else SlateBorder,
                                borderWidth = if (isSelected) 2.dp else 1.dp
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // FULLSCREEN CINEMATIC MODE (100% Edge-to-Edge, Play/Pause Stop button, Speed & Quality, Scrubber)
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
                onPlayStateChanged = { isPlaying ->
                    isVideoPlaying = isPlaying
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
                                    doubleTapFeedback = "-10s"
                                    doubleTapSide = -1
                                } else {
                                    playerController.seekRelative?.invoke(10)
                                    doubleTapFeedback = "+10s"
                                    doubleTapSide = 1
                                }
                                showControlsInFocusMode = true
                            }
                        )
                    }
            )

            // Double Tap Visual Pill Animation
            AnimatedVisibility(
                visible = doubleTapFeedback != null,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.align(if (doubleTapSide == -1) Alignment.CenterStart else Alignment.CenterEnd)
            ) {
                doubleTapFeedback?.let { feedback ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(180.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = if (doubleTapSide == -1) listOf(Color(0x99000000), Color.Transparent)
                                    else listOf(Color.Transparent, Color(0x99000000))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (doubleTapSide == -1) Icons.Default.Replay10 else Icons.Default.Forward10,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = feedback,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
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
                                    Color(0xCC000000),
                                    Color(0x33000000),
                                    Color.Transparent,
                                    Color(0x33000000),
                                    Color(0xDD000000)
                                )
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 20.dp)
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
                        val exitInteractionSource = remember { MutableInteractionSource() }
                        val isExitPressed by exitInteractionSource.collectIsPressedAsState()
                        val exitScale by animateFloatAsState(if (isExitPressed) 0.95f else 1f)
                        
                        Card(
                            onClick = onToggleFocusMode,
                            colors = CardDefaults.cardColors(containerColor = Color(0x55000000)),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier
                                .testTag("exit_focus_mode_button")
                                .scale(exitScale),
                            interactionSource = exitInteractionSource
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "Exit Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Exit",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }

                        // Lecture Title in Header
                        Text(
                            text = lecture.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 24.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Active Focus Timer Pill
                            if (timerState.isRunning) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x55000000), RoundedCornerShape(12.dp))
                                        .border(1.dp, FocusAmber, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = FocusAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = timerState.formattedTime,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                color = FocusAmber,
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
                                    .background(Color(0x55000000), CircleShape)
                                    .size(44.dp)
                                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Speed and Quality",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
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
                                    .background(Color(0x55000000), CircleShape)
                                    .size(44.dp)
                                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "Rotate Screen",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Large Center Play/Pause / Stop Control
                    val playInteractionSource = remember { MutableInteractionSource() }
                    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
                    val playScale by animateFloatAsState(if (isPlayPressed) 0.9f else 1f)
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(76.dp)
                            .scale(playScale)
                            .clip(CircleShape)
                            .background(Color(0x77000000))
                            .border(2.dp, FocusIndigo, CircleShape)
                            .clickable(
                                interactionSource = playInteractionSource,
                                indication = null
                            ) {
                                playerController.togglePlayPause?.invoke()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isVideoPlaying) "Pause Video" else "Play Video",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    // Bottom Floating Scrubber HUD in Fullscreen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xCC0C101A))
                            .border(1.dp, Color(0x446366F1), RoundedCornerShape(20.dp))
                            .padding(horizontal = 20.dp, vertical = 12.dp)
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
                                    .height(28.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Quick 10s Rewind + Play/Pause Stop Button + Forward 10s + Time
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { playerController.seekRelative?.invoke(-10) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "Rewind 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // In-Bar Play / Pause Stop Button
                                IconButton(
                                    onClick = { playerController.togglePlayPause?.invoke() },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0x446366F1), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isVideoPlaying) "Pause Video" else "Play Video",
                                        tint = FocusIndigo,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { playerController.seekRelative?.invoke(10) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "Forward 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Text(
                                    text = "${ChapterParser.formatSecondsToDisplay(currentPlaybackSec)} / ${ChapterParser.formatSecondsToDisplay(totalDurationSec)}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }

                            // Speed & Quality Quick Pills
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Quality Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SlateCard)
                                        .clickable { showSettingsSheet = true }
                                        .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (currentQuality == "auto") "Auto" else currentQuality.uppercase(),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = FocusAmber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                // Speed Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(FocusIndigo)
                                        .clickable { showSettingsSheet = true }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "${playbackSpeed}x",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
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
                        .padding(20.dp)
                ) {
                    StudioPlayerHeader(
                        lecture = lecture,
                        onBack = onBack,
                        onToggleFocusMode = onToggleFocusMode,
                        onToggleSave = onToggleSave,
                        onOpenSettings = { showSettingsSheet = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 16:9 Video Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black)
                            .border(2.dp, Color(0x336366F1), RoundedCornerShape(20.dp))
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
                            onPlayStateChanged = { isPlaying ->
                                isVideoPlaying = isPlaying
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Study Action Bar with Play/Pause
                    QuickStudyControlsBar(
                        currentSpeed = playbackSpeed,
                        currentQuality = currentQuality,
                        isPlaying = isVideoPlaying,
                        isLoopActive = isLoopActive,
                        currentSec = currentPlaybackSec,
                        totalSec = totalDurationSec,
                        onTogglePlayPause = { playerController.togglePlayPause?.invoke() },
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

                    Spacer(modifier = Modifier.height(20.dp))

                    // Lecture Info Box
                    LectureMetadataBox(lecture = lecture)
                }

                // Right pane: Study Studio Tabs (Notes, Chapters, Pomodoro, Syllabus)
                Column(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                        .background(Color(0xFF0C101A))
                        .padding(20.dp)
                ) {
                    StudyStudioTabsBar(
                        selectedTab = selectedTab,
                        hasChapters = chapters.isNotEmpty(),
                        onTabSelected = { selectedTab = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black)
                        .border(2.dp, Color(0x336366F1), RoundedCornerShape(20.dp))
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
                        onPlayStateChanged = { isPlaying ->
                            isVideoPlaying = isPlaying
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Quick Study Controls Toolbar with Play/Pause
                QuickStudyControlsBar(
                    currentSpeed = playbackSpeed,
                    currentQuality = currentQuality,
                    isPlaying = isVideoPlaying,
                    isLoopActive = isLoopActive,
                    currentSec = currentPlaybackSec,
                    totalSec = totalDurationSec,
                    onTogglePlayPause = { playerController.togglePlayPause?.invoke() },
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

                Spacer(modifier = Modifier.height(40.dp))
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
            modifier = Modifier
                .testTag("player_back_button")
                .background(SlateCard, CircleShape)
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to list",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Fullscreen Focus Mode Button
            Card(
                onClick = onToggleFocusMode,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, FocusIndigo),
                modifier = Modifier
                    .testTag("enter_focus_mode_button")
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0x336366F1), Color(0x116366F1))
                        ),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Focus Mode",
                        tint = FocusIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Focus Mode",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FocusIndigo
                        )
                    )
                }
            }

            // Save / Bookmark Button
            val saveInteractionSource = remember { MutableInteractionSource() }
            val isSavePressed by saveInteractionSource.collectIsPressedAsState()
            val saveScale by animateFloatAsState(if (isSavePressed) 0.8f else 1f)

            IconButton(
                onClick = onToggleSave,
                interactionSource = saveInteractionSource,
                modifier = Modifier
                    .testTag("player_save_button")
                    .background(SlateCard, CircleShape)
                    .size(40.dp)
                    .scale(saveScale)
            ) {
                Icon(
                    imageVector = if (lecture.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save lecture",
                    tint = if (lecture.isSaved) FocusAmber else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Settings button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .background(SlateCard, CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Speed and Quality",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickStudyControlsBar(
    currentSpeed: Float,
    currentQuality: String,
    isPlaying: Boolean,
    isLoopActive: Boolean,
    currentSec: Int,
    totalSec: Int,
    onTogglePlayPause: () -> Unit,
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
            .clip(RoundedCornerShape(16.dp))
            .background(SlateCard)
            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rewind 10s + Play/Pause Stop + Forward 10s
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onRewind10, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "Rewind 10 seconds",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0x336366F1), CircleShape)
                    .border(1.dp, FocusIndigo, CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = FocusIndigo,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = onForward10, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Forward 10 seconds",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Quick Timestamp Note Button
        Card(
            onClick = onAddTimestampNote,
            colors = CardDefaults.cardColors(containerColor = Color(0x22F59E0B)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0x44F59E0B))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = FocusAmber,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "+ $timeLabel",
                    style = MaterialTheme.typography.labelMedium.copy(
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
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                1.dp,
                if (isLoopActive) FocusIndigo else SlateBorder
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Loop,
                    contentDescription = null,
                    tint = if (isLoopActive) FocusIndigo else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isLoopActive) "Loop Active" else "A/B Loop",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isLoopActive) FocusIndigo else TextSecondary,
                        fontWeight = FontWeight.Medium
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
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[index])
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                    color = FocusIndigo,
                    height = 3.dp
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
                        style = MaterialTheme.typography.labelLarge.copy(
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
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = FocusIndigo,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Lecture Topics & Timestamps",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (chapters.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No topic timestamps in this lecture description.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You can add your own custom timestamps in the Notes tab!",
                        style = MaterialTheme.typography.labelMedium.copy(color = FocusIndigo, fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chapters.forEach { chapter ->
                        val isCurrent = currentSeconds >= chapter.startSeconds &&
                                (chapters.getOrNull(chapters.indexOf(chapter) + 1)?.startSeconds ?: Int.MAX_VALUE) > currentSeconds

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCurrent) Color(0x336366F1) else Color(0xFF131B2E))
                                .border(
                                    1.dp,
                                    if (isCurrent) Color(0x556366F1) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onChapterClick(chapter.startSeconds) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chapter.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isCurrent) Color.White else TextPrimary,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) FocusIndigo else Color(0xFF1E293B))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = chapter.displayTimestamp,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isCurrent) Color.White else FocusAmber,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
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
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = FocusIndigo,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Course & Lecture Details",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = lecture.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = FocusAmber,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = lecture.channelTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = FocusAmber,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Duration: ${lecture.duration}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            }

            if (lecture.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF131B2E))
                        .padding(16.dp)
                ) {
                    Text(
                        text = lecture.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            lineHeight = 22.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = onOpenInBrowser,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Video in External Browser", 
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
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
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(FocusIndigo),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lecture.channelTitle.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lecture.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = lecture.channelTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = FocusAmber,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                    )
                    Text(
                        text = lecture.duration,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                }
            }
        }
    }
}
