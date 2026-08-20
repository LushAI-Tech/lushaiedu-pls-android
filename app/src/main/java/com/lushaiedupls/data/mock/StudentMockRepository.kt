package com.lushaiedupls.data.mock

import com.lushaiedupls.R

data class StudentProfile(
    val displayName: String,
    val notificationCount: Int,
)

data class OverviewMetric(
    val label: String,
    val value: String,
    val emphasized: Boolean,
    val iconKind: OverviewIcon,
)

enum class OverviewIcon {
    Subject,
    StemMastery,
    ReadingProgress,
    AverageProgress,
}

data class SessionSummary(
    val presentPercent: Int,
    val present: Int,
    val absent: Int,
    val leave: Int,
    val total: Int,
    val subjectName: String,
    val subjectSessionsLabel: String,
    val subjectPresentPercent: Int,
)

data class AttendanceRecord(
    val date: String,
    val status: AttendanceStatus,
    val className: String,
    val subject: String,
    val time: String,
)

enum class AttendanceStatus {
    Present,
    Absent,
    Leave,
}

enum class AttendanceDayMark {
    Present,
    Absent,
    Leave,
    ExtraClass,
}

data class AttendanceStat(
    val value: String,
    val label: String,
    val emphasized: Boolean,
)

data class SubjectAttendanceRow(
    val subject: String,
    val present: Int,
    val absent: Int,
    val leave: Int,
)

data class AttendanceDayStatus(
    val dayOfMonth: Int,
    val mark: AttendanceDayMark,
)

data class AttendanceDashboard(
    val primaryStats: List<AttendanceStat>,
    val secondaryStats: List<AttendanceStat>,
    val bySubject: List<SubjectAttendanceRow>,
    /** day-of-month → mark for the currently shown month */
    val dayMarks: Map<Int, AttendanceDayMark>,
    val sessions: List<AttendanceSession> = emptyList(),
)

data class AttendanceSession(
    val dayOfMonth: Int,
    val dateLabel: String,
    val className: String,
    val subject: String,
    val time: String,
    val status: AttendanceStatus,
)

data class CalendarEvent(
    val title: String,
    val dateLabel: String,
    val timeLabel: String,
    val type: AcademicEventType = AcademicEventType.Event,
    val dayOfMonth: Int = 1,
    val yearMonth: String = "2026-07",
)

enum class AcademicEventType {
    Holiday,
    Exam,
    Event,
}

data class ChatThread(
    val id: String,
    val title: String,
    val preview: String,
    val updatedAt: String,
)

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val timestampLabel: String,
    val unread: Boolean,
    val section: NotificationSection,
    val authorName: String? = null,
    val teachingUnitLabel: String? = null,
)

enum class NotificationSection {
    Today,
    Earlier,
}

data class ChapterItem(
    val id: String = "",
    val title: String,
    val description: String,
    val progressPercent: Int = 0,
)

data class SubjectChapterStats(
    val mastery: String,
    val reading: String,
    val quizCount: String,
    val quickCheckCount: String,
)

data class QuizItem(
    val title: String,
    val questionCount: Int,
    val completed: Boolean,
)

data class QuizQuestion(
    val id: String = "",
    val prompt: String,
    val difficulty: String,
    val options: List<String>,
)

data class TimetableSlot(
    val day: String,
    val time: String,
    val subject: String,
    val room: String,
)

data class WeeklyTimetable(
    val subjects: List<String>,
    val timeSlots: List<String>,
    val days: List<String>,
    val cellsBySubject: Map<String, List<List<String>>>,
)

data class AiHubStat(
    val value: String,
    val label: String,
)

data class AiSubjectItem(
    val id: String,
    val name: String,
    val abbreviation: String,
    val iconRes: Int,
    val className: String = "",
)

data class RegisteredDevice(
    val platform: String,
    val lastActive: String,
    val sessions: String,
)

data class AiChatMessage(
    val id: String,
    val text: String,
    val fromUser: Boolean,
)

data class AiQuickCheck(
    val question: String,
    val options: List<String>,
    val correctIndex: Int? = null,
    val explanation: String? = null,
)

data class AiSyllabusItem(
    val id: String,
    val title: String,
    val progressLabel: String?,
    val indented: Boolean = false,
    val highlighted: Boolean = false,
)

