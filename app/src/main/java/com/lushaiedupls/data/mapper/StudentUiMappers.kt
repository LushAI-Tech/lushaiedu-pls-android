package com.lushaiedupls.data.mapper

import com.lushaiedupls.R
import com.lushaiedupls.data.mock.AcademicEventType
import com.lushaiedupls.data.mock.AiChatMessage
import com.lushaiedupls.data.mock.AiHubStat
import com.lushaiedupls.data.mock.AiMenuContentItem
import com.lushaiedupls.data.mock.AiQuickCheck
import com.lushaiedupls.data.mock.AiQuizHistoryItem
import com.lushaiedupls.data.mock.AiSubjectItem
import com.lushaiedupls.data.mock.AiSyllabusItem
import com.lushaiedupls.data.mock.AppNotification
import com.lushaiedupls.data.mock.AttendanceDashboard
import com.lushaiedupls.data.mock.AttendanceDayMark
import com.lushaiedupls.data.mock.AttendanceRecord
import com.lushaiedupls.data.mock.AttendanceSession
import com.lushaiedupls.data.mock.AttendanceStat
import com.lushaiedupls.data.mock.AttendanceStatus as UiAttendanceStatus
import com.lushaiedupls.data.mock.CalendarEvent
import com.lushaiedupls.data.mock.ChapterItem
import com.lushaiedupls.data.mock.NotificationSection
import com.lushaiedupls.data.mock.OverviewIcon
import com.lushaiedupls.data.mock.OverviewMetric
import com.lushaiedupls.data.mock.QuizQuestion
import com.lushaiedupls.data.mock.RegisteredDevice
import com.lushaiedupls.data.mock.SessionSummary
import com.lushaiedupls.data.mock.SubjectAttendanceRow
import com.lushaiedupls.data.mock.SubjectChapterStats
import com.lushaiedupls.data.mock.WeeklyTimetable
import com.lushaiedupls.data.remote.dto.AiSubjectOut
import com.lushaiedupls.data.remote.dto.AttendanceCalendar
import com.lushaiedupls.data.remote.dto.AttendanceStatus
import com.lushaiedupls.data.remote.dto.CalendarEventOut
import com.lushaiedupls.data.remote.dto.CalendarEventType
import com.lushaiedupls.data.remote.dto.ChapterListItem
import com.lushaiedupls.data.remote.dto.ChatMessage
import com.lushaiedupls.data.remote.dto.ChatResponse
import com.lushaiedupls.data.remote.dto.ConceptCheck
import com.lushaiedupls.data.remote.dto.ContentBlockOut
import com.lushaiedupls.data.remote.dto.DayOfWeek
import com.lushaiedupls.data.remote.dto.DeviceOut
import com.lushaiedupls.data.remote.dto.DevicePlatform
import com.lushaiedupls.data.remote.dto.NotificationOut
import com.lushaiedupls.data.remote.dto.ProgressDashboardResponse
import com.lushaiedupls.data.remote.dto.QuizAttemptSummary
import com.lushaiedupls.data.remote.dto.QuizQuestionOut
import com.lushaiedupls.data.remote.dto.SectionOut
import com.lushaiedupls.data.remote.dto.StudentAttendanceSummary
import com.lushaiedupls.data.remote.dto.StudentOverview
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.TeachingUnitStatus
import com.lushaiedupls.data.remote.dto.WeekSlot
import com.lushaiedupls.data.remote.dto.WeekView
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

