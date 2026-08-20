package com.lushaiedupls.data.repository

import android.content.Context
import android.net.Uri
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.api.AiApi
import com.lushaiedupls.data.remote.api.AttendanceApi
import com.lushaiedupls.data.remote.api.CalendarApi
import com.lushaiedupls.data.remote.api.ClassesApi
import com.lushaiedupls.data.remote.api.MeApi
import com.lushaiedupls.data.remote.api.NotificationsApi
import com.lushaiedupls.data.remote.api.OverviewApi
import com.lushaiedupls.data.remote.api.ParentApi
import com.lushaiedupls.data.remote.api.TeachingUnitsApi
import com.lushaiedupls.data.remote.api.TimetableApi
import com.lushaiedupls.data.remote.device.DeviceIdProvider
import com.lushaiedupls.data.remote.dto.AiSubjectOut
import com.lushaiedupls.data.remote.dto.AnswerSubmission
import com.lushaiedupls.data.remote.dto.AttendanceCalendar
import com.lushaiedupls.data.remote.dto.AvatarCommitRequest
import com.lushaiedupls.data.remote.dto.AvatarPresignRequest
import com.lushaiedupls.data.remote.dto.CalendarEventOut
import com.lushaiedupls.data.remote.dto.ChapterAttachmentOut
import com.lushaiedupls.data.remote.dto.ChapterListItem
import com.lushaiedupls.data.remote.dto.ChapterOut
import com.lushaiedupls.data.remote.dto.ChatHistoryResponse
import com.lushaiedupls.data.remote.dto.ChatRequest
import com.lushaiedupls.data.remote.dto.ChatResponse
import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.ClearChatHistoryResponse
import com.lushaiedupls.data.remote.dto.DeviceOut
import com.lushaiedupls.data.remote.dto.ExamPrepPyqsResponse
import com.lushaiedupls.data.remote.dto.Gender
import com.lushaiedupls.data.remote.dto.LinkTokenResponse
import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.NotificationOut
import com.lushaiedupls.data.remote.dto.ParentLinkOut
import com.lushaiedupls.data.remote.dto.ProfileUpdate
import com.lushaiedupls.data.remote.dto.ProgressDashboardResponse
import com.lushaiedupls.data.remote.dto.QuizAttemptSummary
import com.lushaiedupls.data.remote.dto.QuizStartResponse
import com.lushaiedupls.data.remote.dto.QuizSubmitRequest
import com.lushaiedupls.data.remote.dto.QuizSubmitResponse
import com.lushaiedupls.data.remote.dto.ResumeResponse
import com.lushaiedupls.data.remote.dto.SectionOut
import com.lushaiedupls.data.remote.dto.StudentAttendanceSummary
import com.lushaiedupls.data.remote.dto.StudentOverview
import com.lushaiedupls.data.remote.dto.SubjectOut
import com.lushaiedupls.data.remote.dto.SubjectPracticeQuestionsResponse
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.UnreadCountResponse
import com.lushaiedupls.data.remote.dto.UserOut
import com.lushaiedupls.data.remote.dto.WeekView
import com.lushaiedupls.data.remote.safeApiCall
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class StudentRepository(
    private val overviewApi: OverviewApi,
    private val attendanceApi: AttendanceApi,
    private val calendarApi: CalendarApi,
    private val timetableApi: TimetableApi,
    private val notificationsApi: NotificationsApi,
    private val meApi: MeApi,
    private val classesApi: ClassesApi,
    private val aiApi: AiApi,
    private val parentApi: ParentApi,
    private val teachingUnitsApi: TeachingUnitsApi,
    private val deviceIdProvider: DeviceIdProvider,
) {    suspend fun overview(month: String? = null): NetworkResult<StudentOverview> =
        safeApiCall { overviewApi.studentOverview(month) }

    suspend fun attendanceSummary(month: String? = null): NetworkResult<StudentAttendanceSummary> =
        safeApiCall { attendanceApi.mySummary(month) }

    suspend fun attendanceCalendar(month: String? = null): NetworkResult<AttendanceCalendar> =
        safeApiCall { attendanceApi.myCalendar(month) }

    suspend fun calendarEvents(from: String? = null, to: String? = null): NetworkResult<List<CalendarEventOut>> =
        safeApiCall { calendarApi.events(from, to) }

    suspend fun timetable(teachingUnitId: String? = null): NetworkResult<WeekView> =
        safeApiCall { timetableApi.myTimetable(teachingUnitId) }

    suspend fun notifications(limit: Int = 50, offset: Int = 0): NetworkResult<List<NotificationOut>> =
        safeApiCall { notificationsApi.list(limit, offset) }

    suspend fun unreadCount(): NetworkResult<UnreadCountResponse> =
        safeApiCall { notificationsApi.unreadCount() }

    suspend fun markNotificationRead(id: String): NetworkResult<MessageResponse> =
        safeApiCall { notificationsApi.markRead(id) }

    suspend fun profile(): NetworkResult<UserOut> = safeApiCall { meApi.me() }

    suspend fun updateProfile(
        name: String? = null,
        phone: String? = null,
        gender: Gender? = null,
        address: String? = null,
    ): NetworkResult<UserOut> = safeApiCall {
        meApi.updateProfile(
            ProfileUpdate(
                name = name,
                phone = phone,
                gender = gender,
                address = address,
            ),
        )
    }

    suspend fun devices(): NetworkResult<List<DeviceOut>> =
        safeApiCall { meApi.devices(deviceIdProvider.deviceId()) }

    suspend fun signOutAllDevices(): NetworkResult<MessageResponse> =
        safeApiCall { meApi.signOutAllDevices() }

    /**
     * Upload a photo to the avatar storage:
     * 1. Presign → get a PUT URL + required headers
     * 2. PUT the raw bytes directly to S3/R2 (no auth header — the URL is already signed)
     * 3. Commit the object_key to the backend so UserOut.avatar_url is updated
     */
    suspend fun uploadAvatar(uri: Uri, context: Context): NetworkResult<UserOut> {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return NetworkResult.Exception(Exception("Cannot read image"))

        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

        val presignResult = safeApiCall {
            meApi.avatarPresign(
                AvatarPresignRequest(
                    content_type = mimeType,
                    content_length = bytes.size,
                ),
            )
        }
        if (presignResult !is NetworkResult.Success) {
            @Suppress("UNCHECKED_CAST")
            return presignResult as NetworkResult<UserOut>
        }
        val presign = presignResult.data

        // PUT to storage directly — no Authorization header here (URL is pre-signed)
        val putRequestBuilder = Request.Builder().url(presign.upload_url)
        presign.required_headers.forEach { (k, v) -> putRequestBuilder.addHeader(k, v) }
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        putRequestBuilder.put(body)

        val putOk = runCatching {
            OkHttpClient().newCall(putRequestBuilder.build()).execute().use { it.isSuccessful }
        }.getOrElse { return NetworkResult.Exception(it) }

        if (!putOk) return NetworkResult.Exception(Exception("Failed to upload image to storage"))

        return safeApiCall { meApi.avatarCommit(AvatarCommitRequest(object_key = presign.object_key)) }
    }

    private val chaptersCache = java.util.concurrent.ConcurrentHashMap<String, List<ChapterListItem>>()
    private val chapterCache = java.util.concurrent.ConcurrentHashMap<String, ChapterOut>()
    private val sectionCache = java.util.concurrent.ConcurrentHashMap<String, SectionOut>()
    private val chatHistoryCache = java.util.concurrent.ConcurrentHashMap<String, ChatHistoryResponse>()
    private val chatIntroCache = java.util.concurrent.ConcurrentHashMap<String, ChatResponse>()
    private val questionsListCache = java.util.concurrent.ConcurrentHashMap<String, SubjectPracticeQuestionsResponse>()
    private val examPrepPyqsCache = java.util.concurrent.ConcurrentHashMap<String, ExamPrepPyqsResponse>()
    private val chapterAttachmentsCache = java.util.concurrent.ConcurrentHashMap<String, List<ChapterAttachmentOut>>()

    suspend fun classes(): NetworkResult<List<ClassOut>> = safeApiCall { classesApi.listClasses() }

    suspend fun questionsList(
        subjectId: String,
        forceRefresh: Boolean = false,
    ): NetworkResult<SubjectPracticeQuestionsResponse> {
        if (!forceRefresh) {
            questionsListCache[subjectId]?.let { return NetworkResult.Success(it) }
        }
        val result = safeApiCall { aiApi.questionsList(subjectId) }
        if (result is NetworkResult.Success) {
            questionsListCache[subjectId] = result.data
        }
        return result
    }

    suspend fun chapterAttachments(
        chapterId: String,
        forceRefresh: Boolean = false,
    ): NetworkResult<List<ChapterAttachmentOut>> {
        if (!forceRefresh) {
            chapterAttachmentsCache[chapterId]?.let { return NetworkResult.Success(it) }
        }
        val result = safeApiCall { aiApi.chapterAttachments(chapterId) }
        if (result is NetworkResult.Success) {
            chapterAttachmentsCache[chapterId] = result.data
        }
        return result
    }

    suspend fun examPrepPyqs(
        chapterId: String,
        sectionId: String? = null,
        chapterScope: Boolean? = null,
        examCodes: String? = null,
        forceRefresh: Boolean = false,
    ): NetworkResult<ExamPrepPyqsResponse> {
        val cacheKey = "${chapterId}_${sectionId}_${chapterScope}_${examCodes}"
        if (!forceRefresh) {
            examPrepPyqsCache[cacheKey]?.let { return NetworkResult.Success(it) }
        }
        val result = safeApiCall {
            aiApi.examPrepPyqs(
                chapterId = chapterId,
                sectionId = sectionId,
                chapterScope = chapterScope,
                examCodes = examCodes,
            )
        }
        if (result is NetworkResult.Success) {
            examPrepPyqsCache[cacheKey] = result.data
        }
        return result
    }

    suspend fun subjects(classId: String): NetworkResult<List<SubjectOut>> =
        safeApiCall { classesApi.listSubjects(classId) }

    suspend fun aiSubjects(): NetworkResult<List<AiSubjectOut>> = safeApiCall { aiApi.subjects() }

    suspend fun teachingUnits(): NetworkResult<List<TeachingUnitOut>> =
        safeApiCall { teachingUnitsApi.list() }

    suspend fun progressDashboard(): NetworkResult<ProgressDashboardResponse> =
        safeApiCall { aiApi.progressDashboard() }

    suspend fun progressResume(): NetworkResult<ResumeResponse?> =
        safeApiCall { aiApi.progressResume() }

    suspend fun chapters(subjectId: String, forceRefresh: Boolean = false): NetworkResult<List<ChapterListItem>> {
        if (!forceRefresh) {
            chaptersCache[subjectId]?.let { return NetworkResult.Success(it) }
        }
        val result = safeApiCall { aiApi.chapters(subjectId) }
        if (result is NetworkResult.Success) {
            chaptersCache[subjectId] = result.data
        }
        return result
    }

    suspend fun chapter(chapterId: String, forceRefresh: Boolean = false): NetworkResult<ChapterOut> {
        if (!forceRefresh) {
            chapterCache[chapterId]?.let { return NetworkResult.Success(it) }
        }
        val result = safeApiCall { aiApi.chapter(chapterId) }
        if (result is NetworkResult.Success) {
            chapterCache[chapterId] = result.data
        }
        return result
    }

    suspend fun section(sectionId: String, forceRefresh: Boolean = false): NetworkResult<SectionOut> {
        if (!forceRefresh) {
            sectionCache[sectionId]?.let { return NetworkResult.Success(it) }
        }
        val result = safeApiCall { aiApi.section(sectionId) }
        if (result is NetworkResult.Success) {
            sectionCache[sectionId] = result.data
        }
        return result
    }

    suspend fun chatIntro(
        chapterId: String,
        language: String,
        forceRefresh: Boolean = false,
    ): NetworkResult<ChatResponse> {
        val key = "${chapterId}_$language"
        if (!forceRefresh) {
            chatIntroCache[key]?.let { return NetworkResult.Success(it) }
        }
        val result = safeApiCall { aiApi.intro(chapterId, language) }
        if (result is NetworkResult.Success) {
            chatIntroCache[key] = result.data
        }
        return result
    }

    suspend fun chatHistory(chapterId: String, forceRefresh: Boolean = false): NetworkResult<ChatHistoryResponse> {
        if (!forceRefresh) {
            chatHistoryCache[chapterId]?.let { return NetworkResult.Success(it) }
        }
        val result = safeApiCall { aiApi.history(chapterId) }
        if (result is NetworkResult.Success) {
            chatHistoryCache[chapterId] = result.data
        }
        return result
    }

    suspend fun prefetchAiChat(chapterIds: List<String>, language: String = "en") {
        try {
            coroutineScope {
                chapterIds.take(3).forEach { chapterId ->
                    launch {
                        chapter(chapterId)
                        chapterAttachments(chapterId)
                        examPrepPyqs(chapterId)
                        val history = chatHistory(chapterId)
                        if (history is NetworkResult.Success && history.data.messages.isEmpty()) {
                            chatIntro(chapterId, language)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Best effort prefetch
        }
    }

    suspend fun chat(
        chapterId: String,
        message: String,
        language: String,
        sectionId: String? = null,
        contentBlockId: String? = null,
    ): NetworkResult<ChatResponse> = safeApiCall {
        aiApi.chat(
            chapterId,
            ChatRequest(
                message = message,
                section_id = sectionId,
                content_block_id = contentBlockId,
                response_language = language,
            ),
        )
    }

    suspend fun clearChat(chapterId: String): NetworkResult<ClearChatHistoryResponse> {
        chatHistoryCache.remove(chapterId)
        chatIntroCache.keys.filter { it.startsWith("${chapterId}_") }.forEach { chatIntroCache.remove(it) }
        return safeApiCall { aiApi.clearHistory(chapterId) }
    }

    suspend fun quizChapter(chapterId: String): NetworkResult<QuizStartResponse> =
        safeApiCall { aiApi.quizChapter(chapterId) }

    suspend fun quizSection(sectionId: String): NetworkResult<QuizStartResponse> =
        safeApiCall { aiApi.quizSection(sectionId) }

    suspend fun quizHistory(): NetworkResult<List<QuizAttemptSummary>> =
        safeApiCall { aiApi.quizHistory() }

    suspend fun submitQuiz(
        attemptId: String,
        answers: List<AnswerSubmission>,
        timeTakenSeconds: Int? = null,
    ): NetworkResult<QuizSubmitResponse> = safeApiCall {
        aiApi.quizSubmit(
            QuizSubmitRequest(
                attempt_id = attemptId,
                answers = answers,
                time_taken_seconds = timeTakenSeconds,
            ),
        )
    }

    suspend fun myParents(): NetworkResult<List<ParentLinkOut>> =
        safeApiCall { parentApi.myParents() }

    suspend fun issueParentLinkToken(): NetworkResult<LinkTokenResponse> =
        safeApiCall { parentApi.issueLinkToken() }

    suspend fun revokeParentLink(linkId: String): NetworkResult<MessageResponse> =
        safeApiCall { parentApi.revokeLink(linkId) }
}
