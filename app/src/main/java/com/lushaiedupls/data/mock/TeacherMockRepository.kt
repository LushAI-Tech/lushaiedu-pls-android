package com.lushaiedupls.data.mock

import com.lushaiedupls.R
import com.lushaiedupls.ui.auth.selectclass.SchoolClass
import com.lushaiedupls.ui.auth.selectsubject.SubjectOption

data class TeacherProfile(
    val displayName: String,
    val notificationCount: Int,
)

data class TeacherGroupOutcome(
    val studentsCount: Int,
    val attendancePercent: Int,
)

data class TeacherAttendanceBlock(
    val presentRate: String,
    val totalSessions: String,
    val presentAbsentLeave: String,
)

data class TeacherHomeDashboard(
    val selectedClass: String,
    val classes: List<String>,
    val groupOutcome: TeacherGroupOutcome,
    val regularAttendance: TeacherAttendanceBlock,
    val extraAttendance: TeacherAttendanceBlock,
    val topPerformances: List<TeacherPerformance>,
)

data class TeacherPerformance(
    val studentName: String,
    val percent: Int,
)

data class TeacherGroup(
    val id: String,
    val title: String,
    val code: String,
    val status: String = "Active",
    val subjectIconRes: Int = R.drawable.ic_metric_subject,
    val subjectName: String = "",
)

data class TeacherVolumeRow(
    val label: String,
    val detail: String,
    val presentPercent: Int,
)

data class TeacherOverviewDashboard(
    val monthLabel: String,
    val presentRate: String,
    val totalSessions: String,
    val presentAbsentLeave: String,
    val presentCount: Int,
    val absentCount: Int,
    val leaveCount: Int,
    val presentPercent: Int,
    val byClass: TeacherVolumeRow,
    val bySubject: TeacherVolumeRow,
)

data class TeacherClassStudentScore(
    val name: String,
    val percent: Int,
    val avatarUrl: String? = null,
    val detail: String = "",
)

data class TeacherStudent(
    val id: String,
    val name: String,
    val email: String,
    val rollNumber: Int,
    val hasParentsSelected: Boolean = false,
)

enum class TeacherAttendanceMark {
    None,
    Present,
    Absent,
}

data class TeacherAttendanceStudent(
    val student: TeacherStudent,
    val mark: TeacherAttendanceMark = TeacherAttendanceMark.None,
)

data class TeacherAttendanceSession(
    val classLabel: String,
    val dateLabel: String,
    val subjectLabel: String,
    val timeLabel: String,
    val students: List<TeacherAttendanceStudent>,
)

data class TeacherClassOverview(
    val groupId: String,
    val title: String,
    val orgLabel: String,
    val studentCount: Int,
    val averageAttendancePercent: Int,
    val periodLabel: String,
    val regularAttendance: TeacherAttendanceBlock,
    val extraAttendance: TeacherAttendanceBlock,
    val studentScores: List<TeacherClassStudentScore>,
)

class TeacherMockRepository {
    fun profile(displayName: String = "C Vanlalawmpuia"): TeacherProfile = TeacherProfile(
        displayName = displayName,
        notificationCount = 2,
    )

    fun homeDashboard(): TeacherHomeDashboard = TeacherHomeDashboard(
        selectedClass = "Class XI",
        classes = listOf("Class XI", "Class XII"),
        groupOutcome = TeacherGroupOutcome(studentsCount = 0, attendancePercent = 0),
        regularAttendance = TeacherAttendanceBlock(
            presentRate = "100%",
            totalSessions = "1",
            presentAbsentLeave = "1/0/0",
        ),
        extraAttendance = TeacherAttendanceBlock(
            presentRate = "100%",
            totalSessions = "1",
            presentAbsentLeave = "1/0/0",
        ),
        topPerformances = listOf(
            TeacherPerformance("Heidi laldinzuali tlau", 50),
            TeacherPerformance("Steven Lalawmpuia", 40),
            TeacherPerformance("Angela Lalramnghaki", 30),
        ),
    )

    fun groups(
        classSubjects: Map<SchoolClass, List<SubjectOption>> = emptyMap(),
    ): List<TeacherGroup> {
        if (classSubjects.isNotEmpty()) {
            return classSubjects.entries
                .sortedBy { it.key.ordinal }
                .flatMap { (schoolClass, subjects) ->
                    subjects.sortedBy { it.ordinal }.map { subject ->
                        TeacherGroup(
                            id = "${schoolClass.name}_${subject.name}",
                            title = "${schoolClass.displayLabel()} (${subject.name})",
                            code = mockGroupCode(schoolClass, subject),
                            status = "Active",
                            subjectIconRes = subjectIconFor(subject.name),
                            subjectName = subject.name,
                        )
                    }
                }
        }
        return listOf(
            TeacherGroup("g1", "Class XII (Chemistry)", "J3QUE3", "Active", R.drawable.ic_subject_chemistry, "Chemistry"),
            TeacherGroup("g2", "Class XI (Chemistry)", "BFTNHE", "Active", R.drawable.ic_subject_chemistry, "Chemistry"),
        )
    }

