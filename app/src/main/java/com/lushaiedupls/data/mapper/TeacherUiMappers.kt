package com.lushaiedupls.data.mapper

import com.lushaiedupls.data.mock.TeacherAnnouncement
import com.lushaiedupls.data.mock.TeacherAnnouncementAudience
import com.lushaiedupls.data.mock.TeacherAttendanceBlock
import com.lushaiedupls.data.mock.TeacherAttendanceMark
import com.lushaiedupls.data.mock.TeacherAttendanceSession
import com.lushaiedupls.data.mock.TeacherAttendanceStudent
import com.lushaiedupls.data.mock.TeacherClassOverview
import com.lushaiedupls.data.mock.TeacherClassStudentScore
import com.lushaiedupls.data.mock.TeacherGroup
import com.lushaiedupls.data.mock.TeacherGroupOutcome
import com.lushaiedupls.data.mock.TeacherHomeDashboard
import com.lushaiedupls.data.mock.TeacherOverviewDashboard
import com.lushaiedupls.data.mock.TeacherPerformance
import com.lushaiedupls.data.mock.TeacherStudent
import com.lushaiedupls.data.mock.TeacherTeachingTimetable
import com.lushaiedupls.data.mock.TeacherTimetableCell
import com.lushaiedupls.data.mock.TeacherVolumeRow
import com.lushaiedupls.data.remote.dto.AttendanceStatus
import com.lushaiedupls.data.remote.dto.AttendanceTotals
import com.lushaiedupls.data.remote.dto.DayOfWeek
import com.lushaiedupls.data.remote.dto.MemberOut
import com.lushaiedupls.data.remote.dto.NotificationAudience
import com.lushaiedupls.data.remote.dto.NotificationOut
import com.lushaiedupls.data.remote.dto.RosterResponse
import com.lushaiedupls.data.remote.dto.TeacherOverview
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.TeachingUnitStatus
import com.lushaiedupls.data.remote.dto.UnitAttendanceSummary
import com.lushaiedupls.data.remote.dto.UserSummary
import com.lushaiedupls.data.remote.dto.WeekSlot
import com.lushaiedupls.data.remote.dto.WeekView
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object TeacherUiMappers {
    private val monthLabelFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val sentAtFmt = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)

    fun groups(units: List<TeachingUnitOut>): List<TeacherGroup> =
        units.map { unit ->
            TeacherGroup(
                id = unit.id,
                title = groupTitle(unit.class_name, unit.subject_name),
                code = shortCode(unit.id),
                status = when (unit.status) {
                    TeachingUnitStatus.ACTIVE -> "Active"
                    TeachingUnitStatus.ARCHIVED -> "Archived"
                },
            )
        }

    fun classChips(units: List<TeachingUnitOut>): List<TeacherClassChip> {
        val seen = linkedSetOf<String>()
        return units.mapNotNull { unit ->
            val label = classLabel(unit.class_name)
            if (seen.add(unit.class_id)) {
                TeacherClassChip(classId = unit.class_id, label = label)
            } else {
                null
            }
        }
    }

    fun homeDashboard(
        overview: TeacherOverview,
        classLabel: String,
        classOptions: List<String>,
    ): TeacherHomeDashboard = TeacherHomeDashboard(
        selectedClass = classLabel,
        classes = classOptions,
        groupOutcome = TeacherGroupOutcome(
            studentsCount = overview.student_count,
            attendancePercent = overview.regular.present_pct_all.roundToInt(),
        ),
        regularAttendance = attendanceBlock(overview.regular),
        extraAttendance = attendanceBlock(overview.extra),
        topPerformances = overview.top_performers.map { row ->
            TeacherPerformance(
                studentName = row.student.name,
                percent = row.combined_score.roundToInt(),
            )
        },
    )

    fun overviewDashboard(
        overview: TeacherOverview,
        unitSummary: UnitAttendanceSummary? = null,
    ): TeacherOverviewDashboard {
        // Prefer unit regular; if that session block is empty but extras exist, include extras
        // so Overview reflects what the teacher just marked.
        val totals = when {
            unitSummary != null -> preferredTotals(unitSummary.regular, unitSummary.extra)
            else -> preferredTotals(overview.regular, overview.extra)
        }
        val byClassSource = unitSummary
        val firstUnit = overview.units.firstOrNull()
        val className = byClassSource?.class_name ?: firstUnit?.class_name.orEmpty()
        val subjectName = byClassSource?.subject_name ?: firstUnit?.subject_name.orEmpty()
        val classTotals = when {
            byClassSource != null -> preferredTotals(byClassSource.regular, byClassSource.extra)
            firstUnit != null -> preferredTotals(firstUnit.regular, firstUnit.extra)
            else -> totals
        }
        val subjectTotals = classTotals
        return TeacherOverviewDashboard(
            monthLabel = monthLabel(overview.month),
            presentRate = "${totals.present_pct_all.roundToInt()}%",
            totalSessions = totals.sessions.toString(),
            presentAbsentLeave = "${totals.present}/${totals.absent}/${totals.leave}",
            presentCount = totals.present,
            absentCount = totals.absent,
            leaveCount = totals.leave,
            presentPercent = totals.present_pct_all.roundToInt(),
            byClass = TeacherVolumeRow(
                label = classLabel(className).ifBlank { "Class" },
                detail = volumeDetail(classTotals),
                presentPercent = classTotals.present_pct_all.roundToInt(),
            ),
            bySubject = TeacherVolumeRow(
                label = subjectName.ifBlank { "Subject" },
                detail = volumeDetail(subjectTotals),
                presentPercent = subjectTotals.present_pct_all.roundToInt(),
            ),
        )
    }

    fun periodTimeLabel(startTime: String, endTime: String): String =
        formatPeriodRange(startTime, endTime)

    fun classOverview(
        unit: TeachingUnitOut,
        summary: UnitAttendanceSummary?,
        memberCount: Int = unit.student_count,
    ): TeacherClassOverview {
        val regular = summary?.regular
        val extra = summary?.extra
        val className = summary?.class_name ?: unit.class_name
        val subjectName = summary?.subject_name ?: unit.subject_name
        return TeacherClassOverview(
            groupId = unit.id,
            title = groupTitle(className, subjectName),
            orgLabel = "LushaiEdu",
            studentCount = memberCount.takeIf { it > 0 } ?: unit.student_count,
            averageAttendancePercent = regular?.present_pct_all?.roundToInt() ?: 0,
            periodLabel = periodLabel(summary?.month),
            regularAttendance = attendanceBlock(regular),
            extraAttendance = attendanceBlock(extra),
            studentScores = summary?.students.orEmpty()
                .sortedByDescending { it.totals.present_pct_all }
                .map { row ->
                    val totals = row.totals
                    TeacherClassStudentScore(
                        name = row.student.name,
                        percent = totals.present_pct_all.roundToInt(),
                        avatarUrl = row.student.avatar_url,
                        detail = "${totals.present}/${totals.absent}/${totals.leave} · ${totals.sessions} sessions",
                    )
                },
        )
    }

    fun students(members: List<MemberOut>, parentIds: Set<String> = emptySet()): List<TeacherStudent> {
        val sorted = members.sortedWith(
            compareBy<MemberOut> { it.roll_no?.takeIf { roll -> roll > 0 } ?: Int.MAX_VALUE }
                .thenBy { it.student.name }
        )
        return sorted.mapIndexed { index, member ->
            TeacherStudent(
                id = member.student.id,
                name = member.student.name,
                email = member.student.email.orEmpty(),
                rollNumber = member.roll_no?.takeIf { it > 0 } ?: (index + 1),
                hasParentsSelected = member.student.id in parentIds,
            )
        }
    }

    fun attendanceSession(roster: RosterResponse): TeacherAttendanceSession {
        val sortedStudents = roster.students.sortedWith(
            compareBy<com.lushaiedupls.data.remote.dto.RosterStudent> { it.roll_no?.takeIf { roll -> roll > 0 } ?: Int.MAX_VALUE }
                .thenBy { it.student.name }
        )
        return TeacherAttendanceSession(
            classLabel = classLabel(roster.class_name),
            dateLabel = roster.attendance_date,
            subjectLabel = roster.subject_name,
            timeLabel = roster.time_key.ifBlank {
                if (roster.is_extra_class) "Extra class" else ""
            },
            students = sortedStudents.mapIndexed { index, row ->
                TeacherAttendanceStudent(
                    student = TeacherStudent(
                        id = row.student.id,
                        name = row.student.name,
                        email = row.student.email.orEmpty(),
                        rollNumber = row.roll_no?.takeIf { it > 0 } ?: (index + 1),
                    ),
                    mark = row.status.toMark(),
                )
            },
        )
    }

    fun announcementAudiences(units: List<TeachingUnitOut>): List<TeacherAnnouncementAudience> {
        val unitRows = units.map { unit ->
            TeacherAnnouncementAudience(
                id = unit.id,
                title = groupTitle(unit.class_name, unit.subject_name),
                subtitle = "${unit.student_count} students",
            )
        }
        return listOf(
            TeacherAnnouncementAudience(
                id = "all",
                title = "Select all classes",
                subtitle = "Applies to all listed classes.",
                isSelectAll = true,
            ),
        ) + unitRows
    }

    fun announcements(items: List<NotificationOut>): List<TeacherAnnouncement> =
        items.map { n ->
            TeacherAnnouncement(
                id = n.id,
                subject = n.title,
                body = n.body,
                priority = n.audience.label(),
                audienceLabel = n.teaching_unit_label ?: n.audience.label(),
                sentAtLabel = formatDateTime(n.published_at),
            )
        }

    fun teachingTimetable(week: WeekView): TeacherTeachingTimetable {
        val periods = week.periods.filter { it.is_active }.sortedBy { it.sort_order }
        val dayOrder = listOf(
            DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED, DayOfWeek.THU, DayOfWeek.FRI, DayOfWeek.SAT,
        )
        val dayLabels = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val allSlots = week.days.values.flatten()
        val classes = allSlots.map { classLabel(it.class_name) }.distinct().ifEmpty { listOf("All classes") }
        val timeSlots = periods.map { formatPeriodRange(it.start_time, it.end_time) }
            .ifEmpty { listOf("—") }
        val cells = mutableMapOf<Pair<Int, Int>, TeacherTimetableCell>()
        periods.forEachIndexed { row, period ->
            dayOrder.forEachIndexed { col, day ->
                val slot = slotsForDay(week, day).firstOrNull { it.period_id == period.id }
                    ?: return@forEachIndexed
                cells[row to col] = TeacherTimetableCell(
                    subject = slot.subject_name,
                    detail = groupTitle(slot.class_name, slot.subject_name),
                )
            }
        }
        return TeacherTeachingTimetable(
            classes = classes,
            days = dayLabels,
            timeSlots = timeSlots,
            cells = cells,
        )
    }

    fun timetableSubjects(week: WeekView): List<String> =
        week.days.values.flatten().map { it.subject_name }.distinct().sorted()
            .ifEmpty { emptyList() }

    fun attendanceMarkToStatus(mark: TeacherAttendanceMark): AttendanceStatus? = when (mark) {
        TeacherAttendanceMark.Present -> AttendanceStatus.PRESENT
        TeacherAttendanceMark.Absent -> AttendanceStatus.ABSENT
        TeacherAttendanceMark.None -> null
    }

    fun parentIds(parents: List<UserSummary>): Set<String> = parents.map { it.id }.toSet()

    private fun AttendanceStatus?.toMark(): TeacherAttendanceMark = when (this) {
        AttendanceStatus.PRESENT -> TeacherAttendanceMark.Present
        AttendanceStatus.ABSENT, AttendanceStatus.LEAVE -> TeacherAttendanceMark.Absent
        null -> TeacherAttendanceMark.None
    }

    private fun preferredTotals(regular: AttendanceTotals, extra: AttendanceTotals): AttendanceTotals {
        return when {
            regular.sessions > 0 -> regular
            extra.sessions > 0 -> extra
            regular.present + regular.absent + regular.leave > 0 -> regular
            extra.present + extra.absent + extra.leave > 0 -> extra
            else -> regular
        }
    }

    private fun attendanceBlock(totals: AttendanceTotals?): TeacherAttendanceBlock {
        val value = totals ?: AttendanceTotals()
        return TeacherAttendanceBlock(
            presentRate = "${value.present_pct_all.roundToInt()}%",
            totalSessions = value.sessions.toString(),
            presentAbsentLeave = "${value.present}/${value.absent}/${value.leave}",
        )
    }

    private fun volumeDetail(totals: AttendanceTotals): String =
        "${totals.sessions} sessions · ${totals.present_pct_all.roundToInt()}% present"

    private fun groupTitle(className: String, subjectName: String): String {
        val cls = classLabel(className)
        return if (subjectName.isBlank()) cls else "$cls ($subjectName)"
    }

    private fun classLabel(className: String): String {
        val trimmed = className.trim()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.startsWith("Class", ignoreCase = true)) trimmed else "Class $trimmed"
    }

    private fun shortCode(id: String): String =
        id.filter { it.isLetterOrDigit() }.takeLast(6).uppercase(Locale.ENGLISH).ifBlank { "GROUP" }

    private fun monthLabel(month: String): String {
        val parsed = runCatching { YearMonth.parse(month) }.getOrNull() ?: return month
        return parsed.format(monthLabelFmt)
    }

    private fun periodLabel(month: String?): String {
        val value = month?.trim().orEmpty()
        if (value.isEmpty() || value.equals("all", ignoreCase = true)) return "All time"
        return monthLabel(value)
    }

    private fun formatDateTime(raw: String): String {
        val odt = runCatching { OffsetDateTime.parse(raw) }.getOrNull()
        return odt?.format(sentAtFmt) ?: raw
    }

    private fun NotificationAudience.label(): String = when (this) {
        NotificationAudience.ALL -> "All"
        NotificationAudience.STUDENTS -> "Students"
        NotificationAudience.TEACHERS -> "Teachers"
        NotificationAudience.PARENTS -> "Parents"
        NotificationAudience.TEACHING_UNIT -> "Class"
    }

    private fun slotsForDay(week: WeekView, day: DayOfWeek): List<WeekSlot> {
        val keys = listOf(day.name, day.name.lowercase(Locale.ENGLISH), day.name.take(3))
        return keys.flatMap { week.days[it].orEmpty() }.distinctBy { it.slot_id }
            .ifEmpty { week.days.values.flatten().filter { it.day_of_week == day } }
    }

    private fun formatPeriodRange(start: String, end: String): String {
        val startLabel = formatClock(start)
        val endLabel = formatClock(end)
        return if (startLabel != null && endLabel != null) "$startLabel - $endLabel" else "$start - $end"
    }

    private fun formatClock(raw: String): String? {
        val cleaned = raw.trim().take(8)
        val parts = cleaned.split(":")
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        val amPm = if (hour < 12) "AM" else "PM"
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.ENGLISH, "%d:%02d %s", h12, minute, amPm)
    }
}

data class TeacherClassChip(
    val classId: String,
    val label: String,
)
