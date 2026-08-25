package com.lushaiedupls.ui.admin

import com.lushaiedupls.data.remote.dto.UserRole
import com.lushaiedupls.data.remote.dto.UserStatus
import com.lushaiedupls.data.remote.dto.Gender
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

fun normalizeTime(value: String): String {
    val trimmed = value.trim()
    return when {
        trimmed.matches(Regex("""\d{2}:\d{2}:\d{2}""")) -> trimmed
        trimmed.matches(Regex("""\d{2}:\d{2}""")) -> "$trimmed:00"
        else -> trimmed
    }
}
