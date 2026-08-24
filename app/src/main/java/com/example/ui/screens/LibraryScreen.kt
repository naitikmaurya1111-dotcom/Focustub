package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditNote
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lecture
import com.example.ui.components.LectureCard
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
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
            // Header
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Study Syllabus & Library",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        modifier = Modifier.testTag("library_screen_title")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your curated syllabus of saved lectures, active notes, and learning progress.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                }
            }

            // Quick Filter & Search inside Library
            if (savedLectures.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Search bar inside library
                        OutlinedTextField(
                            value = librarySearchQuery,
                            onValueChange = { librarySearchQuery = it },
                            placeholder = {
                                Text(
                                    text = "Filter saved lectures or your study notes...",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = FocusIndigo,
                                    modifier = Modifier.size(18.dp)
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
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SlateCard,
                                unfocusedContainerColor = SlateCard,
                                focusedBorderColor = FocusIndigo,
                                unfocusedBorderColor = SlateBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = FocusIndigo
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Filter Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            listOf(
                                LibraryFilter.ALL to "All (${savedLectures.size})",
                                LibraryFilter.IN_PROGRESS to "In Progress (${savedLectures.count { !it.isCompleted && it.progressSeconds > 0 }})",
                                LibraryFilter.COMPLETED to "Completed (${savedLectures.count { it.isCompleted }})",
                                LibraryFilter.HAS_NOTES to "With Notes (${savedLectures.count { it.notes.isNotBlank() }})"
                            ).forEach { (filter, label) ->
                                val isSelected = selectedFilter == filter
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFilter = filter },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium.copy(
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
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Content List
            if (filteredLectures.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (savedLectures.isEmpty()) "No saved lectures yet" else "No lectures match this filter",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (savedLectures.isEmpty())
                                "Search for lectures and bookmark them to construct your personalized academic syllabus."
                            else
                                "Clear filters or search query to view all your saved educational videos.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        if (savedLectures.isEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onNavigateToSearch,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FocusIndigo,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("explore_lectures_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Find Lectures", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(filteredLectures, key = { it.videoId }) { lecture ->
                    LectureCard(
                        lecture = lecture,
                        onClick = { onLectureSelected(lecture) },
                        onToggleSave = { onToggleSaveLecture(lecture) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