    fun overviewDashboard(): TeacherOverviewDashboard = TeacherOverviewDashboard(
        monthLabel = "July 2026",
        presentRate = "40%",
        totalSessions = "8",
        presentAbsentLeave = "7/1/0",
        presentCount = 1,
        absentCount = 1,
        leaveCount = 0,
        presentPercent = 50,
        byClass = TeacherVolumeRow("Class 12", "8 sessions · 50% present", 50),
        bySubject = TeacherVolumeRow("english", "8 sessions · 50% present", 50),
    )

    fun classOverview(groupId: String): TeacherClassOverview {
        val group = groups().find { it.id == groupId }
            ?: TeacherGroup(groupId, "Class XII (Chemistry)", "J3QUE3")
        return TeacherClassOverview(
            groupId = group.id,
            title = group.title,
            orgLabel = "LushaiEdu",
            studentCount = 9,
            averageAttendancePercent = 38,
            periodLabel = "All time",
            regularAttendance = TeacherAttendanceBlock("38%", "8", "7/1/0"),
            extraAttendance = TeacherAttendanceBlock("0%", "0", "0/0/0"),
            studentScores = listOf(
                TeacherClassStudentScore("Heidi laldinzuali tlau", 50, detail = "4/4/0 · 8 sessions"),
                TeacherClassStudentScore("Steven Lalawmpuia", 40, detail = "3/5/0 · 8 sessions"),
                TeacherClassStudentScore("Angela Lalramnghaki", 40, detail = "3/5/0 · 8 sessions"),
                TeacherClassStudentScore("Lalmuansangi", 20, detail = "2/6/0 · 8 sessions"),
                TeacherClassStudentScore("Ignatius Malsawmsanga", 0, detail = "0/8/0 · 8 sessions"),
            ),
        )
    }

    fun studentsInClass(groupId: String = "g1"): List<TeacherStudent> = listOf(
        TeacherStudent("s1", "Angela Lalramnghaki", "ann.xud809@gmail.com", 1, hasParentsSelected = true),
        TeacherStudent("s2", "F.Lalremsanga", "sangafanai2007@gmail.com", 2),
        TeacherStudent("s3", "Heidi laldinzuali tlau", "heiditlau1@gmail.com", 3),
        TeacherStudent("s4", "Ignatius Malsawmsanga", "ignatiusmalsawmsanga@gmail.com", 4),
        TeacherStudent("s5", "Lalmuansangi", "lalmuansangi803@gmail.com", 5),
        TeacherStudent("s6", "Robert Rodingliana", "robertchhakchhuak043@gmail.com", 6),
        TeacherStudent("s7", "Steven Lalawmpuia", "stevenlalawmpuia777@gmail.com", 7),
        TeacherStudent("s8", "K.lawmsangzuala", "gususerac13@gmail.com", 8),
        TeacherStudent("s9", "Lalthanzami", "lalthanzami@gmail.com", 9),
    )

    fun attendanceClasses(): List<String> =
        listOf("Class XII", "Class XI", "Class X", "Class IX")

    fun attendanceSession(
        classLabel: String = "Class XII",
        dateLabel: String = "2026-06-17",
        subjectLabel: String = "English",
        timeLabel: String = "9:50 AM - 10:30 AM",
    ): TeacherAttendanceSession {
        val students = studentsInClass().mapIndexed { index, student ->
            val mark = when (index) {
                0, 3 -> TeacherAttendanceMark.Present
                1, 4 -> TeacherAttendanceMark.Absent
                else -> TeacherAttendanceMark.None
            }
            TeacherAttendanceStudent(student = student, mark = mark)
        }
        return TeacherAttendanceSession(
            classLabel = classLabel,
            dateLabel = dateLabel,
            subjectLabel = subjectLabel,
            timeLabel = timeLabel,
            students = students,
        )
    }

