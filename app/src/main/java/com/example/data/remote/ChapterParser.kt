package com.example.data.remote

import com.example.data.model.LectureChapter
import java.util.regex.Pattern

object ChapterParser {

    // Matches timestamps like 00:00, 0:00, 1:23:45, [02:30], (05:12) followed by title
    private val TIMESTAMP_LINE_PATTERN = Pattern.compile(
        """(?:^|\n)\s*[\[\(]?(\d{1,2}:\d{2}(?::\d{2})?)[\]\)]?\s*[-–—:]?\s*(.+)"""
    )

    fun parseChapters(description: String): List<LectureChapter> {
        if (description.isBlank()) return emptyList()

        val chapters = mutableListOf<LectureChapter>()
        val lines = description.lines()

        for (line in lines) {
            val trimmed = line.trim()
            val matcher = TIMESTAMP_LINE_PATTERN.matcher(trimmed)
            if (matcher.find()) {
                val timeStr = matcher.group(1)?.trim().orEmpty()
                val rawTitle = matcher.group(2)?.trim().orEmpty()
                    .replace(Regex("""^[-\–—:\s]+"""), "") // strip leading dashes or colons
                    .take(80)

                val seconds = parseTimestampToSeconds(timeStr)
                if (seconds >= 0 && rawTitle.isNotBlank()) {
                    chapters.add(
                        LectureChapter(
                            title = rawTitle,
                            startSeconds = seconds,
                            displayTimestamp = timeStr
                        )
                    )
                }
            }
        }

        return chapters.distinctBy { it.startSeconds }.sortedBy { it.startSeconds }
    }

    fun parseTimestampToSeconds(timeStr: String): Int {
        val parts = timeStr.split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            2 -> (parts[0] * 60) + parts[1]
            3 -> (parts[0] * 3600) + (parts[1] * 60) + parts[2]
            else -> 0
        }
    }

    fun formatSecondsToDisplay(totalSeconds: Int): String {
        if (totalSeconds < 0) return "00:00"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
