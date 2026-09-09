package com.lushaiedupls.data.repository

import android.content.Context
import android.net.Uri
import com.lushaiedupls.data.remote.AiQueryParams
import com.lushaiedupls.data.remote.FeeMonth
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.api.AiApi
import com.lushaiedupls.data.remote.api.AttendanceApi
import com.lushaiedupls.data.remote.api.CalendarApi
import com.lushaiedupls.data.remote.api.ClassesApi
import com.lushaiedupls.data.remote.api.FeesApi
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
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.ClearChatHistoryResponse
import com.lushaiedupls.data.remote.dto.DeviceOut
import com.lushaiedupls.data.remote.dto.ExamPrepPyqsResponse
import com.lushaiedupls.data.remote.dto.FeeHistoryResponse
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private val feesApi: FeesApi,
) {
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<Any?>>()
    private val inFlightMutex = Mutex()

    private suspend fun <T> singleFlight(key: String, block: suspend () -> NetworkResult<T>): NetworkResult<T> {
        val pair: Pair<Deferred<Any?>, Boolean> = inFlightMutex.withLock {
            val existing = inFlightRequests[key]
            if (existing != null) {
                Pair(existing, false)
            } else {
                val newDeferred = CompletableDeferred<Any?>()
                inFlightRequests[key] = newDeferred
                Pair(newDeferred, true)
            }
        }
        val deferred = pair.first
        val isLeader = pair.second

        if (!isLeader) {
            @Suppress("UNCHECKED_CAST")
            return deferred.await() as NetworkResult<T>
        }

        val leaderDeferred = deferred as CompletableDeferred<Any?>
        try {
            val result = block()
            leaderDeferred.complete(result)
            return result
        } catch (e: Throwable) {
            leaderDeferred.completeExceptionally(e)
            throw e
        } finally {
            inFlightMutex.withLock {
                inFlightRequests.remove(key)
            }
        }
    }

    private val overviewCache = ConcurrentHashMap<String, StudentOverview>()
    private val attendanceSummaryCache = ConcurrentHashMap<String, StudentAttendanceSummary>()
    private val attendanceCalendarCache = ConcurrentHashMap<String, AttendanceCalendar>()
    private val timetableCache = ConcurrentHashMap<String, WeekView>()
    private val chaptersCache = ConcurrentHashMap<String, List<ChapterListItem>>()
    private val chapterCache = ConcurrentHashMap<String, ChapterOut>()
    private val sectionCache = ConcurrentHashMap<String, SectionOut>()
    private val chatHistoryCache = ConcurrentHashMap<String, ChatHistoryResponse>()
    private val chatIntroCache = ConcurrentHashMap<String, ChatResponse>()
    private val questionsListCache = ConcurrentHashMap<String, SubjectPracticeQuestionsResponse>()
    private val examPrepPyqsCache = ConcurrentHashMap<String, ExamPrepPyqsResponse>()
    private val chapterAttachmentsCache = ConcurrentHashMap<String, List<ChapterAttachmentOut>>()
    private val aiSubjectsCache = AtomicReference<List<AiSubjectOut>?>()
    private val teachingUnitsCache = AtomicReference<List<TeachingUnitOut>?>()
    private val progressDashboardCache = AtomicReference<ProgressDashboardResponse?>()
    private val progressResumeCache = AtomicReference<ResumeResponse?>()
    private val hasCachedResume = AtomicBoolean(false)
    private val quizHistoryCache = AtomicReference<List<QuizAttemptSummary>?>()
    private val isPrefetchingAiLearn = AtomicBoolean(false)
    private val isPrefetchingAiChat = AtomicBoolean(false)
    private val aiPrefetchSemaphore = Semaphore(permits = AI_PREFETCH_CONCURRENCY)

    fun getCachedOverview(month: String? = null): StudentOverview? = overviewCache[month.orEmpty()]
    fun getCachedAiSubjects(): List<AiSubjectOut>? = aiSubjectsCache.get()
    fun getCachedTeachingUnits(): List<TeachingUnitOut>? = teachingUnitsCache.get()
    fun getCachedProgressDashboard(): ProgressDashboardResponse? = progressDashboardCache.get()
    fun getCachedProgressResume(): ResumeResponse? =
        if (hasCachedResume.get()) progressResumeCache.get() else null
    fun getCachedChapters(subjectId: String): List<ChapterListItem>? = chaptersCache[subjectId]
    fun getCachedChapter(chapterId: String): ChapterOut? = chapterCache[chapterId]
    fun getCachedChatHistory(chapterId: String): ChatHistoryResponse? = chatHistoryCache[chapterId]
    fun getCachedChatIntro(chapterId: String, language: String): ChatResponse? = chatIntroCache["${chapterId}_$language"]
    fun getCachedQuestionsList(
        subjectId: String,
        chapterId: String? = null,
        subtopicIds: String? = null,
    ): SubjectPracticeQuestionsResponse? =
        questionsListCache[questionsListCacheKey(subjectId, chapterId, subtopicIds)]

    fun getCachedAttachments(
        chapterId: String,
        subtopicIds: String? = null,
    ): List<ChapterAttachmentOut>? = chapterAttachmentsCache[attachmentsCacheKey(chapterId, subtopicIds)]

    fun getCachedExamPrepPyqs(
        chapterId: String,
        sectionId: String? = null,
        chapterScope: Boolean? = null,
        examCodes: String? = null,
        subtopicIds: String? = null,
    ): ExamPrepPyqsResponse? = examPrepPyqsCache[
        examPrepCacheKey(chapterId, sectionId, chapterScope, examCodes, subtopicIds),
    ]
    fun getCachedQuizHistory(): List<QuizAttemptSummary>? = quizHistoryCache.get()

    /** Picks the resume chapter when available, otherwise the first active chapter. */
    fun preferredChatPrefetchChapterId(chapterIds: List<String>): String? {
        val distinct = chapterIds.distinct().filter { it.isNotBlank() }
        if (distinct.isEmpty()) return null
        val resumeId = getCachedProgressResume()?.chapter_id
        return resumeId?.takeIf { it in distinct } ?: distinct.first()
    }

    private val _unreadNotificationCount = MutableStateFlow<Int?>(null)
    val unreadNotificationCount: StateFlow<Int?> = _unreadNotificationCount.asStateFlow()

    fun setUnreadNotificationCount(count: Int) {
        val nonNegative = count.coerceAtLeast(0)
        _unreadNotificationCount.value = nonNegative
        overviewCache[""]?.let { current ->
            overviewCache[""] = current.copy(unread_notifications = nonNegative)
        }
    }

    fun decrementUnreadNotificationCount() {
        val current = _unreadNotificationCount.value ?: 1
        setUnreadNotificationCount(current - 1)
    }

    fun clearAiCache() {
        overviewCache.clear()
        attendanceSummaryCache.clear()
        attendanceCalendarCache.clear()
        timetableCache.clear()
        chaptersCache.clear()
        chapterCache.clear()
        sectionCache.clear()
        chatHistoryCache.clear()
        chatIntroCache.clear()
        questionsListCache.clear()
        examPrepPyqsCache.clear()
        chapterAttachmentsCache.clear()
        aiSubjectsCache.set(null)
        teachingUnitsCache.set(null)
        progressDashboardCache.set(null)
        progressResumeCache.set(null)
        hasCachedResume.set(false)
        quizHistoryCache.set(null)
    }

    suspend fun overview(month: String? = null, forceRefresh: Boolean = false): NetworkResult<StudentOverview> {
        val key = month.orEmpty()
        if (!forceRefresh) {
            overviewCache[key]?.let { return NetworkResult.Success(it) }
        }
        return singleFlight("overview_$key") {
            val res = safeApiCall { overviewApi.studentOverview(month) }
            if (res is NetworkResult.Success) {
                overviewCache[key] = res.data
                _unreadNotificationCount.value = res.data.unread_notifications
            }
            res
        }
    }

    suspend fun attendanceSummary(month: String? = null, forceRefresh: Boolean = false): NetworkResult<StudentAttendanceSummary> {
        val key = month.orEmpty()
        if (!forceRefresh) {
            attendanceSummaryCache[key]?.let { return NetworkResult.Success(it) }
        }
        return singleFlight("att_summary_$key") {
            val res = safeApiCall { attendanceApi.mySummary(month) }
            if (res is NetworkResult.Success) {
                attendanceSummaryCache[key] = res.data
            }
            res
        }
    }

    suspend fun attendanceCalendar(month: String? = null, forceRefresh: Boolean = false): NetworkResult<AttendanceCalendar> {
        val key = month.orEmpty()
        if (!forceRefresh) {
            attendanceCalendarCache[key]?.let { return NetworkResult.Success(it) }
        }
        return singleFlight("att_cal_$key") {
            val res = safeApiCall { attendanceApi.myCalendar(month) }
            if (res is NetworkResult.Success) {
                attendanceCalendarCache[key] = res.data
            }
            res
        }
    }

    suspend fun calendarEvents(from: String? = null, to: String? = null): NetworkResult<List<CalendarEventOut>> =
        safeApiCall { calendarApi.events(from, to) }

    suspend fun timetable(teachingUnitId: String? = null, forceRefresh: Boolean = false): NetworkResult<WeekView> {
        val key = teachingUnitId.orEmpty()
        if (!forceRefresh) {
            timetableCache[key]?.let { return NetworkResult.Success(it) }
        }
        return singleFlight("timetable_$key") {
            val res = safeApiCall { timetableApi.myTimetable(teachingUnitId = teachingUnitId) }
            if (res is NetworkResult.Success) {
                timetableCache[key] = res.data
            }
            res
        }
    }

    suspend fun notifications(limit: Int = 50, offset: Int = 0): NetworkResult<List<NotificationOut>> {
        val res = safeApiCall { notificationsApi.list(limit, offset) }
        if (res is NetworkResult.Success) {
            setUnreadNotificationCount(res.data.count { !it.is_read })
        }
        return res
    }

    suspend fun unreadCount(): NetworkResult<UnreadCountResponse> =
        safeApiCall { notificationsApi.unreadCount() }

    suspend fun markNotificationRead(id: String): NetworkResult<MessageResponse> {
        decrementUnreadNotificationCount()
        return safeApiCall { notificationsApi.markRead(id) }
    }

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

    suspend fun institutions(): NetworkResult<List<InstitutionOut>> =
        safeApiCall { classesApi.listInstitutions() }

    suspend fun classes(institutionId: String): NetworkResult<List<ClassOut>> =
        safeApiCall { classesApi.listClasses(institutionId) }

    suspend fun questionsList(
        subjectId: String,
        chapterId: String? = null,
        subtopicIds: String? = null,
        forceRefresh: Boolean = false,
    ): NetworkResult<SubjectPracticeQuestionsResponse> {
        val scopedChapterId = AiQueryParams.nonEmpty(chapterId)
        val scopedSubtopicIds = AiQueryParams.nonEmpty(subtopicIds)
        val cacheKey = questionsListCacheKey(subjectId, scopedChapterId, scopedSubtopicIds)
        if (!forceRefresh) {
            questionsListCache[cacheKey]?.let { return NetworkResult.Success(it) }
        }
        return singleFlight("questions_$cacheKey") {
            val result = safeApiCall {
                aiApi.questionsList(
                    subjectId = subjectId,
                    chapterId = scopedChapterId,
                    subtopicIds = scopedSubtopicIds,
                )
            }
            if (result is NetworkResult.Success) {
                questionsListCache[cacheKey] = result.data
            }
            result
        }
    }

    suspend fun chapterAttachments(
        chapterId: String,
        subtopicIds: String? = null,
        forceRefresh: Boolean = false,
    ): NetworkResult<List<ChapterAttachmentOut>> {
        val scopedSubtopicIds = AiQueryParams.nonEmpty(subtopicIds)
        val cacheKey = attachmentsCacheKey(chapterId, scopedSubtopicIds)
        if (!forceRefresh) {
            chapterAttachmentsCache[cacheKey]?.let { return NetworkResult.Success(it) }
        }
        return singleFlight("attachments_$cacheKey") {
            val result = safeApiCall {
                aiApi.chapterAttachments(
                    chapterId = chapterId,
                    subtopicIds = scopedSubtopicIds,
                )
            }
            if (result is NetworkResult.Success) {
                chapterAttachmentsCache[cacheKey] = result.data
            }
            result
        }
    }

    suspend fun examPrepPyqs(
        chapterId: String,
        sectionId: String? = null,
        chapterScope: Boolean? = null,
        examCodes: String? = null,
        subtopicIds: String? = null,
        forceRefresh: Boolean = false,
    ): NetworkResult<ExamPrepPyqsResponse> {
        val scopedSectionId = AiQueryParams.nonEmpty(sectionId)
        val scopedExamCodes = AiQueryParams.nonEmpty(examCodes)
        val scopedSubtopicIds = if (chapterScope == true) {
            null
        } else {
            AiQueryParams.nonEmpty(subtopicIds)
        }
        val cacheKey = examPrepCacheKey(
            chapterId,
            scopedSectionId,
            chapterScope,
            scopedExamCodes,
            scopedSubtopicIds,
        )
        if (!forceRefresh) {
            examPrepPyqsCache[cacheKey]?.let { return NetworkResult.Success(it) }
        }
        return singleFlight("pyqs_$cacheKey") {
            val result = safeApiCall {
                aiApi.examPrepPyqs(
                    chapterId = chapterId,
                    sectionId = scopedSectionId,
                    chapterScope = chapterScope,
                    examCodes = scopedExamCodes,
                    subtopicIds = scopedSubtopicIds,
                )
            }
            if (result is NetworkResult.Success) {
                examPrepPyqsCache[cacheKey] = result.data
            }
            result
        }
    }

    suspend fun subjects(classId: String, institutionId: String): NetworkResult<List<SubjectOut>> =
        safeApiCall { classesApi.listSubjects(classId, institutionId) }

    suspend fun myFeeHistory(month: String? = FeeMonth.ALL): NetworkResult<FeeHistoryResponse> {
        if (!FeeMonth.isListFilter(month)) return FeeMonth.invalidMonthError()
        return safeApiCall { feesApi.myHistory(FeeMonth.listFilterOrNull(month)) }
    }

    suspend fun aiSubjects(forceRefresh: Boolean = false): NetworkResult<List<AiSubjectOut>> {
        if (!forceRefresh) {
            aiSubjectsCache.get()?.let { return NetworkResult.Success(it) }
        }
        return singleFlight("ai_subjects") {
            val result = safeApiCall { aiApi.subjects() }
            if (result is NetworkResult.Success) {
                aiSubjectsCache.set(result.data)
            }
            result
        }
    }

    suspend fun teachingUnits(forceRefresh: Boolean = false): NetworkResult<List<TeachingUnitOut>> {
        if (!forceRefresh) {
            teachingUnitsCache.get()?.let { return NetworkResult.Success(it) }
        }
        return singleFlight("teaching_units") {
            val result = safeApiCall { teachingUnitsApi.list() }
            if (result is NetworkResult.Success) {
                teachingUnitsCache.set(result.data)
            }
            result
        }
    }

    suspend fun progressDashboard(forceRefresh: Boolean = false): NetworkResult<ProgressDashboardResponse> {
        if (!forceRefresh) {
            progressDashboardCache.get()?.let { return NetworkResult.Success(it) }
        }
        return singleFlight("progress_dashboard") {
            val result = safeApiCall { aiApi.progressDashboard() }
            if (result is NetworkResult.Success) {
                progressDashboardCache.set(result.data)
            }
            result
        }
    }

    suspend fun progressResume(forceRefresh: Boolean = false): NetworkResult<ResumeResponse?> {
        if (!forceRefresh && hasCachedResume.get()) {
            return NetworkResult.Success(progressResumeCache.get())
        }
        return singleFlight("progress_resume") {
            val result = safeApiCall { aiApi.progressResume() }
            if (result is NetworkResult.Success) {
                progressResumeCache.set(result.data)
                hasCachedResume.set(true)
            }
            result
        }
    }

    suspend fun chapters(subjectId: String, forceRefresh: Boolean = false): NetworkResult<List<ChapterListItem>> {
        if (!forceRefresh) {
            chaptersCache[subjectId]?.let { return NetworkResult.Success(it) }
        }
        return singleFlight("chapters_$subjectId") {
            val result = safeApiCall { aiApi.chapters(subjectId) }
            if (result is NetworkResult.Success) {
                chaptersCache[subjectId] = result.data
            }
            result
        }
    }

    suspend fun chapter(chapterId: String, forceRefresh: Boolean = false): NetworkResult<ChapterOut> {
        if (!forceRefresh) {
            chapterCache[chapterId]?.let { return NetworkResult.Success(it) }
        }
        return singleFlight("chapter_$chapterId") {
            val result = safeApiCall { aiApi.chapter(chapterId) }
            if (result is NetworkResult.Success) {
                chapterCache[chapterId] = result.data
            }
            result
        }
    }

    suspend fun section(sectionId: String, forceRefresh: Boolean = false): NetworkResult<SectionOut> {
        if (!forceRefresh) {
            sectionCache[sectionId]?.let { return NetworkResult.Success(it) }
        }
        return singleFlight("section_$sectionId") {
            val result = safeApiCall { aiApi.section(sectionId) }
            if (result is NetworkResult.Success) {
                sectionCache[sectionId] = result.data
            }
            result
        }
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
        return singleFlight("chat_intro_$key") {
            val result = safeApiCall { aiApi.intro(chapterId, language) }
            if (result is NetworkResult.Success) {
                chatIntroCache[key] = result.data
            }
            result
        }
    }

    suspend fun chatHistory(chapterId: String, forceRefresh: Boolean = false): NetworkResult<ChatHistoryResponse> {
        if (!forceRefresh) {
            chatHistoryCache[chapterId]?.let { return NetworkResult.Success(it) }
        }
        return singleFlight("chat_history_$chapterId") {
            val result = safeApiCall { aiApi.history(chapterId) }
            if (result is NetworkResult.Success) {
                chatHistoryCache[chapterId] = result.data
            }
            result
        }
    }

    suspend fun prefetchAiChat(chapterIds: List<String>, language: String = "en") {
        if (!isPrefetchingAiChat.compareAndSet(false, true)) {
            return
        }
        try {
            val targetId = chapterIds
                .distinct()
                .firstOrNull { it.isNotBlank() && chatHistoryCache[it] == null }
                ?: return
            aiPrefetchSemaphore.withPermit {
                if (chapterCache[targetId] == null) {
                    chapter(targetId)
                }
                when (val history = chatHistory(targetId)) {
                    is NetworkResult.Success -> {
                        if (history.data.messages.isEmpty() &&
                            chatIntroCache["${targetId}_$language"] == null
                        ) {
                            chatIntro(targetId, language)
                        }
                    }
                    else -> Unit
                }
            }
        } catch (_: Exception) {
            // Best effort prefetch
        } finally {
            isPrefetchingAiChat.set(false)
        }
    }

    /**
     * Prefetches AI Learn hub metadata only (dashboard, subjects, teaching units, resume, quiz).
     * Chapter lists load on demand when a subject is opened, to avoid a DB fan-out.
     */
    suspend fun prefetchAiLearn(language: String = "en") {
        if (!isPrefetchingAiLearn.compareAndSet(false, true)) {
            return
        }
        try {
            supervisorScope {
                aiPrefetchSemaphore.withPermit { aiSubjects() }
                aiPrefetchSemaphore.withPermit { teachingUnits() }
                aiPrefetchSemaphore.withPermit { progressDashboard() }
                aiPrefetchSemaphore.withPermit { progressResume() }
                aiPrefetchSemaphore.withPermit { quizHistory() }
            }
        } catch (_: Exception) {
            // Best effort background prefetch
        } finally {
            isPrefetchingAiLearn.set(false)
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

    suspend fun quizHistory(forceRefresh: Boolean = false): NetworkResult<List<QuizAttemptSummary>> {
        if (!forceRefresh) {
            quizHistoryCache.get()?.let { return NetworkResult.Success(it) }
        }
        val result = safeApiCall { aiApi.quizHistory() }
        if (result is NetworkResult.Success) {
            quizHistoryCache.set(result.data)
        }
        return result
    }

    suspend fun submitQuiz(
        attemptId: String,
        answers: List<AnswerSubmission>,
        timeTakenSeconds: Int? = null,
    ): NetworkResult<QuizSubmitResponse> {
        quizHistoryCache.set(null)
        progressDashboardCache.set(null)
        return safeApiCall {
            aiApi.quizSubmit(
                QuizSubmitRequest(
                    attempt_id = attemptId,
                    answers = answers,
                    time_taken_seconds = timeTakenSeconds,
                ),
            )
        }
    }

    suspend fun myParents(): NetworkResult<List<ParentLinkOut>> =
        safeApiCall { parentApi.myParents() }

    suspend fun issueParentLinkToken(): NetworkResult<LinkTokenResponse> =
        safeApiCall { parentApi.issueLinkToken() }

    suspend fun revokeParentLink(linkId: String): NetworkResult<MessageResponse> =
        safeApiCall { parentApi.revokeLink(linkId) }

    companion object {
        /** Keeps concurrent AI prefetch calls low to respect backend DB pool limits. */
        private const val AI_PREFETCH_CONCURRENCY = 3

        private fun questionsListCacheKey(
            subjectId: String,
            chapterId: String?,
            subtopicIds: String?,
        ): String = "${subjectId}_${chapterId.orEmpty()}_${subtopicIds.orEmpty()}"

        private fun attachmentsCacheKey(chapterId: String, subtopicIds: String?): String =
            "${chapterId}_${subtopicIds.orEmpty()}"

        private fun examPrepCacheKey(
            chapterId: String,
            sectionId: String?,
            chapterScope: Boolean?,
            examCodes: String?,
            subtopicIds: String?,
        ): String = listOf(
            chapterId,
            sectionId.orEmpty(),
            chapterScope?.toString().orEmpty(),
            examCodes.orEmpty(),
            subtopicIds.orEmpty(),
        ).joinToString("_")
    }
}
