package com.lushaiedupls.ui.admin

import com.lushaiedupls.data.remote.dto.Gender
import com.lushaiedupls.data.remote.dto.PeriodOut
import com.lushaiedupls.data.remote.dto.UserRole
import com.lushaiedupls.data.remote.dto.UserStatus
import java.text.NumberFormat
import java.util.Locale

fun formatInrFromPaise(paise: Int): String {
    val rupees = paise / 100
    val formatted = NumberFormat.getNumberInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).format(rupees)
    return "₹$formatted"
}

fun formatIsoDate(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return value.take(10)
}

fun UserRole.label(): String = when (this) {
    UserRole.STUDENT -> "Student"
    UserRole.TEACHER -> "Teacher"
    UserRole.ADMIN -> "Admin"
    UserRole.PARENT -> "Parent"
}

fun UserStatus.label(): String = when (this) {
    UserStatus.ACTIVE -> "Active"
    UserStatus.PENDING_APPROVAL -> "Pending"
    UserStatus.SUSPENDED -> "Suspended"
    UserStatus.DELETED -> "Deleted"
}

fun Gender.label(): String = when (this) {
    Gender.MALE -> "Male"
    Gender.FEMALE -> "Female"
    Gender.OTHER -> "Other"
}

/** Formats hour (0–23) + minute as `9:30 AM`. */
fun formatHourMinute12h(hour24: Int, minute: Int): String {
    val amPm = if (hour24 < 12) "AM" else "PM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return String.format(Locale.ENGLISH, "%d:%02d %s", hour12, minute.coerceIn(0, 59), amPm)
}

/** Returns (hour24, minute) for picker state, or null if unparseable. */
fun parseHourMinute(raw: String): Pair<Int, Int>? {
    val minutes = parseTimeToMinutes(raw) ?: return null
    return (minutes / 60) to (minutes % 60)
}

/** Formats API times like "09:30:00" / "09:30" / "9:30 AM" → "9:30 AM". */
fun formatTime12h(raw: String): String {
    val parsed = parseTimeToMinutes(raw) ?: return raw.trim()
    return formatHourMinute12h(parsed / 60, parsed % 60)
}

fun formatPeriodRange12h(start: String, end: String): String =
    "${formatTime12h(start)} – ${formatTime12h(end)}"

/** Chronological list order by start time, then name. */
fun List<PeriodOut>.sortedPeriods(): List<PeriodOut> =
    sortedWith(
        compareBy(
            { parseTimeToMinutes(it.start_time) ?: Int.MAX_VALUE },
            { it.name.lowercase(Locale.ENGLISH) },
        ),
    )

/** Sort key derived from start time so periods stay ordered by the clock. */
fun sortOrderFromStartTime(raw: String): Int =
    parseTimeToMinutes(raw) ?: 0

/**
 * Normalizes UI time input to API `HH:mm:ss`.
 * Accepts 24h (`16:00`, `16:00:00`) and 12h (`4:00 PM`, `04:00pm`).
 */
fun normalizeTime(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return trimmed
    val minutes = parseTimeToMinutes(trimmed)
    if (minutes != null) {
        val hour = minutes / 60
        val minute = minutes % 60
        return String.format(Locale.ENGLISH, "%02d:%02d:00", hour, minute)
    }
    return when {
        trimmed.matches(Regex("""\d{2}:\d{2}:\d{2}""")) -> trimmed
        trimmed.matches(Regex("""\d{2}:\d{2}""")) -> "$trimmed:00"
        else -> trimmed
    }
}

private fun parseTimeToMinutes(raw: String): Int? {
    val cleaned = raw.trim().uppercase(Locale.ENGLISH).replace('.', ':')
    val twelveHour = Regex(
        """^(\d{1,2}):(\d{2})(?::\d{2})?\s*(AM|PM)$""",
    ).matchEntire(cleaned)
    if (twelveHour != null) {
        var hour = twelveHour.groupValues[1].toIntOrNull() ?: return null
        val minute = twelveHour.groupValues[2].toIntOrNull() ?: return null
        val meridiem = twelveHour.groupValues[3]
        if (hour !in 1..12 || minute !in 0..59) return null
        hour = when {
            meridiem == "AM" && hour == 12 -> 0
            meridiem == "PM" && hour != 12 -> hour + 12
            else -> hour
        }
        return hour * 60 + minute
    }
    val twentyFour = Regex(
        """^(\d{1,2}):(\d{2})(?::\d{2})?$""",
    ).matchEntire(cleaned)
    if (twentyFour != null) {
        val hour = twentyFour.groupValues[1].toIntOrNull() ?: return null
        val minute = twentyFour.groupValues[2].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }
    return null
}