data class AiMenuContentItem(
    val id: String,
    val sectionId: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
)

data class AiQuizHistoryItem(
    val id: String,
    val title: String,
    val subtitle: String,
)

data class AiChatLanguagePack(
    val messages: List<AiChatMessage>,
    val suggestions: List<String>,
    val quickCheck: AiQuickCheck,
)

data class AiChatSession(
    val chapterTitle: String,
    val english: AiChatLanguagePack,
    val mizo: AiChatLanguagePack,
    val syllabus: List<AiSyllabusItem>,
    val textbookQuestions: List<AiMenuContentItem> = emptyList(),
    val resources: List<AiMenuContentItem> = emptyList(),
    val quizHistory: List<AiQuizHistoryItem> = emptyList(),
) {
    fun packFor(language: String): AiChatLanguagePack =
        if (language.equals("Mizo", ignoreCase = true)) mizo else english
}

enum class AiMenuTab {
    Chats,
    TextbookQuestions,
    ExamPreparation,
    Resources,
}

/** Shared bilingual AI chat mock used by Student and Teacher orgs. */
fun aiChatSessionFor(subjectId: String): AiChatSession {
    val chapter = when (subjectId) {
        "mathematics" -> "Trigonometry"
        "physics" -> "Motion in a Straight Line"
        else -> "Chemical Kinetics"
    }
    val english = AiChatLanguagePack(
        messages = listOf(
            AiChatMessage("u1", "Hi", fromUser = true),
            AiChatMessage(
                id = "a1",
                fromUser = false,
                text = """Hello! Today's chapter is **Solutions**. In this chapter we will learn:

- Different types of solutions
- Ways of expressing concentration
- Henry's law: \(p = k_H \times c\)
- Raoult's law and colligative properties

The mole fraction of a solute is:

\[x_{solute} = \frac{n_{solute}}{n_{solute} + n_{solvent}}\]

Where would you like to start?""".trimIndent(),
            ),
        ),
        suggestions = listOf("What is Henry's law?", "Soda Freeze?"),
        quickCheck = AiQuickCheck(
            question = "To make a solution we use 10 g sugar and 100 ml water. What is the solvent?",
            options = listOf("Water", "Sugar", "Water and Sugar", "None of these"),
        ),
    )
    val mizo = AiChatLanguagePack(
        messages = listOf(
            AiChatMessage("u1", "Hi", fromUser = true),
            AiChatMessage(
                id = "a1",
                fromUser = false,
                text = """Chibai! Tunah kan chapter hi "Solutions" a ni a. He chapter ah hian i zir dawn te chu:

• Solutions chi hrang hrangte.
• Concentration sawifiah dan.
• Henry's law leh Raoult's law.
• Vapour pressure leh colligative properties lamte kan zir dawn a ni.

Eng atangin nge i tan duh?""".trimIndent(),
            ),
        ),
        suggestions = listOf("Eng nge Henry's law?", "Soda Freez?"),
        quickCheck = AiQuickCheck(
            question = "Solution pakhat siam nan chini gram 10 leh tui ml 100 kan hman a, eng nge solvent?",
            options = listOf("Tui", "Chini", "Tui leh Chini", "A ni lo ve ve."),
        ),
    )
    return AiChatSession(
        chapterTitle = chapter,
        english = english,
        mizo = mizo,
        syllabus = listOf(
            AiSyllabusItem("1.0", "1.0 Objectives", "0/1", highlighted = true),
            AiSyllabusItem("1.1", "1.1 Types of Solutions", "0/1"),
            AiSyllabusItem("1.2", "1.2 Expressing Concentration of Solutions", "0/1"),
            AiSyllabusItem("1.3", "1.3 Solubility", null),
            AiSyllabusItem("1.3.1", "1.3.1 Solubility of a Solid in a Liquid", null, indented = true),
            AiSyllabusItem("1.3.2", "1.3.2 Solubility of a Gas in a Liquid", "0/4", indented = true),
        ),
        textbookQuestions = listOf(
            AiMenuContentItem(
                id = "q1",
                sectionId = "1.1",
                title = "What is a solution? Give two examples from daily life.",
                subtitle = "1.1 Types of Solutions",
            ),
            AiMenuContentItem(
                id = "q2",
                sectionId = "1.2",
                title = "Calculate the mole fraction of a solute in a binary solution.",
                subtitle = "1.2 Expressing Concentration of Solutions",
            ),
        ),
        resources = listOf(
            AiMenuContentItem(
                id = "r1",
                sectionId = "1.2",
                title = "Fig. 2.1 Concentration units",
                subtitle = "1.2 Expressing Concentration of Solutions",
            ),
            AiMenuContentItem(
                id = "r2",
                sectionId = "1.3",
                title = "Fig. 2.3 Effect of pressure on solubility",
                subtitle = "1.3 Solubility",
            ),
        ),
        quizHistory = listOf(
            AiQuizHistoryItem("h1", "Chapter practice · Attempt 1", "8/10  ·  12 Aug 2026"),
            AiQuizHistoryItem("h2", "Section · Attempt 1", "6/10  ·  10 Aug 2026"),
        ),
    )
}

