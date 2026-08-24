package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusAmberLight
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.FocusIndigoLight
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.StudyTimerState

@Composable
fun StudyTimerWidget(
    timerState: StudyTimerState,
    onStart: (Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("study_timer_card"),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            1.dp,
            if (timerState.isRunning) FocusAmber else SlateBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (timerState.isRunning) FocusAmber.copy(alpha = 0.2f) else FocusIndigo.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (timerState.isRunning) FocusAmber else FocusIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Focus Session",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                if (timerState.isFinished) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(FocusEmerald.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = FocusEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Complete!",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else if (timerState.isRunning) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(FocusAmber.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusAmber,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Circular Progress Indicator & Countdown
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                // Pulse effect when running
                if (timerState.isRunning) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(pulseScale)
                            .border(BorderStroke(2.dp, FocusAmber.copy(alpha = pulseAlpha)), CircleShape)
                    )
                }

                Canvas(modifier = Modifier.size(200.dp)) {
                    // Track
                    drawArc(
                        color = Color(0xFF131B2E),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Progress
                    val progressAngle = 360f * timerState.progressFraction
                    drawArc(
                        color = if (timerState.isRunning) FocusAmber else FocusIndigo,
                        startAngle = -90f,
                        sweepAngle = progressAngle,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                if (timerState.isFinished) {
                    // Celebration state
                    Box(contentAlignment = Alignment.Center) {
                        // Confetti
                        Box(modifier = Modifier.offset(x = (-40).dp, y = (-40).dp).size(8.dp).clip(CircleShape).background(FocusAmber))
                        Box(modifier = Modifier.offset(x = (45).dp, y = (-25).dp).size(6.dp).clip(CircleShape).background(FocusIndigoLight))
                        Box(modifier = Modifier.offset(x = (-30).dp, y = (45).dp).size(10.dp).clip(CircleShape).background(FocusEmerald))
                        Box(modifier = Modifier.offset(x = (35).dp, y = (40).dp).size(7.dp).clip(CircleShape).background(Color(0xFFE2E8F0)))
                        
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Session Complete",
                            tint = FocusEmerald,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                } else {
                    Text(
                        text = timerState.formattedTime,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (timerState.isRunning) FocusAmber else TextPrimary,
                            shadow = Shadow(
                                color = (if (timerState.isRunning) FocusAmber else FocusIndigo).copy(alpha = 0.5f),
                                blurRadius = 20f
                            )
                        ),
                        modifier = Modifier.testTag("timer_countdown_text")
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Duration Presets
            if (!timerState.isRunning && !timerState.isFinished) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    listOf(
                        15 to "15m",
                        25 to "25m",
                        50 to "50m"
                    ).forEach { (mins, label) ->
                        val selected = timerState.initialMinutes == mins
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) FocusAmber else Color(0xFF131B2E))
                                .border(
                                    BorderStroke(1.dp, if (selected) Color.Transparent else SlateBorder),
                                    CircleShape
                                )
                                .clickable { onReset(mins) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color.Black else TextSecondary
                                )
                            )
                        }
                    }
                }
            } else if (timerState.isFinished) {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (timerState.isRunning) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(FocusAmber, FocusAmberLight)))
                            .clickable { onPause() }
                            .padding(horizontal = 32.dp, vertical = 16.dp)
                            .testTag("timer_pause_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pause",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    val isStart = timerState.remainingSeconds == timerState.initialMinutes * 60
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(FocusIndigo, FocusIndigoLight)))
                            .clickable { if (isStart) onStart(timerState.initialMinutes) else onResume() }
                            .padding(horizontal = 32.dp, vertical = 16.dp)
                            .testTag("timer_start_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = if (isStart) "Start" else "Resume",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isStart) "Start Session" else "Resume",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onReset(timerState.initialMinutes) },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    modifier = Modifier.testTag("timer_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset", color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    }
}
