package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lecture
import com.example.ui.components.LectureCard
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusAmberLight
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.FocusIndigoLight
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateMutedBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class LibraryFilter {
    ALL,
    IN_PROGRESS,
    COMPLETED,
    HAS_NOTES
}

@Composable
fun LibraryScreen(
    savedLectures: List<Lecture>,
    onLectureSelected: (Lecture) -> Unit,
    onToggleSaveLecture: (Lecture) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var librarySearchQuery by remember { mutableStateOf("") }

    val filteredLectures = remember(savedLectures, selectedFilter, librarySearchQuery) {
        val byFilter = when (selectedFilter) {
            LibraryFilter.ALL -> savedLectures
            LibraryFilter.IN_PROGRESS -> savedLectures.filter { !it.isCompleted && it.progressSeconds > 0 }
            LibraryFilter.COMPLETED -> savedLectures.filter { it.isCompleted }
            LibraryFilter.HAS_NOTES -> savedLectures.filter { it.notes.isNotBlank() }
        }

        if (librarySearchQuery.isBlank()) {
            byFilter
        } else {
            byFilter.filter {
                it.title.contains(librarySearchQuery, ignoreCase = true) ||
                it.channelTitle.contains(librarySearchQuery, ignoreCase = true) ||
                it.notes.contains(librarySearchQuery, ignoreCase = true)
            }
        }
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Premium Header
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    brush = Brush.linearGradient(listOf(FocusIndigoLight, FocusIndigo)),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Library",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            modifier = Modifier.testTag("library_screen_title")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .background(FocusIndigo.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${savedLectures.size}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = FocusIndigoLight,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your curated syllabus of saved lectures, active notes, and learning progress.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )

                    // Stats mini-bar
                    if (savedLectures.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            StatPill(
                                label = "Total",
                                count = savedLectures.size,
                                color = FocusIndigo
                            )
                            StatPill(
                                label = "In Progress",
                                count = savedLectures.count { !it.isCompleted && it.progressSeconds > 0 },
                                color = FocusAmber
                            )
                            StatPill(
                                label = "Completed",
                                count = savedLectures.count { it.isCompleted },
                                color = FocusEmerald
                            )
                        }
                    }
                }
            }

            // Quick Filter & Search inside Library with AnimatedVisibility
            item {
                AnimatedVisibility(
                    visible = savedLectures.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Premium Animated Search Bar
                        val interactionSource = remember { MutableInteractionSource() }
                        val isFocused by interactionSource.collectIsFocusedAsState()
                        val glowAlpha by animateFloatAsState(
                            targetValue = if (isFocused) 1f else 0f,
                            label = "search_glow"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = if (isFocused) FocusIndigo else SlateBorder,
                            label = "search_border"
                        )

                        OutlinedTextField(
                            value = librarySearchQuery,
                            onValueChange = { librarySearchQuery = it },
                            placeholder = {
                                Text(
                                    text = "Search lectures, channels, or notes...",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (isFocused) FocusIndigo else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (librarySearchQuery.isNotBlank()) {
                                    IconButton(onClick = { librarySearchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear filter",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SlateCard,
                                unfocusedContainerColor = SlateCard,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = FocusIndigo
                            ),
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isFocused) 2.dp else 1.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(14.dp)
                                )
                        )

                        // Animated Filter Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp)
                        ) {
                            listOf(
                                LibraryFilter.ALL to "All",
                                LibraryFilter.IN_PROGRESS to "In Progress",
                                LibraryFilter.COMPLETED to "Completed",
                                LibraryFilter.HAS_NOTES to "With Notes"
                            ).forEach { (filter, label) ->
                                val isSelected = selectedFilter == filter
                                val chipBgColor by animateColorAsState(
                                    targetValue = if (isSelected) FocusIndigo else SlateCard,
                                    label = "chip_bg"
                                )
                                val chipBorderColor by animateColorAsState(
                                    targetValue = if (isSelected) FocusIndigo else SlateMutedBorder,
                                    label = "chip_border"
                                )
                                val chipTextColor by animateColorAsState(
                                    targetValue = if (isSelected) Color.White else TextSecondary,
                                    label = "chip_text"
                                )

                                Surface(
                                    onClick = { selectedFilter = filter },
                                    shape = RoundedCornerShape(12.dp),
                                    color = chipBgColor,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, chipBorderColor),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = chipTextColor
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Content List
            if (filteredLectures.isEmpty()) {
                item {
                    EmptyStatePremium(
                        isLibraryEmpty = savedLectures.isEmpty(),
                        onNavigateToSearch = onNavigateToSearch
                    )
                }
            } else {
                items(filteredLectures, key = { it.videoId }) { lecture ->
                    LectureCard(
                        lecture = lecture,
                        onClick = { onLectureSelected(lecture) },
                        onToggleSave = { onToggleSaveLecture(lecture) },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            // Generous bottom spacing for nav bar clearance
            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun StatPill(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.labelSmall.copy(
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun EmptyStatePremium(
    isLibraryEmpty: Boolean,
    onNavigateToSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(SlateCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = null,
                tint = TextMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        val gradientBrush = Brush.linearGradient(
            colors = listOf(FocusIndigoLight, FocusIndigo)
        )

        Text(
            text = if (isLibraryEmpty) "Your Library is Empty" else "No Matches Found",
            style = TextStyle(
                brush = gradientBrush,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isLibraryEmpty)
                "Search for lectures and bookmark them to construct your personalized academic syllabus."
            else
                "Try adjusting your filters or search query to find what you're looking for.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (isLibraryEmpty) {
            Spacer(modifier = Modifier.height(32.dp))

            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Button(
                onClick = onNavigateToSearch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FocusIndigo,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .scale(scale)
                    .height(48.dp)
                    .testTag("explore_lectures_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Explore Lectures", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