class StudentMockRepository {
    fun profile(displayName: String = "V Lalfakea"): StudentProfile = StudentProfile(
        displayName = displayName,
        notificationCount = 3,
    )

    fun overviewMetrics(): List<OverviewMetric> = listOf(
        OverviewMetric("Subject", "4", emphasized = true, OverviewIcon.Subject),
        OverviewMetric("Stem Mastery", "100%", emphasized = true, OverviewIcon.StemMastery),
        OverviewMetric("Reading Progress", "100%", emphasized = false, OverviewIcon.ReadingProgress),
        OverviewMetric("Average Progress", "4", emphasized = false, OverviewIcon.AverageProgress),
    )

    fun sessionSummary(): SessionSummary = SessionSummary(
        presentPercent = 50,
        present = 1,
        absent = 1,
        leave = 0,
        total = 2,
        subjectName = "english",
        subjectSessionsLabel = "2 sessions · 50% present",
        subjectPresentPercent = 50,
    )

    fun attendanceRecords(): List<AttendanceRecord> = listOf(
        AttendanceRecord("2026-08-05", AttendanceStatus.Present, "Class XII", "English", "2:10 PM–2:50 PM"),
        AttendanceRecord("2026-08-04", AttendanceStatus.Absent, "Class XII", "English", "2:10 PM–2:50 PM"),
        AttendanceRecord("2026-08-03", AttendanceStatus.Leave, "Class XII", "Physics", "10:00 AM–10:40 AM"),
        AttendanceRecord("2026-08-02", AttendanceStatus.Present, "Class XII", "Mathematics", "11:00 AM–11:40 AM"),
    )

    fun attendanceDashboard(): AttendanceDashboard = AttendanceDashboard(
        primaryStats = listOf(
            AttendanceStat("4", "Present", emphasized = true),
            AttendanceStat("3", "Absent", emphasized = true),
            AttendanceStat("0", "Leave", emphasized = true),
        ),
        secondaryStats = listOf(
            AttendanceStat("7", "Sessions", emphasized = false),
            AttendanceStat("57.1%", "Present % (All)", emphasized = false),
            AttendanceStat("57.1%", "Present % (excl. leave)", emphasized = false),
        ),
        bySubject = listOf(
            SubjectAttendanceRow("english", present = 3, absent = 3, leave = 0),
            SubjectAttendanceRow("mathematics", present = 1, absent = 0, leave = 0),
        ),
        dayMarks = mapOf(
            3 to AttendanceDayMark.Leave,
            4 to AttendanceDayMark.Absent,
            5 to AttendanceDayMark.Present,
        ),
        sessions = listOf(
            AttendanceSession(5, "Wed, 5 Aug 2026", "Class XII", "English", "9:50 AM - 10:30 AM", AttendanceStatus.Present),
            AttendanceSession(4, "Tue, 4 Aug 2026", "Class XII", "English", "9:50 AM - 10:30 AM", AttendanceStatus.Absent),
            AttendanceSession(3, "Mon, 3 Aug 2026", "Class XII", "Physics", "10:00 AM - 10:40 AM", AttendanceStatus.Leave),
        ),
    )

