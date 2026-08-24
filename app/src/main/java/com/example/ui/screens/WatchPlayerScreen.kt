package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Lecture
import com.example.ui.components.NotesSection
import com.example.ui.components.StudyTimerWidget
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

    var showControlsInFocusMode by remember { mutableStateOf(true) }
    var useInAppPlayer by remember(lecture.videoId, openInYouTubeDefault) {
        mutableStateOf(!openInYouTubeDefault)
    }

    // Auto-launch YouTube link as default without requiring touching every time
    LaunchedEffect(lecture.videoId, openInYouTubeDefault) {
        if (openInYouTubeDefault && !useInAppPlayer) {
            onOpenInYouTube(lecture.videoId)
        }
    }

    // Keep screen awake effect
    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Restore portrait when leaving if changed
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

    // FOCUS MODE VIEW (Distraction-Free)
    if (isFocusMode) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { showControlsInFocusMode = !showControlsInFocusMode }
                .testTag("focus_mode_container")
        ) {
            if (useInAppPlayer) {
                YouTubePlayerView(
                    videoId = lecture.videoId,
                    startSeconds = lecture.progressSeconds,
                    playbackSpeed = playbackSpeed,
                    onProgressUpdate = onProgressUpdate,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                YouTubeCompanionFocusCard(
                    lecture = lecture,
                    onOpenInYouTube = { onOpenInYouTube(lecture.videoId) },
                    onSwitchToInAppPlayer = { useInAppPlayer = true }
                )
            }

            // Minimalist HUD Overlay in Focus Mode
            AnimatedVisibility(
                visible = showControlsInFocusMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x77000000))
                        .padding(16.dp)
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onToggleFocusMode,
                                modifier = Modifier
                                    .background(Color(0xCC1E293B), CircleShape)
                                    .testTag("exit_focus_mode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CenterFocusWeak,
                                    contentDescription = "Exit Focus Mode",
                                    tint = Color.White
                                )
                            }

                            Text(
                                text = "FOCUS MODE ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FocusAmber,
                                    letterSpacing = 1.5.sp
                                )
                            )
                        }

                        // Compact timer badge in focus mode
                        if (timerState.isRunning) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xCC090D16), RoundedCornerShape(8.dp))
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

                        // Orientation toggle
                        IconButton(
                            onClick = {
                                activity?.requestedOrientation = if (isLandscape) {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                            },
                            modifier = Modifier.background(Color(0xCC1E293B), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Toggle Fullscreen Orientation",
                                tint = Color.White
                            )
                        }
                    }

                    // Bottom Floating Title
                    Text(
                        text = lecture.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color(0xAA090D16), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
        return
    }

    // STANDARD DEDICATED STUDY PLAYER VIEW
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("study_player_container")
    ) {
        val isWideScreen = maxWidth >= 760.dp

        if (isWideScreen) {
            // Adaptive Tablet Dual-Pane
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Left pane: Video & Core controls
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    PlayerHeader(
                        lecture = lecture,
                        onBack = onBack,
                        onToggleFocusMode = onToggleFocusMode,
                        onToggleSave = onToggleSave
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Player / Companion Area
                    if (useInAppPlayer) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black)
                        ) {
                            YouTubePlayerView(
                                videoId = lecture.videoId,
                                startSeconds = lecture.progressSeconds,
                                playbackSpeed = playbackSpeed,
                                onProgressUpdate = onProgressUpdate,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    useInAppPlayer = false
                                    onOpenInYouTube(lecture.videoId)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = FocusIndigo
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Open in Web Browser",
                                    style = MaterialTheme.typography.labelSmall.copy(color = FocusIndigo)
                                )
                            }
                        }
                    } else {
                        YouTubeCompanionCard(
                            lecture = lecture,
                            onOpenInYouTube = { onOpenInYouTube(lecture.videoId) },
                            onSwitchToInAppPlayer = { useInAppPlayer = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SpeedSelectorRow(
                        currentSpeed = playbackSpeed,
                        onSpeedSelected = onSpeedChanged
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LectureMetadataBox(lecture = lecture)
                }

                // Right pane: Study Timer & Scratchpad Notes
                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StudyTimerWidget(
                        timerState = timerState,
                        onStart = onStartTimer,
                        onPause = onPauseTimer,
                        onResume = onResumeTimer,
                        onReset = onResetTimer
                    )

                    NotesSection(
                        notes = notes,
                        onNotesChanged = onNotesChanged
                    )
                }
            }
        } else {
            // Phone Single Column Scroll
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PlayerHeader(
                    lecture = lecture,
                    onBack = onBack,
                    onToggleFocusMode = onToggleFocusMode,
                    onToggleSave = onToggleSave
                )

                // Video / Companion Container
                if (useInAppPlayer) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black)
                    ) {
                        YouTubePlayerView(
                            videoId = lecture.videoId,
                            startSeconds = lecture.progressSeconds,
                            playbackSpeed = playbackSpeed,
                            onProgressUpdate = onProgressUpdate,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                useInAppPlayer = false
                                onOpenInYouTube(lecture.videoId)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = FocusIndigo
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Open in Web Browser",
                                style = MaterialTheme.typography.labelSmall.copy(color = FocusIndigo)
                            )
                        }
                    }
                } else {
                    YouTubeCompanionCard(
                        lecture = lecture,
                        onOpenInYouTube = { onOpenInYouTube(lecture.videoId) },
                        onSwitchToInAppPlayer = { useInAppPlayer = true }
                    )
                }

                // Speed Selector
                SpeedSelectorRow(
                    currentSpeed = playbackSpeed,
                    onSpeedSelected = onSpeedChanged
                )

                // Lecture Information Card
                LectureMetadataBox(lecture = lecture)

                // Study Companion Timer
                StudyTimerWidget(
                    timerState = timerState,
                    onStart = onStartTimer,
                    onPause = onPauseTimer,
                    onResume = onResumeTimer,
                    onReset = onResetTimer
                )

                // Study Scratchpad Notes
                NotesSection(
                    notes = notes,
                    onNotesChanged = onNotesChanged
                )

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun YouTubeCompanionCard(
    lecture: Lecture,
    onOpenInYouTube: () -> Unit,
    onSwitchToInAppPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("youtube_companion_card"),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x336366F1))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail with gradient overlay & Play Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color.Black)
                    .clickable { onOpenInYouTube() }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(lecture.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = lecture.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x33000000),
                                    Color(0xBB080C14)
                                )
                            )
                        )
                )

                // Center YouTube launch button
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .background(Color(0xE6EF4444), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Watch in YouTube",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Top Badge: Status
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xDD090D16))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = FocusEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Launched in Web Browser",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Duration Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = lecture.duration,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Controls & Options below thumbnail
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "Video opened in your distraction-free web browser (no YouTube app recommendations / shorts).",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onOpenInYouTube,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FocusIndigo,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reopen_in_browser_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reopen in Browser",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    OutlinedButton(
                        onClick = onSwitchToInAppPlayer,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        modifier = Modifier.testTag("try_embedded_player_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OndemandVideo,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("In-App", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun YouTubeCompanionFocusCard(
    lecture: Lecture,
    onOpenInYouTube: () -> Unit,
    onSwitchToInAppPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(lecture.thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xDD05080E))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0x226366F1), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = FocusIndigo,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Opened in Web Browser",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = lecture.title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenInYouTube,
                    colors = ButtonDefaults.buttonColors(containerColor = FocusIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reopen in Browser", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSwitchToInAppPlayer,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Try In-App Player")
                }
            }
        }
    }
}

@Composable
private fun PlayerHeader(
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
            // Fullscreen / Focus Mode Button
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
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen Focus Mode",
                        tint = FocusIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Fullscreen",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = FocusIndigo
                        )
                    )
                }
            }

            // Bookmark Button
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
private fun SpeedSelectorRow(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(end = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Speed:",
                style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
            )
        }

        listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
            val isSelected = currentSpeed == speed
            FilterChip(
                selected = isSelected,
                onClick = { onSpeedSelected(speed) },
                label = {
                    Text(
                        text = "${speed}x",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FocusIndigo,
                    containerColor = SlateCard
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
}

@Composable
private fun LectureMetadataBox(
    lecture: Lecture
) {
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
            if (lecture.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = lecture.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        lineHeight = 18.sp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
