package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.ChapterParser
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.regex.Pattern

@Composable
fun NotesSection(
    notes: String,
    currentPlaybackSeconds: Int = 0,
    onNotesChanged: (String) -> Unit,
    onSeekTo: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Extract any timestamps mentioned in the notes text for quick jump chips
    val timestampPattern = remember { Pattern.compile("""\[?(\d{1,2}:\d{2}(?::\d{2})?)\]?""") }
    val extractedTimestamps = remember(notes) {
        val matches = mutableListOf<Pair<String, Int>>()
        val matcher = timestampPattern.matcher(notes)
        while (matcher.find()) {
            val str = matcher.group(1) ?: continue
            val sec = ChapterParser.parseTimestampToSeconds(str)
            if (sec > 0 && matches.none { it.second == sec }) {
                matches.add(Pair(str, sec))
            }
        }
        matches.take(6)
    }

    val currentFormattedTime = remember(currentPlaybackSeconds) {
        ChapterParser.formatSecondsToDisplay(currentPlaybackSeconds)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lecture_notes_card"),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with Save Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = FocusIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Study Notes & Key Takeaways",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = FocusEmerald,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Auto-saved",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusEmerald,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Formatting & Timestamp Insertion Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Insert current video timestamp
                SuggestionChip(
                    onClick = {
                        val stamp = "[$currentFormattedTime] "
                        val newNotes = if (notes.isBlank()) stamp else "$notes\n$stamp"
                        onNotesChanged(newNotes)
                    },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = FocusAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "+ $currentFormattedTime",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FocusAmber
                                )
                            )
                        }
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0x22F59E0B)
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = FocusAmber
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                // Key Concept Tag
                SuggestionChip(
                    onClick = {
                        val tag = "💡 Concept: "
                        val newNotes = if (notes.isBlank()) tag else "$notes\n$tag"
                        onNotesChanged(newNotes)
                    },
                    label = {
                        Text(
                            text = "💡 Concept",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFF131B2E)
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = SlateBorder
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                // Formula Tag
                SuggestionChip(
                    onClick = {
                        val tag = "📐 Formula: "
                        val newNotes = if (notes.isBlank()) tag else "$notes\n$tag"
                        onNotesChanged(newNotes)
                    },
                    label = {
                        Text(
                            text = "📐 Formula",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFF131B2E)
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = SlateBorder
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Clickable Jump-to-Timestamp Chips found in notes
            if (extractedTimestamps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Jump to:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    )
                    extractedTimestamps.forEach { (timeStr, seconds) ->
                        Box(
                            modifier = Modifier
                                .background(Color(0x336366F1), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "▶ $timeStr",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FocusIndigo,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier
                                    .testTag("jump_timestamp_$timeStr")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Editor Field
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChanged,
                placeholder = {
                    Text(
                        text = "Write your structured notes here...\nClick '+ Timestamp' above to link moments from the lecture.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            lineHeight = 18.sp
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .testTag("notes_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FocusIndigo,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = FocusIndigo,
                    focusedContainerColor = Color(0xFF131B2E),
                    unfocusedContainerColor = Color(0xFF131B2E)
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
