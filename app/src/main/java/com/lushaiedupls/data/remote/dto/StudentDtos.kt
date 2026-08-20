package com.lushaiedupls.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AiSnapshot(
    val available: Boolean = true,
    val stem_mastery_pct: Double? = null,
    val reading_progress_pct: Double? = null,
    val quizzes_completed: Int? = null,
    val subjects_in_progress: Int? = null,
    val chapters_in_progress: Int? = null,
)

@Serializable
data class AiSubjectOut(
    val subject_id: String,
    val stem_subject_id: String,
    val name: String,
    val code: String? = null,
    val class_id: String,
    val class_name: String,
)

@Serializable
data class AnswerResult(
    val question_id: String,
    val student_answer: String,
    val correct_answer: String? = null,
    val is_correct: Boolean,
    val marks_awarded: Double = 0.0,
    val explanation: String? = null,
)

@Serializable
data class AnswerSubmission(
    val question_id: String,
    val student_answer: String,
    val time_spent_seconds: Int = 0,
)

@Serializable
data class AttendanceCalendar(
    val month: String,
    val days: List<DayStatus>,
)

@Serializable
data class AttendanceTotals(
    val present: Int = 0,
    val absent: Int = 0,
    val leave: Int = 0,
    val sessions: Int = 0,
    val present_pct_all: Double = 0.0,
    val present_pct_excl_leave: Double = 0.0,
)

@Serializable
data class CalendarEventOut(
    val id: String,
    val title: String,
    val event_type: CalendarEventType,
    val start_date: String,
    val end_date: String,
    val description: String? = null,
    val created_at: String,
)

@Serializable
data class ChapterListItem(
    val id: String,
    val textbook_id: String,
    val title: String,
    val chapter_number: Int,
    val description: String? = null,
    val is_active: Boolean = true,
    val structure_pending: Boolean = false,
)

@Serializable
data class ChapterOut(
    val id: String,
    val textbook_id: String,
    val title: String,
    val chapter_number: Int,
    val description: String? = null,
    val is_active: Boolean = true,
    val structure_pending: Boolean = false,
    val sections: List<SectionOut> = emptyList(),
)

@Serializable
data class ChatHistoryResponse(
    val chapter_id: String,
    val messages: List<ChatMessage>,
)

@Serializable
data class ChatMessage(
    val id: String? = null,
    val role: String,
    val message: String,
    val timestamp: String? = null,
    val concept_check: ConceptCheck? = null,
    val suggestions: List<String> = emptyList(),
    val quick_check_attempt: QuickCheckAttemptSummary? = null,
)

@Serializable
data class ChatRequest(
    val message: String,
    val section_id: String? = null,
    val content_block_id: String? = null,
    val response_language: String = "en",
)

@Serializable
data class ChatResponse(
    val message: String,
    val referenced_blocks: List<String> = emptyList(),
    val concept_check: ConceptCheck? = null,
    val suggestions: List<String> = emptyList(),
    val message_id: String? = null,
)

@Serializable
data class ClassOut(
    val id: String,
    val name: String,
    val sort_order: Int,
    val is_active: Boolean,
)

@Serializable
data class ClearChatHistoryResponse(
    val chapter_id: String,
    val cleared_count: Int,
)

@Serializable
data class ConceptCheck(
    val question: String,
    val options: List<String>,
    val correct: Int,
    val explanation: String,
)

@Serializable
data class ContentBlockOut(
    val id: String,
    val section_id: String,
    val block_type: String,
    val sort_order: Int,
    val title: String? = null,
    val content_text: String? = null,
    val figure_ref: String? = null,
    val ai_image_url: String? = null,
    val original_image_url: String? = null,
    val page_number: Int? = null,
)

@Serializable
data class DayStatus(
    val day: String,
    val status: AttendanceStatus,
    val is_extra_class: Boolean = false,
    val teaching_unit_id: String,
    val subject_name: String,
)

@Serializable
data class LinkTokenResponse(
    val token: String,
    val expires_at: String,
    val expires_in_seconds: Int,
)

@Serializable
data class NotificationOut(
    val id: String,
    val title: String,
    val body: String,
    val audience: NotificationAudience,
    val teaching_unit_id: String? = null,
    val teaching_unit_label: String? = null,
    val published_at: String,
    val expires_at: String? = null,
    val author_name: String? = null,
    val is_read: Boolean = false,
    val push_recipient_count: Int? = null,
)

@Serializable
data class RedeemLinkRequest(
    val token: String,
    val relationship: ParentRelationship = ParentRelationship.GUARDIAN,
)

@Serializable
data class LinkedStudentOut(
    val student: UserSummary,
    val relationship: ParentRelationship,
    val class_name: String? = null,
    val subjects: List<String> = emptyList(),
    val linked_at: String? = null,
)

@Serializable
data class ParentChildSummary(
    val student: UserSummary,
    val class_name: String? = null,
    val subjects: List<String> = emptyList(),
    val overall: AttendanceTotals,
    val ai: AiSnapshot,
)

@Serializable
data class ParentOverview(
    val parent: UserSummary,
    val month: String,
    val children: List<ParentChildSummary> = emptyList(),
    val unread_notifications: Int = 0,
)

@Serializable
data class ParentLinkOut(
    val id: String,
    val parent: UserSummary,
    val student: UserSummary,
    val relationship: ParentRelationship,
    val status: ParentLinkStatus,
    val linked_at: String? = null,
)

@Serializable
data class PeriodOut(
    val id: String,
    val name: String,
    val start_time: String,
    val end_time: String,
    val sort_order: Int,
    val is_active: Boolean,
)

