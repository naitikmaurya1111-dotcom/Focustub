package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ApiKeySource
import com.example.data.repository.ApiKeyStatus
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    apiKeyStatus: ApiKeyStatus,
    isTestingApiKey: Boolean,
    apiKeyValidationResult: String?,
    keepScreenOn: Boolean,
    openInYouTubeDefault: Boolean,
    totalMinutesStudied: Int,
    savedCount: Int,
    defaultTimerMinutes: Int,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onTestApiKey: (String) -> Unit,
    onClearValidationResult: () -> Unit,
    onToggleKeepScreenOn: () -> Unit,
    onToggleOpenInYouTubeDefault: () -> Unit,
    onDefaultTimerChanged: (Int) -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var inputKey by remember(apiKeyStatus.activeKey) {
        mutableStateOf(if (apiKeyStatus.isCustomKeySet) apiKeyStatus.activeKey else "")
    }
    var isKeyVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x33EF4444)), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                    }
                    Text(
                        text = "Reset Study Library",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }
            },
            text = {
                Text(
                    text = "This will permanently remove all saved lectures, learning progress, custom API keys, and notes from this device. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, lineHeight = 22.sp),
                    modifier = Modifier.padding(top = 8.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        inputKey = ""
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear All Data", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = SlateCard,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Premium Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(FocusIndigo, Color(0xFF818CF8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                letterSpacing = (-0.5).sp
                            ),
                            modifier = Modifier.testTag("settings_screen_title")
                        )
                        Text(
                            text = "Configure your learning experience",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x226366F1))
                            .border(1.dp, Color(0x446366F1), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "v3.8",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = FocusIndigo,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Dual Stats Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedContent(
                                targetState = totalMinutesStudied,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
                                    } else {
                                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(slideOutVertically { height -> height } + fadeOut())
                                    }.using(SizeTransform(clip = false))
                                }, label = "totalMins"
                            ) { targetCount ->
                                Text(
                                    text = "$targetCount",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = FocusAmber
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Minutes Studied",
                                style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(60.dp)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, SlateBorder, Color.Transparent)))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedContent(
                                targetState = savedCount,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
                                    } else {
                                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(slideOutVertically { height -> height } + fadeOut())
                                    }.using(SizeTransform(clip = false))
                                }, label = "savedCount"
                            ) { targetCount ->
                                Text(
                                    text = "$targetCount",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = FocusEmerald
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Saved Lectures",
                                style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            // YouTube API v3 Key Configuration Box
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_api_key_card"),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(
                        1.dp,
                        if (apiKeyStatus.source != ApiKeySource.NONE) Color(0x556366F1) else SlateBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Frosted Glass Effect Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(Color(0x116366F1), Color.Transparent)))
                                .padding(horizontal = 24.dp, vertical = 20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x226366F1)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VpnKey,
                                            contentDescription = null,
                                            tint = FocusIndigo,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "YouTube Data API v3",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                }

                                // Active Status Indicator Pill
                                val (badgeBg, badgeColor, badgeText) = when (apiKeyStatus.source) {
                                    ApiKeySource.IN_APP_SETTINGS -> Triple(Color(0x2210B981), FocusEmerald, "Custom Key Active")
                                    ApiKeySource.ENVIRONMENT_CONFIG -> Triple(Color(0x226366F1), FocusIndigo, "Configured Key Active")
                                    ApiKeySource.NONE -> Triple(Color(0x22F59E0B), FocusAmber, "Curated Mode")
                                }

                                val isKeyActive = apiKeyStatus.source != ApiKeySource.NONE

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(badgeBg)
                                        .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .alpha(if (isKeyActive) pulseAlpha else 1f)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (isKeyActive) {
                                            Box(modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(badgeColor))
                                        }
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
                            Text(
                                text = "Add your own Google Cloud YouTube Data API v3 key to unlock unrestricted real-time search across all educational channels.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    lineHeight = 22.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Ultra-premium Input Field
                            OutlinedTextField(
                                value = inputKey,
                                onValueChange = { inputKey = it },
                                placeholder = {
                                    Text(
                                        text = if (apiKeyStatus.source == ApiKeySource.ENVIRONMENT_CONFIG) "Configured via Build Environment" else "Paste AIzaSy... YouTube API Key",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                                    )
                                },
                                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    onSaveApiKey(inputKey)
                                }),
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (inputKey.isNotBlank()) {
                                            IconButton(onClick = { inputKey = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Clear input",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                            Icon(
                                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (isKeyVisible) "Hide Key" else "Show Key",
                                                tint = FocusIndigo,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0D121F), // Darker for inner shadow feel
                                    unfocusedContainerColor = Color(0xFF0D121F),
                                    focusedBorderColor = FocusIndigo,
                                    unfocusedBorderColor = SlateBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = FocusIndigo
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("youtube_api_key_input")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Actions: Save, Test, Clear
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        onSaveApiKey(inputKey)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = FocusIndigo,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("save_api_key_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save Key", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        onTestApiKey(inputKey)
                                    },
                                    enabled = !isTestingApiKey,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, FocusIndigo),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FocusIndigo),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("test_api_key_button")
                                ) {
                                    if (isTestingApiKey) {
                                        CircularProgressIndicator(
                                            color = FocusIndigo,
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Test", fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                if (apiKeyStatus.isCustomKeySet) {
                                    OutlinedButton(
                                        onClick = {
                                            inputKey = ""
                                            onClearApiKey()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, SlateBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("clear_custom_key_button")
                                    ) {
                                        Text("Remove", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // Validation Feedback Banner
                            AnimatedVisibility(
                                visible = apiKeyValidationResult != null,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    val isSuccess = apiKeyValidationResult?.startsWith("Success") == true
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSuccess) Color(0x1F10B981) else Color(0x1FEF4444))
                                            .border(1.dp, if (isSuccess) Color(0x3310B981) else Color(0x33EF4444), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = if (isSuccess) FocusEmerald else Color(0xFFEF4444),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = apiKeyValidationResult ?: "",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = if (isSuccess) FocusEmerald else Color(0xFFEF4444),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        }
                                        IconButton(
                                            onClick = onClearValidationResult,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Dismiss",
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Screen On Behavior Setting
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x226366F1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = FocusIndigo,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Keep Screen Awake",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Prevents display from sleeping during active sessions.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                )
                            }
                        }

                        Switch(
                            checked = keepScreenOn,
                            onCheckedChange = { onToggleKeepScreenOn() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = FocusIndigo,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color(0xFF131B2E)
                            ),
                            modifier = Modifier.testTag("keep_screen_on_switch")
                        )
                    }
                }
            }

            // Open in YouTube Default Setting
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x2210B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OndemandVideo,
                                    contentDescription = null,
                                    tint = FocusEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Open in YouTube Default",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Launch lectures directly in the YouTube app.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                )
                            }
                        }

                        Switch(
                            checked = openInYouTubeDefault,
                            onCheckedChange = { onToggleOpenInYouTubeDefault() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = FocusEmerald,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color(0xFF131B2E)
                            )
                        )
                    }
                }
            }

            // Default Study Timer Length
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22F59E0B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = FocusAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Study Timer Preset",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Choose your preferred session length.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(15 to "15m Sprint", 25 to "25m Pomo", 50 to "50m Deep").forEach { (mins, label) ->
                                val isSelected = defaultTimerMinutes == mins
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onDefaultTimerChanged(mins) },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else TextSecondary
                                            ),
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FocusAmber,
                                        containerColor = Color(0xFF131B2E)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Clear Data
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1517)), // Subtle red tint
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0x33EF4444))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22EF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Danger Zone",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFCA5A5)
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Clear saved lectures, progress, and local data.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showConfirmDialog = true },
                            border = BorderStroke(1.dp, Color(0x88EF4444)),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0x11EF4444)),
                            modifier = Modifier.testTag("clear_data_button")
                        ) {
                            Text("Clear All", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Philosophy manifesto
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0x226366F1))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(Color(0xFF131B2E), Color(0xFF0F1423))))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x226366F1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = FocusIndigo,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "“The student opens FocusTube, watches the lecture they came for, takes structured notes, and studies in peace.”",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = TextPrimary,
                                    lineHeight = 26.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "Pure academic focus with zero algorithmic distractions.",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = FocusIndigo,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