object StudentUiMappers {
    private val monthDayFmt = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
    private val sessionDateFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
    private val deviceFmt = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)
    private val attemptDateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val QUESTION_TYPE_MARKERS = listOf(
        "question",
        "exercise",
        "problem",
        "activity",
        "example",
        "intext",
        "in_text",
        "in-text",
        "mcq",
        "worksheet",
        "ncert",
        "homework",
        "practice",
        "solved",
        "unsolved",
    )
    private val RESOURCE_TYPE_MARKERS = listOf(
        "figure",
        "image",
        "diagram",
        "table",
        "photo",
        "illustration",
        "resource",
        "map",
        "chart",
        "graph",
        "picture",
    )

    fun overviewMetrics(overview: StudentOverview): List<OverviewMetric> = listOf(
        OverviewMetric(
            label = "Subject",
            value = overview.subject_count.toString(),
            emphasized = false,
            iconKind = OverviewIcon.Subject,
        ),
        OverviewMetric(
            label = "Stem Mastery",
            value = pct(overview.ai.stem_mastery_pct),
            emphasized = true,
            iconKind = OverviewIcon.StemMastery,
        ),
        OverviewMetric(
            label = "Reading Progress",
            value = pct(overview.ai.reading_progress_pct),
            emphasized = false,
            iconKind = OverviewIcon.ReadingProgress,
        ),
        OverviewMetric(
            label = "Quizzes Done",
            value = (overview.ai.quizzes_completed ?: 0).toString(),
            emphasized = false,
            iconKind = OverviewIcon.AverageProgress,
        ),
    )

    fun sessionSummary(overview: StudentOverview): SessionSummary {
        val overall = overview.overall
        val first = overview.by_subject.firstOrNull()
        return SessionSummary(
            presentPercent = overall.present_pct_all.roundToInt(),
            present = overall.present,
            absent = overall.absent,
            leave = overall.leave,
            total = overall.sessions,
            subjectName = first?.subject_name ?: overview.class_name.orEmpty().ifBlank { "Subjects" },
            subjectSessionsLabel = first?.let {
                "${it.totals.sessions} sessions · ${it.totals.present_pct_all.roundToInt()}% present"
            } ?: "${overall.sessions} sessions · ${overall.present_pct_all.roundToInt()}% present",
            subjectPresentPercent = (first?.totals?.present_pct_all ?: overall.present_pct_all).roundToInt(),
        )
    }

    fun attendancePreview(overview: StudentOverview): List<AttendanceRecord> =
        overview.recent_attendance.take(2).map { row ->
            AttendanceRecord(
                date = formatDayLabel(row.day),
                status = row.status.toUi(),
                className = row.class_name,
                subject = row.subject_name,
                time = "",
            )
        }

    fun calendarEvents(events: List<CalendarEventOut>): List<CalendarEvent> =
        events.mapNotNull { event ->
            val start = parseDate(event.start_date) ?: return@mapNotNull null
            CalendarEvent(
                title = event.title,
                dateLabel = start.format(monthDayFmt),
                timeLabel = if (event.start_date == event.end_date) "All day" else "${event.start_date} – ${event.end_date}",
                type = event.event_type.toUi(),
                dayOfMonth = start.dayOfMonth,
                yearMonth = YearMonth.from(start).toString(),
            )
        }

    fun dayMarksFromEvents(events: List<CalendarEvent>): Map<Int, AcademicEventType> {
        val priority = listOf(AcademicEventType.Holiday, AcademicEventType.Exam, AcademicEventType.Event)
        return events.groupBy { it.dayOfMonth }.mapValues { (_, dayEvents) ->
            priority.first { type -> dayEvents.any { it.type == type } }
        }
    }

    fun attendanceDashboard(
        summary: StudentAttendanceSummary,
        calendar: AttendanceCalendar,
        timetable: WeekView? = null,
    ): AttendanceDashboard {
        val o = summary.overall
        val classByUnit = summary.by_subject.associate { it.teaching_unit_id to it.class_name }
        val sessions = calendar.days.mapNotNull { day ->
            val date = parseDate(day.day) ?: return@mapNotNull null
            val slot = timetable?.let { week ->
                sessionSlot(week, day.teaching_unit_id, day.subject_name, date.dayOfWeek)
            }
            AttendanceSession(
                dayOfMonth = date.dayOfMonth,
                dateLabel = date.format(sessionDateFmt),
                className = formatClassLabel(
                    classByUnit[day.teaching_unit_id] ?: slot?.class_name,
                ),
                subject = day.subject_name,
                time = slot?.let { formatPeriodRange(it.start_time, it.end_time) }.orEmpty(),
                status = day.status.toUi(),
            )
        }.sortedWith(compareByDescending<AttendanceSession> { it.dayOfMonth }.thenBy { it.time })
        return AttendanceDashboard(
            primaryStats = listOf(
                AttendanceStat(o.present.toString(), "Present", true),
                AttendanceStat(o.absent.toString(), "Absent", false),
                AttendanceStat(o.leave.toString(), "Leave", false),
            ),
            secondaryStats = listOf(
                AttendanceStat(o.sessions.toString(), "Sessions", false),
                AttendanceStat(pct(o.present_pct_all), "Present % (All)", true),
                AttendanceStat(pct(o.present_pct_excl_leave), "Present % (excl. leave)", false),
            ),
            bySubject = summary.by_subject.map {
                SubjectAttendanceRow(
                    subject = it.subject_name,
                    present = it.totals.present,
                    absent = it.totals.absent,
                    leave = it.totals.leave,
                )
            },
            dayMarks = calendar.days.mapNotNull { day ->
                val date = parseDate(day.day) ?: return@mapNotNull null
                date.dayOfMonth to when {
                    day.is_extra_class -> AttendanceDayMark.ExtraClass
                    day.status == AttendanceStatus.PRESENT -> AttendanceDayMark.Present
                    day.status == AttendanceStatus.ABSENT -> AttendanceDayMark.Absent
                    else -> AttendanceDayMark.Leave
                }
            }.groupBy({ it.first }, { it.second }).mapValues { (_, marks) ->
                when {
                    AttendanceDayMark.ExtraClass in marks -> AttendanceDayMark.ExtraClass
                    AttendanceDayMark.Absent in marks -> AttendanceDayMark.Absent
                    AttendanceDayMark.Leave in marks -> AttendanceDayMark.Leave
                    else -> AttendanceDayMark.Present
                }
            },
            sessions = sessions,
        )
    }

    fun aiHubStats(dashboard: ProgressDashboardResponse): List<AiHubStat> = listOf(
        AiHubStat(pct(dashboard.overall_mastery_pct), "Mastery"),
        AiHubStat(pct(dashboard.overall_progress_pct), "Reading"),
        AiHubStat(dashboard.quizzes_completed.toString(), "Quiz"),
    )

    fun emptyAiHubStats(): List<AiHubStat> = listOf(
        AiHubStat("0%", "Mastery"),
        AiHubStat("0%", "Reading"),
        AiHubStat("0", "Quiz"),
    )

    fun aiSubjects(subjects: List<AiSubjectOut>): List<AiSubjectItem> =
        subjects.map { subject ->
            AiSubjectItem(
                id = subject.subject_id,
                name = subject.name,
                abbreviation = subject.code?.takeIf { it.isNotBlank() }
                    ?: subject.name.take(4).uppercase(Locale.ENGLISH),
                iconRes = subjectIcon(subject.name, subject.code),
                className = formatClassLabel(subject.class_name),
            )
        }

    fun teachingUnitSubjects(units: List<TeachingUnitOut>): List<AiSubjectItem> =
        units
            .filter { it.status == TeachingUnitStatus.ACTIVE }
            .distinctBy { it.class_id to it.subject_id }
            .map { unit ->
                AiSubjectItem(
                    id = unit.subject_id,
                    name = unit.subject_name,
                    abbreviation = unit.class_name,
                    iconRes = subjectIcon(unit.subject_name, null),
                    className = formatClassLabel(unit.class_name),
                )
            }

    fun notifications(items: List<NotificationOut>): List<AppNotification> {
        val today = LocalDate.now()
        return items.map { n ->
            val published = parseDateTime(n.published_at)?.toLocalDate()
            AppNotification(
                id = n.id,
                title = n.title,
                body = n.body,
                timestampLabel = formatDateTimeLabel(n.published_at),
                unread = !n.is_read,
                section = if (published == today) NotificationSection.Today else NotificationSection.Earlier,
                authorName = n.author_name,
                teachingUnitLabel = n.teaching_unit_label,
            )
        }
    }

    fun weeklyTimetable(week: WeekView): WeeklyTimetable {
        val periods = week.periods.filter { it.is_active }.sortedBy { it.sort_order }
        val dayOrder = listOf(
            DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED, DayOfWeek.THU, DayOfWeek.FRI,
        )
        // Match design/students/TIme table(student).png — full weekday names as rows.
        val dayLabels = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        val allSlots = week.days.values.flatten()
        val subjects = allSlots.map { it.subject_name }.distinct().sorted()
        val cells = subjects.associateWith { subject ->
            dayOrder.map { day ->
                val slots = slotsForDay(week, day)
                periods.map { period ->
                    val hit = slots.any {
                        it.period_id == period.id &&
                            it.subject_name == subject &&
                            it.day_of_week == day
                    }
                    if (hit) subject else "Off"
                }
            }
        }
        val emptyRow = periods.map { "Off" }.ifEmpty { listOf("Off") }
        return WeeklyTimetable(
            subjects = subjects.ifEmpty { listOf("—") },
            timeSlots = periods.map { formatPeriodRange(it.start_time, it.end_time) }
                .ifEmpty { listOf("—") },
            days = dayLabels,
            cellsBySubject = cells.ifEmpty {
                mapOf("—" to dayLabels.map { emptyRow })
            },
        )
    }

    private fun slotsForDay(week: WeekView, day: DayOfWeek): List<WeekSlot> {
        val keys = listOf(day.name, day.name.lowercase(Locale.ENGLISH), day.name.take(3))
        return keys.flatMap { week.days[it].orEmpty() }.distinctBy { it.slot_id }
            .ifEmpty {
                week.days.values.flatten().filter { it.day_of_week == day }
            }
    }

    private fun sessionSlot(
        week: WeekView,
        teachingUnitId: String,
        subjectName: String,
        javaDow: java.time.DayOfWeek,
    ): WeekSlot? {
        val apiDow = javaDow.toApi()
        val slots = slotsForDay(week, apiDow)
        return slots.firstOrNull { it.teaching_unit_id == teachingUnitId }
            ?: slots.firstOrNull { it.subject_name.equals(subjectName, ignoreCase = true) }
    }

    private fun java.time.DayOfWeek.toApi(): DayOfWeek = when (this) {
        java.time.DayOfWeek.MONDAY -> DayOfWeek.MON
        java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUE
        java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WED
        java.time.DayOfWeek.THURSDAY -> DayOfWeek.THU
        java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRI
        java.time.DayOfWeek.SATURDAY -> DayOfWeek.SAT
        java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUN
    }

    private fun formatClassLabel(className: String?): String {
        val trimmed = className?.trim().orEmpty()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.startsWith("Class", ignoreCase = true)) trimmed else "Class $trimmed"
    }

    /** Formats API times like "09:30:00" / "09:30" → "9:30 AM - 10:30 AM". */
    private fun formatPeriodRange(start: String, end: String): String {
        val startLabel = formatClock(start)
        val endLabel = formatClock(end)
        return if (startLabel != null && endLabel != null) {
            "$startLabel - $endLabel"
        } else {
            "$start - $end"
        }
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

    fun chapterStats(dashboard: ProgressDashboardResponse): SubjectChapterStats =
        SubjectChapterStats(
            mastery = pct(dashboard.overall_mastery_pct),
            reading = pct(dashboard.overall_progress_pct),
            quizCount = dashboard.quizzes_completed.toString(),
            quickCheckCount = dashboard.quick_check_attempts.toString(),
        )

    fun chapters(items: List<ChapterListItem>): List<ChapterItem> =
        items.filter { it.is_active }.sortedBy { it.chapter_number }.map {
            ChapterItem(
                id = it.id,
                title = "CHAPTER: ${it.chapter_number} ${it.title}",
                description = it.description?.takeIf { text -> text.isNotBlank() }
                    ?: "Tap to start this chapter.",
                progressPercent = 0,
            )
        }

    fun quizQuestions(questions: List<QuizQuestionOut>): List<QuizQuestion> =
        questions.map {
            QuizQuestion(
                id = it.id,
                prompt = it.question_text,
                difficulty = it.difficulty.replaceFirstChar { c -> c.titlecase(Locale.ENGLISH) },
                options = parseOptions(it.options),
            )
        }

    fun devices(items: List<DeviceOut>): List<RegisteredDevice> =
        items.map {
            RegisteredDevice(
                platform = when (it.platform) {
                    DevicePlatform.IOS -> "iOS"
                    DevicePlatform.ANDROID -> "Android"
                    DevicePlatform.WEB -> "Web"
                },
                lastActive = it.last_active_at?.let(::formatDateTimeLabel) ?: "—",
                sessions = if (it.is_current) "Yes" else "--",
            )
        }

    fun chatMessages(messages: List<ChatMessage>): List<AiChatMessage> =
        messages.map {
            AiChatMessage(
                id = it.id ?: it.timestamp.orEmpty().ifBlank { it.message.hashCode().toString() },
                text = it.message,
                fromUser = it.role.equals("user", true) || it.role.equals("student", true),
            )
        }

    fun chatResponseMessage(response: ChatResponse): AiChatMessage =
        AiChatMessage(
            id = response.message_id ?: response.message.hashCode().toString(),
            text = response.message,
            fromUser = false,
        )

    fun quickCheck(check: ConceptCheck?): AiQuickCheck? =
        check?.let {
            AiQuickCheck(
                question = it.question,
                options = it.options,
                correctIndex = it.correct,
                explanation = it.explanation,
            )
        }

    fun syllabus(sections: List<SectionOut>): List<AiSyllabusItem> {
        val out = mutableListOf<AiSyllabusItem>()
        fun walk(list: List<SectionOut>) {
            list.sortedBy { it.sort_order }.forEach { section ->
                out += AiSyllabusItem(
                    id = section.id,
                    title = listOfNotNull(section.section_number, section.title).joinToString(" "),
                    progressLabel = null,
                    indented = section.depth > 0 || section.parent_section_id != null,
                    highlighted = false,
                )
                walk(section.subsections)
            }
        }
        walk(sections)
        return out
    }

    fun flattenSections(sections: List<SectionOut>): List<SectionOut> {
        val out = mutableListOf<SectionOut>()
        fun walk(list: List<SectionOut>) {
            list.sortedBy { it.sort_order }.forEach { section ->
                out += section
                walk(section.subsections)
            }
        }
        walk(sections)
        return out
    }

    fun textbookQuestions(
        sections: List<SectionOut>,
        fallbackTitle: String = "Question",
    ): List<AiMenuContentItem> =
        contentItems(sections, fallbackTitle) { block -> isQuestionBlock(block) && !isResourceBlock(block) }

    fun resources(
        sections: List<SectionOut>,
        fallbackTitle: String = "Figure",
    ): List<AiMenuContentItem> =
        contentItems(sections, fallbackTitle, ::isResourceBlock)

    fun quizHistory(attempts: List<QuizAttemptSummary>): List<AiQuizHistoryItem> =
        attempts.sortedByDescending { it.completed_at ?: it.started_at }.map { attempt ->
            val mode = humanizeQuizMode(attempt.quiz_mode)
            val score = when {
                attempt.completed_at == null -> "In progress"
                attempt.score != null && attempt.max_score != null ->
                    "${attempt.score}/${attempt.max_score}"
                else -> "${attempt.total_questions} questions"
            }
            val whenLabel = formatAttemptWhen(attempt.completed_at ?: attempt.started_at)
            AiQuizHistoryItem(
                id = attempt.id,
                title = "$mode · Attempt ${attempt.attempt_number}",
                subtitle = listOfNotNull(score, whenLabel).joinToString("  ·  "),
            )
        }

    private fun contentItems(
        sections: List<SectionOut>,
        fallbackTitle: String,
        predicate: (ContentBlockOut) -> Boolean,
    ): List<AiMenuContentItem> {
        val out = mutableListOf<AiMenuContentItem>()
        flattenSections(sections).forEach { section ->
            val sectionLabel = listOfNotNull(section.section_number, section.title)
                .joinToString(" ")
                .ifBlank { null }
            section.content_blocks.sortedBy { it.sort_order }.filter(predicate).forEach { block ->
                out += AiMenuContentItem(
                    id = block.id,
                    sectionId = block.section_id.ifBlank { section.id },
                    title = blockTitle(block, fallbackTitle),
                    subtitle = sectionLabel,
                    imageUrl = block.ai_image_url?.takeIf { it.isNotBlank() }
                        ?: block.original_image_url?.takeIf { it.isNotBlank() },
                )
            }
        }
        return out
    }

    private fun isResourceBlock(block: ContentBlockOut): Boolean {
        val type = block.block_type.lowercase()
        if (RESOURCE_TYPE_MARKERS.any { it in type }) return true
        if (!block.figure_ref.isNullOrBlank()) return true
        if (!block.ai_image_url.isNullOrBlank() || !block.original_image_url.isNullOrBlank()) return true
        return false
    }

    private fun isQuestionBlock(block: ContentBlockOut): Boolean {
        val type = block.block_type.lowercase()
        if (QUESTION_TYPE_MARKERS.any { it in type }) return true
        val text = listOfNotNull(block.title, block.content_text).joinToString(" ")
        return text.contains('?')
    }

    private fun blockTitle(block: ContentBlockOut, fallback: String): String {
        block.title?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        block.content_text?.lineSequence()?.firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.let { line -> if (line.length > 90) line.take(87).trimEnd() + "…" else line }
            ?.let { return it }
        block.figure_ref?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        return fallback
    }

    private fun humanizeQuizMode(mode: String): String =
        mode.replace('_', ' ').lowercase(Locale.ENGLISH).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString()
        }

    private fun formatAttemptWhen(value: String): String? = runCatching {
        OffsetDateTime.parse(value).format(attemptDateFmt)
    }.getOrNull() ?: value.take(10).takeIf { it.length >= 10 }

    fun apiLanguage(uiLanguage: String): String =
        if (uiLanguage.equals("Mizo", ignoreCase = true)) "mizo" else "en"

    private fun AttendanceStatus.toUi(): UiAttendanceStatus = when (this) {
        AttendanceStatus.PRESENT -> UiAttendanceStatus.Present
        AttendanceStatus.ABSENT -> UiAttendanceStatus.Absent
        AttendanceStatus.LEAVE -> UiAttendanceStatus.Leave
    }

    private fun CalendarEventType.toUi(): AcademicEventType = when (this) {
        CalendarEventType.HOLIDAY -> AcademicEventType.Holiday
        CalendarEventType.EXAM -> AcademicEventType.Exam
        CalendarEventType.EVENT -> AcademicEventType.Event
    }

    private fun pct(value: Double?): String =
        "${(value ?: 0.0).roundToInt()}%"

    private fun parseDate(value: String): LocalDate? = runCatching {
        when {
            value.length >= 10 -> LocalDate.parse(value.take(10))
            else -> LocalDate.parse(value)
        }
    }.getOrNull()

    private fun parseDateTime(value: String): OffsetDateTime? = runCatching {
        OffsetDateTime.parse(value)
    }.getOrNull() ?: parseDate(value)?.atStartOfDay()?.atOffset(java.time.ZoneOffset.UTC)

    private fun formatDayLabel(value: String): String =
        parseDate(value)?.format(monthDayFmt) ?: value

    private fun formatDateTimeLabel(value: String): String {
        val odt = parseDateTime(value) ?: return value
        return odt.format(deviceFmt)
    }

    private fun subjectIcon(name: String, code: String?): Int {
        val key = "${name.lowercase(Locale.ENGLISH)} ${code.orEmpty().lowercase(Locale.ENGLISH)}"
        return when {
            "chem" in key -> R.drawable.ic_subject_chemistry
            "math" in key -> R.drawable.ic_subject_mathematics
            "phys" in key -> R.drawable.ic_subject_physics
            else -> R.drawable.ic_subject_chemistry
        }
    }

    private fun parseOptions(options: JsonElement?): List<String> {
        if (options == null) return emptyList()
        return when (options) {
            is JsonArray -> options.mapNotNull(::optionText)
            is JsonPrimitive -> listOfNotNull(options.contentOrNull)
            is JsonObject -> listOfNotNull(optionText(options))
            else -> emptyList()
        }
    }

    private fun optionText(element: JsonElement): String? = when (element) {
        is JsonPrimitive -> element.contentOrNull
        is JsonObject -> {
            val keys = listOf("text", "label", "option", "value", "content", "html", "markdown")
            keys.firstNotNullOfOrNull { key ->
                (element[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            } ?: element.values.firstNotNullOfOrNull { value ->
                (value as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            }
        }
        else -> null
    }
}