    fun calendarEvents(): List<CalendarEvent> = listOf(
        CalendarEvent(
            title = "Independence Day Holiday",
            dateLabel = "Wed, 22 Jul",
            timeLabel = "All day",
            type = AcademicEventType.Holiday,
            dayOfMonth = 22,
            yearMonth = "2026-07",
        ),
        CalendarEvent(
            title = "Physics Mid-term Exam",
            dateLabel = "Fri, 10 Jul",
            timeLabel = "09:00 – 11:00",
            type = AcademicEventType.Exam,
            dayOfMonth = 10,
            yearMonth = "2026-07",
        ),
        CalendarEvent(
            title = "Science Fair",
            dateLabel = "Tue, 14 Jul",
            timeLabel = "10:00 – 15:00",
            type = AcademicEventType.Event,
            dayOfMonth = 14,
            yearMonth = "2026-07",
        ),
        CalendarEvent(
            title = "Math Quiz",
            dateLabel = "Wed, 22 Jul",
            timeLabel = "11:00 – 12:00",
            type = AcademicEventType.Exam,
            dayOfMonth = 22,
            yearMonth = "2026-07",
        ),
        CalendarEvent(
            title = "Parent-Teacher Meeting",
            dateLabel = "Sat, 25 Jul",
            timeLabel = "14:00 – 16:00",
            type = AcademicEventType.Event,
            dayOfMonth = 25,
            yearMonth = "2026-07",
        ),
        CalendarEvent(
            title = "Physics Lab",
            dateLabel = "Mon, 10 Aug",
            timeLabel = "09:00 – 10:00",
            type = AcademicEventType.Event,
            dayOfMonth = 10,
            yearMonth = "2026-08",
        ),
    )

    fun academicDayMarks(yearMonth: String): Map<Int, AcademicEventType> =
        calendarEvents()
            .filter { it.yearMonth == yearMonth }
            .groupBy { it.dayOfMonth }
            .mapValues { (_, events) ->
                when {
                    events.any { it.type == AcademicEventType.Holiday } -> AcademicEventType.Holiday
                    events.any { it.type == AcademicEventType.Exam } -> AcademicEventType.Exam
                    else -> AcademicEventType.Event
                }
            }

    fun chatThreads(): List<ChatThread> = listOf(
        ChatThread("1", "Explain Newton’s laws", "Sure — let’s start with inertia…", "Today"),
        ChatThread("2", "Organic chemistry tips", "Focus on functional groups first.", "Yesterday"),
        ChatThread("3", "Study plan for exams", "Here’s a 7-day schedule…", "Mon"),
    )

    fun notifications(): List<AppNotification> = listOf(
        AppNotification(
            id = "n1",
            title = "Science Fair Registration Open",
            body = "Register by Friday for the annual science fair. Projects can be individual or team-based. Submit your proposal through the school portal or ask your class teacher for the paper form.",
            timestampLabel = "Today, 9:15 AM",
            unread = true,
            section = NotificationSection.Today,
            authorName = "Admin Office",
            teachingUnitLabel = "Class 12 · Chemistry",
        ),
        AppNotification(
            id = "n2",
            title = "Class starting soon",
            body = "Class 12 (Chemistry): starts at 1:00 pm (in 30 min). Please bring your lab notebook and completed worksheet from last session.",
            timestampLabel = "Today, 12:30 PM",
            unread = true,
            section = NotificationSection.Today,
            authorName = "Mrs. Zuali",
            teachingUnitLabel = "Class 12 · Chemistry",
        ),
        AppNotification(
            id = "n3",
            title = "Holiday notice",
            body = "School will remain closed next Monday for the state holiday. Regular classes resume on Tuesday.",
            timestampLabel = "25 Jul 2026, 06:55",
            unread = false,
            section = NotificationSection.Earlier,
            authorName = "Principal",
            teachingUnitLabel = null,
        ),
    )

    fun chapterStats(): SubjectChapterStats = SubjectChapterStats(
        mastery = "100%",
        reading = "100%",
        quizCount = "50",
        quickCheckCount = "4",
    )

    fun chapters(): List<ChapterItem> = listOf(
        ChapterItem(
            id = "chem-1",
            title = "CHAPTER: 1 Solutions",
            description = "This unit covers the formation of different types of soluti....",
        ),
        ChapterItem(
            title = "CHAPTER: 2 Electrochemistry",
            description = "Study of production of electricity from chemical reactio....",
        ),
        ChapterItem(
            title = "CHAPTER: 3 Chemical Kinetics",
            description = "Rates of chemical reactions and factors affecting reaction speed.",
        ),
    )

