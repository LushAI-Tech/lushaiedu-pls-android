package com.lushaiedupls.ui.teacher

import android.net.Uri

object TeacherRoutes {
    const val HOME = "teacher_home"
    const val MY_GROUPS = "teacher_my_groups"
    const val OVERVIEW = "teacher_overview"
    const val CLASS_OVERVIEW = "teacher_class_overview/{groupId}"
    const val TAKE_ATTENDANCE = "teacher_take_attendance/{unitId}/{date}"
    const val CALENDAR = "teacher_calendar"
    const val ACADEMIC_CALENDAR = "teacher_academic_calendar"
    const val AI = "teacher_ai"
    const val AI_CHAPTERS = "teacher_ai_chapters/{subjectId}?name={name}"
    const val AI_CHATS = "teacher_ai_chats/{subjectId}?chapterId={chapterId}"
    const val MENU = "teacher_menu"
    const val MORE = "teacher_more"
    const val CHAPTERS = "teacher_chapters"
    const val QUIZ = "teacher_quiz?chapterId={chapterId}&sectionId={sectionId}"
    const val TIMETABLE = "teacher_timetable"
    const val ANNOUNCEMENTS = "teacher_announcements"
    const val NEW_ANNOUNCEMENT = "teacher_new_announcement"
    const val ACCOUNT = "teacher_account"
    const val PRIVACY = "teacher_privacy"
    const val TERMS = "teacher_terms"
    const val NOTIFICATIONS = "teacher_notifications"

    fun classOverview(groupId: String): String = "teacher_class_overview/$groupId"

    fun takeAttendance(unitId: String, date: String): String =
        "teacher_take_attendance/${Uri.encode(unitId)}/${Uri.encode(date)}"

    fun aiChapters(subjectId: String, name: String = ""): String =
        "teacher_ai_chapters/$subjectId?name=${Uri.encode(name)}"

    fun aiChats(subjectId: String, chapterId: String = ""): String =
        "teacher_ai_chats/$subjectId?chapterId=$chapterId"

    fun quiz(chapterId: String = "", sectionId: String = "", sectionIds: List<String> = emptyList()): String {
        val sections = sectionIds.filter { it.isNotBlank() }.ifEmpty {
            listOfNotNull(sectionId.takeIf { it.isNotBlank() })
        }.joinToString(",")
        return "teacher_quiz?chapterId=$chapterId&sectionId=${Uri.encode(sections)}"
    }
}