    fun calendarEvents(): List<CalendarEvent> = listOf(
        CalendarEvent(
            title = "Unit Test — Chemistry",
            dateLabel = "Wed, 22 Jul",
            timeLabel = "09:00 – 11:00",
            type = AcademicEventType.Exam,
            dayOfMonth = 22,
            yearMonth = "2026-07",
        ),
        CalendarEvent(
            title = "Staff Meeting",
            dateLabel = "Tue, 14 Jul",
            timeLabel = "15:00 – 16:00",
            type = AcademicEventType.Event,
            dayOfMonth = 14,
            yearMonth = "2026-07",
        ),
        CalendarEvent(
            title = "Independence Day Holiday",
            dateLabel = "Wed, 22 Jul",
            timeLabel = "All day",
            type = AcademicEventType.Holiday,
            dayOfMonth = 15,
            yearMonth = "2026-08",
        ),
        CalendarEvent(
            title = "Parent Orientation",
            dateLabel = "Sat, 25 Jul",
            timeLabel = "10:00 – 12:00",
            type = AcademicEventType.Event,
            dayOfMonth = 25,
            yearMonth = "2026-07",
        ),
        CalendarEvent(
            title = "Mid-term Exam",
            dateLabel = "Fri, 10 Jul",
            timeLabel = "09:00 – 12:00",
            type = AcademicEventType.Exam,
            dayOfMonth = 10,
            yearMonth = "2026-07",
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

    fun chapterStats(): SubjectChapterStats = SubjectChapterStats(
        mastery = "100%",
        reading = "100%",
        quizCount = "50",
        quickCheckCount = "4",
    )

    fun chapters(subjectId: String = "chemistry"): List<ChapterItem> = when (subjectId) {
        "mathematics" -> listOf(
            ChapterItem(
                id = "math-1",
                title = "CHAPTER: 1 Trigonometry",
                description = "Ratios, identities, and heights and distances.",
            ),
            ChapterItem(
                id = "math-2",
                title = "CHAPTER: 2 Sequences and Series",
                description = "Arithmetic and geometric progressions.",
            ),
            ChapterItem(
                id = "math-3",
                title = "CHAPTER: 3 Probability",
                description = "Random experiments, events, and simple probability.",
            ),
        )
        "physics" -> listOf(
            ChapterItem(
                id = "phy-1",
                title = "CHAPTER: 1 Motion in a Straight Line",
                description = "Displacement, velocity, and acceleration.",
            ),
            ChapterItem(
                id = "phy-2",
                title = "CHAPTER: 2 Laws of Motion",
                description = "Newton's laws and applications of force.",
            ),
            ChapterItem(
                id = "phy-3",
                title = "CHAPTER: 3 Work, Energy and Power",
                description = "Work-energy theorem and conservation of energy.",
            ),
        )
        else -> listOf(
            ChapterItem(
                id = "chem-1",
                title = "CHAPTER: 1 Solutions",
                description = "This unit covers the formation of different types of soluti....",
            ),
            ChapterItem(
                id = "chem-2",
                title = "CHAPTER: 2 Electrochemistry",
                description = "Study of production of electricity from chemical reactio....",
            ),
            ChapterItem(
                id = "chem-3",
                title = "CHAPTER: 3 Chemical Kinetics",
                description = "This unit chemical kinetics, helping to understand how c....",
            ),
        )
    }

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

    fun teachingTimetable(): TeacherTeachingTimetable {
        val times = listOf(
            "01:00 PM - 02:30 PM",
            "03:00 PM - 04:30 PM",
            "04:30 PM - 06:00 PM",
            "06:00 PM - 07:30 PM",
            "07:30 PM - 09:00 PM",
            "09:00 PM - 10:30 PM",
            "10:30 PM - 12:00 AM",
            "12:00 AM - 01:30 AM",
        )
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val booked = mapOf(
            (1 to 1) to TeacherTimetableCell("Chemistry", "Class 12 (Chemistry)"),
            (1 to 3) to TeacherTimetableCell("Chemistry", "Class 12 (Chemistry)"),
            (2 to 2) to TeacherTimetableCell("Chemistry", "Class 12 (Chemistry)"),
            (4 to 1) to TeacherTimetableCell("Chemistry", "Class 12 (Chemistry)"),
            (1 to 0) to TeacherTimetableCell("Chemistry", "Class 12 (Chemistry)"),
            (2 to 5) to TeacherTimetableCell("Chemistry", "Class 12 (Chemistry)"),
        )
        return TeacherTeachingTimetable(
            classes = listOf("Class XII", "Class XI", "Class X", "Class IX"),
            days = days,
            timeSlots = times,
            cells = booked,
        )
    }

    fun announcementAudiences(): List<TeacherAnnouncementAudience> = listOf(
        TeacherAnnouncementAudience(
            id = "all",
            title = "Select all classes",
            subtitle = "Applies to all listed classes.",
            isSelectAll = true,
        ),
        TeacherAnnouncementAudience(
            id = "xi_chem",
            title = "Class XI (Chemistry)",
            subtitle = "6 students",
        ),
        TeacherAnnouncementAudience(
            id = "xii_chem",
            title = "Class XII (Chemistry)",
            subtitle = "9 students",
        ),
    )

    fun announcements(): List<TeacherAnnouncement> = emptyList()

    fun notifications(): List<AppNotification> = listOf(
        AppNotification(
            id = "tn1",
            title = "Class starting soon",
            body = "Class 12 (Chemistry): starts at 1:00 pm (in 30 min)",
            timestampLabel = "Unread 2026-07-25T06:55:02.590Z",
            unread = true,
            section = NotificationSection.Earlier,
        ),
        AppNotification(
            id = "tn2",
            title = "Class starting soon",
            body = "Class 11 (Chemistry): starts at 3:00 pm (in 30 min)",
            timestampLabel = "Unread 2026-07-10T06:55:02.590Z",
            unread = true,
            section = NotificationSection.Earlier,
        ),
    )

    fun accountEmail(): String = "Fakeavangchhia@gmail.com"

    fun registeredDevices(): List<RegisteredDevice> = listOf(
        RegisteredDevice("iOS", "7th Aug 2026, 12:06", "--"),
        RegisteredDevice("Web", "7th Aug 2026, 12:32", "--"),
        RegisteredDevice("Android", "10th Aug 2026, 11:06", "Yes"),
    )

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

    fun aiChatSession(subjectId: String): AiChatSession = aiChatSessionFor(subjectId)

    fun dayPeriods(): List<TeacherDayPeriod> = listOf(
        TeacherDayPeriod("9:00 AM - 9:40 AM", "Science", "No roll. yet", emphasized = false),
        TeacherDayPeriod("9:00 AM - 10:40 AM", "English", "3/4 present · 1 absent · 0 leave", emphasized = true),
        TeacherDayPeriod("10:40 AM - 11:30 AM", "Science", "No roll. yet", emphasized = false),
        TeacherDayPeriod("11:40 AM - 12:30 PM", "English", "3/4 present · 1 absent · 0 leave", emphasized = true),
        TeacherDayPeriod("12:00 PM - 1:10 PM", "Break", null, emphasized = false),
        TeacherDayPeriod("1:20 PM - 2:00 PM", "Mathematics", "3/4 present · 1 absent · 0 leave", emphasized = true),
        TeacherDayPeriod("2:10 PM - 2:50 PM", "English", "3/4 present · 1 absent · 0 leave", emphasized = true),
        TeacherDayPeriod("3:00 PM - 3:50 PM", "Science", "No roll. yet", emphasized = false),
    )

    fun timetableSubjects(): List<String> =
        listOf("Mathematics", "Chemistry", "Economics", "Biology")
}

data class TeacherDayPeriod(
    val timeLabel: String,
    val title: String,
    val attendanceLabel: String?,
    val emphasized: Boolean,
)

data class TeacherTimetableCell(
    val subject: String,
    val detail: String,
)

data class TeacherTeachingTimetable(
    val classes: List<String>,
    val days: List<String>,
    val timeSlots: List<String>,
    /** Key = timeRowIndex to dayIndex */
    val cells: Map<Pair<Int, Int>, TeacherTimetableCell>,
)

data class TeacherAnnouncementAudience(
    val id: String,
    val title: String,
    val subtitle: String,
    val isSelectAll: Boolean = false,
)

data class TeacherAnnouncement(
    val id: String,
    val subject: String,
    val body: String,
    val priority: String,
    val audienceLabel: String,
    val sentAtLabel: String,
)

private fun SchoolClass.displayLabel(): String = when (this) {
    SchoolClass.IX -> "Class IX"
    SchoolClass.X -> "Class X"
    SchoolClass.XI -> "Class XI"
    SchoolClass.XII -> "Class XII"
}

private fun mockGroupCode(schoolClass: SchoolClass, subject: SubjectOption): String {
    val preset = when (schoolClass to subject) {
        SchoolClass.XII to SubjectOption.Chemistry -> "J3QUE3"
        SchoolClass.XI to SubjectOption.Chemistry -> "BFTNHE"
        else -> null
    }
    if (preset != null) return preset
    val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val seed = "${schoolClass.name}${subject.name}".hashCode().toUInt()
    return buildString {
        var value = seed
        repeat(6) {
            append(alphabet[(value % alphabet.length.toUInt()).toInt()])
            value /= alphabet.length.toUInt()
            if (value == 0u) value = seed xor (it + 1).toUInt()
        }
    }
}

private fun subjectIconFor(name: String): Int {
    val key = name.lowercase(java.util.Locale.ENGLISH)
    return when {
        "chem" in key -> R.drawable.ic_subject_chemistry
        "math" in key -> R.drawable.ic_subject_mathematics
        "phys" in key -> R.drawable.ic_subject_physics
        "bio" in key -> R.drawable.ic_subject_biology
        else -> R.drawable.ic_metric_subject
    }
}