    fun quizzes(): List<QuizItem> = listOf(
        QuizItem("Physics — Kinematics", 10, completed = true),
        QuizItem("Chemistry — Mole Concept", 12, completed = false),
        QuizItem("Math — Sequences", 8, completed = false),
    )

    fun quizQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            prompt = "Restaurant tehkhin-na hman a nih khan, 'building' kha eng nen nge an thuhmun?",
            difficulty = "MEDIUM",
            options = listOf("Hosting", "Domain", "Front-End", "Back-End"),
        ),
        QuizQuestion(
            prompt = "Eng nge Henry's law a sawi?",
            difficulty = "EASY",
            options = listOf("Gas solubility", "Boiling point", "Osmosis", "pH scale"),
        ),
        QuizQuestion(
            prompt = "Solution pakhat siam nan solvent eng nge?",
            difficulty = "MEDIUM",
            options = listOf("Tui", "Chini", "Salt", "Oil"),
        ),
        QuizQuestion(
            prompt = "Raoult's law eng nge a cover?",
            difficulty = "HARD",
            options = listOf("Vapour pressure", "Density", "Colour", "Mass"),
        ),
        QuizQuestion(
            prompt = "Colligative properties depend on eng nge?",
            difficulty = "MEDIUM",
            options = listOf("Particle number", "Colour", "Smell", "Shape"),
        ),
    )

    fun timetable(): List<TimetableSlot> = listOf(
        TimetableSlot("Monday", "09:00", "Physics", "Lab 1"),
        TimetableSlot("Monday", "11:00", "English", "Room 204"),
        TimetableSlot("Tuesday", "10:00", "Chemistry", "Lab 2"),
        TimetableSlot("Wednesday", "09:00", "Mathematics", "Room 101"),
    )

    fun weeklyTimetable(): WeeklyTimetable {
        val slots = listOf("9:30 AM - 10:30 AM", "10:30 AM - 11:30 AM")
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        return WeeklyTimetable(
            subjects = listOf("Chemistry", "Physics", "Maths", "Bio"),
            timeSlots = slots,
            days = days,
            cellsBySubject = mapOf(
                "Chemistry" to listOf(
                    listOf("Off", "Chemistry"),
                    listOf("Chemistry", "Off"),
                    listOf("Chemistry", "Off"),
                    listOf("Off", "Chemistry"),
                    listOf("Chemistry", "Off"),
                ),
                "Physics" to listOf(
                    listOf("Physics", "Off"),
                    listOf("Off", "Physics"),
                    listOf("Physics", "Off"),
                    listOf("Off", "Physics"),
                    listOf("Physics", "Off"),
                ),
                "Maths" to listOf(
                    listOf("Off", "Maths"),
                    listOf("Maths", "Off"),
                    listOf("Off", "Maths"),
                    listOf("Maths", "Off"),
                    listOf("Off", "Maths"),
                ),
                "Bio" to listOf(
                    listOf("Bio", "Off"),
                    listOf("Off", "Bio"),
                    listOf("Bio", "Off"),
                    listOf("Off", "Bio"),
                    listOf("Bio", "Off"),
                ),
            ),
        )
    }

    fun aiHubStats(): List<AiHubStat> = listOf(
        AiHubStat("40%", "Mastery"),
        AiHubStat("30%", "Reading"),
        AiHubStat("0", "Quiz"),
    )

    fun aiSubjects(): List<AiSubjectItem> = listOf(
        AiSubjectItem("chemistry", "Chemistry", "CHEM", R.drawable.ic_subject_chemistry),
        AiSubjectItem("mathematics", "Mathematics", "MATHS", R.drawable.ic_subject_mathematics),
        AiSubjectItem("physics", "Physics", "PHY", R.drawable.ic_subject_physics),
    )

    fun aiSubjectById(id: String): AiSubjectItem? = aiSubjects().find { it.id == id }

    fun accountEmail(): String = "Fakeavangchhia@gmail.com"

    fun registeredDevices(): List<RegisteredDevice> = listOf(
        RegisteredDevice("iOS", "7th Aug 2026, 12:06", "--"),
        RegisteredDevice("Web", "7th Aug 2026, 12:32", "--"),
        RegisteredDevice("Android", "10th Aug 2026, 11:06", "Yes"),
    )

    fun aiChatSession(subjectId: String): AiChatSession = aiChatSessionFor(subjectId)
}