@Serializable
data class ProgressDashboardResponse(
    val subjects_in_progress: Int = 0,
    val chapters_in_progress: Int = 0,
    val overall_mastery_pct: Double = 0.0,
    val overall_progress_pct: Double = 0.0,
    val mastered_sections: Int = 0,
    val total_sections: Int = 0,
    val quick_check_attempts: Int = 0,
    val quick_check_correct: Int = 0,
    val quick_check_accuracy_pct: Double? = null,
    val quizzes_completed: Int = 0,
    val last_accessed_at: String? = null,
)

@Serializable
data class ProgressUpdateRequest(
    val textbook_id: String,
    val chapter_id: String? = null,
    val section_id: String? = null,
    val content_block_id: String? = null,
    val progress_pct: Double? = null,
)

@Serializable
data class QuickCheckAttemptSummary(
    val selected_index: Int,
    val is_correct: Boolean,
)

@Serializable
data class QuizAttemptSummary(
    val id: String,
    val quiz_mode: String,
    val attempt_number: Int,
    val pool_group: Int,
    val score: Int? = null,
    val max_score: Int? = null,
    val total_questions: Int,
    val time_limit_seconds: Int? = null,
    val time_taken_seconds: Int? = null,
    val started_at: String,
    val completed_at: String? = null,
)

@Serializable
data class QuizQuestionOut(
    val id: String,
    val question_text: String,
    val question_type: String,
    val options: JsonElement? = null,
    val difficulty: String,
    val pool_group: Int,
    val marks: Int = 1,
    val negative_marks: Double = 0.0,
)

@Serializable
data class QuizStartResponse(
    val attempt_id: String,
    val section_id: String? = null,
    val chapter_id: String? = null,
    val quiz_mode: String,
    val attempt_number: Int,
    val pool_group: Int,
    val time_limit_seconds: Int? = null,
    val total_questions: Int,
    val questions: List<QuizQuestionOut>,
)

@Serializable
data class QuizSubmitRequest(
    val attempt_id: String,
    val answers: List<AnswerSubmission>,
    val time_taken_seconds: Int? = null,
)

@Serializable
data class QuizSubmitResponse(
    val attempt_id: String,
    val quiz_mode: String,
    val score: Double,
    val max_score: Double,
    val total_questions: Int,
    val correct: Int,
    val incorrect: Int,
    val unanswered: Int,
    val negative_total: Double = 0.0,
    val percentage: Double,
    val time_taken_seconds: Int? = null,
    val results: List<AnswerResult>,
    val suggestions: QuizSuggestions? = null,
)

@Serializable
data class QuizSuggestions(
    val passed: Boolean = false,
    val message: String = "",
    val weak_concepts: List<JsonElement> = emptyList(),
    val next_steps: List<String> = emptyList(),
    val recommended_retake: Boolean = false,
)

@Serializable
data class RecentAttendanceRow(
    val day: String,
    val class_name: String,
    val subject_name: String,
    val status: AttendanceStatus,
)

@Serializable
data class ResumeResponse(
    val chapter_id: String? = null,
    val chapter_title: String? = null,
    val chapter_number: Int? = null,
    val section_id: String? = null,
    val section_title: String? = null,
    val section_number: String? = null,
    val content_block_id: String? = null,
    val content_block_title: String? = null,
    val content_block_type: String? = null,
    val progress_pct: Double = 0.0,
    val last_accessed_at: String? = null,
)

@Serializable
data class SectionOut(
    val id: String,
    val chapter_id: String,
    val parent_section_id: String? = null,
    val title: String,
    val section_number: String? = null,
    val sort_order: Int,
    val depth: Int,
    val content_blocks: List<ContentBlockOut> = emptyList(),
    val subsections: List<SectionOut> = emptyList(),
)

@Serializable
data class StudentAttendanceSummary(
    val student_id: String,
    val month: String,
    val overall: AttendanceTotals,
    val regular: AttendanceTotals,
    val extra: AttendanceTotals,
    val by_subject: List<SubjectBreakdown> = emptyList(),
)

@Serializable
data class StudentOverview(
    val student: UserSummary,
    val class_name: String? = null,
    val subject_count: Int = 0,
    val ai: AiSnapshot,
    val overall: AttendanceTotals,
    val by_subject: List<SubjectBreakdown> = emptyList(),
    val recent_attendance: List<RecentAttendanceRow> = emptyList(),
    val unread_notifications: Int = 0,
)

@Serializable
data class SubjectBreakdown(
    val teaching_unit_id: String,
    val class_name: String,
    val subject_name: String,
    val totals: AttendanceTotals,
)

@Serializable
data class SubjectOut(
    val id: String,
    val class_id: String,
    val name: String,
    val code: String? = null,
    val sort_order: Int,
    val is_active: Boolean,
    val stem_subject_id: String? = null,
    val ai_enabled: Boolean = false,
)

@Serializable
data class TeachingUnitOut(
    val id: String,
    val class_id: String,
    val subject_id: String,
    val class_name: String,
    val subject_name: String,
    val teacher: UserSummary? = null,
    val status: TeachingUnitStatus = TeachingUnitStatus.ACTIVE,
    val student_count: Int = 0,
    val ai_enabled: Boolean = false,
)

@Serializable
data class UnreadCountResponse(
    val unread: Int,
)

@Serializable
data class UserSummary(
    val id: String,
    val name: String,
    val email: String? = null,
    val avatar_url: String? = null,
)

@Serializable
data class WeekSlot(
    val slot_id: String,
    val teaching_unit_id: String,
    val class_name: String,
    val subject_name: String,
    val teacher_name: String? = null,
    val period_id: String,
    val period_name: String,
    val start_time: String,
    val end_time: String,
    val day_of_week: DayOfWeek,
    val room: String? = null,
)

@Serializable
data class WeekView(
    val periods: List<PeriodOut>,
    val days: Map<String, List<WeekSlot>>,
)
