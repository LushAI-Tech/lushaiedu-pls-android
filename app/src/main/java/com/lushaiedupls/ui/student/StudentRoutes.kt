package com.lushaiedupls.ui.student

import android.net.Uri

object StudentRoutes {
    const val HOME = "student_home"
    const val CALENDAR = "student_calendar"
    const val AI = "student_ai"
    const val AI_CHAPTERS = "student_ai_chapters/{subjectId}?name={name}"
    const val AI_CHATS = "student_ai_chats/{subjectId}?chapterId={chapterId}"
    const val ATTENDANCE = "student_attendance"
    const val MENU = "student_menu"
    const val ACCOUNT = "student_account"
    const val LINK_PARENT = "student_link_parent"
    const val PRIVACY = "student_privacy"
    const val TERMS = "student_terms"
    const val MORE = "student_more"
    const val NOTIFICATIONS = "student_notifications"
    const val CHAPTERS = "student_chapters"
    const val QUIZ = "student_quiz?chapterId={chapterId}&sectionId={sectionId}"
    const val TIMETABLE = "student_timetable"

    fun aiChapters(subjectId: String, name: String = ""): String =
        "student_ai_chapters/$subjectId?name=${Uri.encode(name)}"

    fun aiChats(subjectId: String, chapterId: String = ""): String =
        "student_ai_chats/$subjectId?chapterId=$chapterId"

    fun quiz(chapterId: String = "", sectionId: String = "", sectionIds: List<String> = emptyList()): String {
        val sections = sectionIds.filter { it.isNotBlank() }.ifEmpty {
            listOfNotNull(sectionId.takeIf { it.isNotBlank() })
        }.joinToString(",")
        return "student_quiz?chapterId=$chapterId&sectionId=${Uri.encode(sections)}"
    }
}
