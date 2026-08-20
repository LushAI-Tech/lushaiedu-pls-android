package com.lushaiedupls.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TeacherOverview(
    val teacher: UserSummary,
    val month: String,
    val student_count: Int = 0,
    val regular: AttendanceTotals,
    val extra: AttendanceTotals,
    val units: List<TeacherUnitTile> = emptyList(),
    val top_performers: List<TopPerformer> = emptyList(),
    val ai_available: Boolean = true,
    val unread_notifications: Int = 0,
)

@Serializable
data class TeacherUnitTile(
    val teaching_unit_id: String,
    val class_name: String,
    val class_id: String? = null,
    val subject_name: String,
    val student_count: Int = 0,
    val regular: AttendanceTotals,
    val extra: AttendanceTotals,
)

@Serializable
data class TopPerformer(
    val student: UserSummary,
    val combined_score: Double = 0.0,
    val quiz_avg_pct: Double? = null,
    val attendance_pct: Double? = null,
    val progress_pct: Double? = null,
    val components_used: List<String> = emptyList(),
)

@Serializable
data class UnitAttendanceSummary(
    val teaching_unit_id: String,
    val class_name: String,
    val subject_name: String,
    val month: String,
    val regular: AttendanceTotals,
    val extra: AttendanceTotals,
    val students: List<StudentRate> = emptyList(),
)

@Serializable
data class StudentRate(
    val student: UserSummary,
    val totals: AttendanceTotals,
)

@Serializable
data class PeriodCard(
    val period_id: String,
    val period_name: String,
    val start_time: String,
    val end_time: String,
    val teaching_unit_id: String,
    val class_name: String,
    val subject_name: String,
    val is_marked: Boolean = false,
    val roll_id: String? = null,
    val present: Int = 0,
    val absent: Int = 0,
    val leave: Int = 0,
)

@Serializable
data class DayView(
    val attendance_date: String,
    val periods: List<PeriodCard> = emptyList(),
    val extra_classes: List<RollOut> = emptyList(),
)

@Serializable
data class RosterStudent(
    val student: UserSummary,
    val roll_no: Int? = null,
    val status: AttendanceStatus? = null,
)

@Serializable
data class RosterResponse(
    val teaching_unit_id: String,
    val class_name: String,
    val subject_name: String,
    val attendance_date: String,
    val period_id: String? = null,
    val time_key: String,
    val is_extra_class: Boolean = false,
    val is_marked: Boolean = false,
    val students: List<RosterStudent> = emptyList(),
)

@Serializable
data class EntryInput(
    val student_id: String,
    val status: AttendanceStatus,
)

@Serializable
data class EntryOut(
    val student: UserSummary,
    val roll_no: Int? = null,
    val status: AttendanceStatus,
)

@Serializable
data class RollOut(
    val id: String,
    val teaching_unit_id: String,
    val class_name: String,
    val subject_name: String,
    val attendance_date: String,
    val period_id: String? = null,
    val period_name: String? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val is_extra_class: Boolean = false,
    val time_key: String,
    val marked_by: String? = null,
    val entries: List<EntryOut> = emptyList(),
)

@Serializable
data class UpsertRollRequest(
    val teaching_unit_id: String,
    val attendance_date: String,
    val period_id: String? = null,
    val is_extra_class: Boolean = false,
    val extra_label: String? = null,
    val entries: List<EntryInput>,
)

@Serializable
data class MemberOut(
    val id: String,
    val student: UserSummary,
    val roll_no: Int? = null,
    val roll_status: RollStatus = RollStatus.UNASSIGNED,
    val joined_at: String,
)

@Serializable
data class AddMemberRequest(
    val student_id: String,
)

@Serializable
data class RollNumberAssignment(
    val student_id: String,
    val roll_no: Int,
)

@Serializable
data class SetRollNumbersRequest(
    val assignments: List<RollNumberAssignment>,
)

@Serializable
data class ApproveRollNumbersRequest(
    val student_ids: List<String>? = null,
)

@Serializable
data class TeachingUnitUpdate(
    val teacher_id: String? = null,
    val status: TeachingUnitStatus? = null,
)

@Serializable
data class NotificationCreate(
    val title: String,
    val body: String,
    val audience: NotificationAudience = NotificationAudience.ALL,
    val teaching_unit_id: String? = null,
    val expires_at: String? = null,
)

@Serializable
data class NotificationUpdate(
    val title: String? = null,
    val body: String? = null,
    val expires_at: String? = null,
)

@Serializable
data class SlotInput(
    val period_id: String,
    val day_of_week: DayOfWeek,
    val room: String? = null,
    val effective_from: String? = null,
    val effective_until: String? = null,
)

@Serializable
data class SetSlotsRequest(
    val slots: List<SlotInput>,
)

@Serializable
data class SlotOut(
    val id: String,
    val teaching_unit_id: String,
    val period_id: String,
    val period_name: String,
    val start_time: String,
    val end_time: String,
    val day_of_week: DayOfWeek,
    val room: String? = null,
)
