package com.lushaiedupls.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AttendanceStatus {
    @SerialName("PRESENT") PRESENT,
    @SerialName("ABSENT") ABSENT,
    @SerialName("LEAVE") LEAVE,
}

@Serializable
enum class CalendarEventType {
    @SerialName("HOLIDAY") HOLIDAY,
    @SerialName("EXAM") EXAM,
    @SerialName("EVENT") EVENT,
}

@Serializable
enum class DayOfWeek {
    @SerialName("MON") MON,
    @SerialName("TUE") TUE,
    @SerialName("WED") WED,
    @SerialName("THU") THU,
    @SerialName("FRI") FRI,
    @SerialName("SAT") SAT,
    @SerialName("SUN") SUN,
}

@Serializable
enum class DevicePlatform {
    @SerialName("IOS") IOS,
    @SerialName("ANDROID") ANDROID,
    @SerialName("WEB") WEB,
}

@Serializable
enum class Gender {
    @SerialName("MALE") MALE,
    @SerialName("FEMALE") FEMALE,
    @SerialName("OTHER") OTHER,
}

@Serializable
enum class NotificationAudience {
    @SerialName("ALL") ALL,
    @SerialName("STUDENTS") STUDENTS,
    @SerialName("TEACHERS") TEACHERS,
    @SerialName("PARENTS") PARENTS,
    @SerialName("TEACHING_UNIT") TEACHING_UNIT,
}

@Serializable
enum class OnboardingState {
    @SerialName("ROLE_PENDING") ROLE_PENDING,
    @SerialName("PROFILE_PENDING") PROFILE_PENDING,
    @SerialName("COMPLETE") COMPLETE,
}

@Serializable
enum class ParentLinkStatus {
    @SerialName("ACTIVE") ACTIVE,
    @SerialName("REVOKED") REVOKED,
}

@Serializable
enum class ParentRelationship {
    @SerialName("GUARDIAN") GUARDIAN,
    @SerialName("FATHER") FATHER,
    @SerialName("MOTHER") MOTHER,
    @SerialName("OTHER") OTHER,
}

@Serializable
enum class UserRole {
    @SerialName("STUDENT") STUDENT,
    @SerialName("TEACHER") TEACHER,
    @SerialName("ADMIN") ADMIN,
    @SerialName("PARENT") PARENT,
}

@Serializable
enum class UserStatus {
    @SerialName("ACTIVE") ACTIVE,
    @SerialName("PENDING_APPROVAL") PENDING_APPROVAL,
    @SerialName("SUSPENDED") SUSPENDED,
    @SerialName("DELETED") DELETED,
}

@Serializable
enum class TeachingUnitStatus {
    @SerialName("ACTIVE") ACTIVE,
    @SerialName("ARCHIVED") ARCHIVED,
}

@Serializable
enum class RollStatus {
    @SerialName("UNASSIGNED") UNASSIGNED,
    @SerialName("PROPOSED") PROPOSED,
    @SerialName("APPROVED") APPROVED,
}
