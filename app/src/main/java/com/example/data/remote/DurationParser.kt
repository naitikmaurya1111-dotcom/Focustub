package com.example.data.remote

import java.util.regex.Pattern

object DurationParser {

    private val DURATION_PATTERN = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")

    /**
     * Parses ISO-8601 duration (e.g. PT1H25M30S, PT14M15S, PT45S) into formatted "HH:MM:SS" or "MM:SS"
     */
    fun parseIsoDurationToDisplay(isoDuration: String?): String {
        if (isoDuration.isNullOrBlank()) return "Lecture"
        val totalSec = parseIsoDurationToSeconds(isoDuration)
        if (totalSec <= 0) return "Lecture"

        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    /**
     * Parses ISO-8601 duration into total seconds
     */
    fun parseIsoDurationToSeconds(isoDuration: String?): Int {
        if (isoDuration.isNullOrBlank()) return 0
        try {
            val matcher = DURATION_PATTERN.matcher(isoDuration)
            if (matcher.matches()) {
                val hours = matcher.group(1)?.toIntOrNull() ?: 0
                val minutes = matcher.group(2)?.toIntOrNull() ?: 0
                val seconds = matcher.group(3)?.toIntOrNull() ?: 0
                return (hours * 3600) + (minutes * 60) + seconds
            }
        } catch (_: Exception) {
            // fallback
        }
        return 0
    }
}
