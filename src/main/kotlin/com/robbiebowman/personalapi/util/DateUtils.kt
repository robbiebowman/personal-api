package com.robbiebowman.personalapi.util

import java.time.Duration
import java.util.regex.Matcher
import java.util.regex.Pattern

object DateUtils {
    // Thank you Andreas
    // https://stackoverflow.com/a/52230282/1256019
    fun parseHuman(text: String): Duration {
        val m: Matcher = Pattern.compile(
            "\\s*(?:(\\d+)\\s*(?:days?|d))?" +
                    "\\s*(?:(\\d+)\\s*(?:hours?|hrs?|h))?" +
                    "\\s*(?:(\\d+)\\s*(?:minutes?|mins?|m))?" +
                    "\\s*(?:(\\d+)\\s*(?:seconds?|secs?|s))?" +
                    "\\s*", Pattern.CASE_INSENSITIVE
        )
            .matcher(text)
        if (!m.matches()) throw IllegalArgumentException("Not valid duration: $text")
        val days = (if (m.start(1) == -1) 0 else m.group(1).toInt())
        val hours = (if (m.start(2) == -1) 0 else m.group(2).toInt())
        val mins = (if (m.start(3) == -1) 0 else m.group(3).toInt())
        val secs = (if (m.start(4) == -1) 0 else m.group(4).toInt())
        return Duration.ofSeconds(((days * 24 + hours) * 60L + mins) * 60L + secs)
    }

    fun durationToHuman(duration: Duration): String {
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60

        val daysStr = if (days == 0L) "" else if (days == 1L) "1 day" else "$days days"
        val hoursStr = if (hours == 0L) "" else if (hours == 1L) "1 hour" else "$hours hours"
        val minutesStr = if (minutes == 0L) "" else if (minutes == 1L) "1 minute" else "$minutes minutes"
        val allParts = listOf(daysStr, hoursStr, minutesStr).filter { it.isNotBlank() }
        return allParts.joinToString(", ")
    }
}