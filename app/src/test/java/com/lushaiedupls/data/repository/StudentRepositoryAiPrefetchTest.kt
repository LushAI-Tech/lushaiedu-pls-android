package com.lushaiedupls.data.repository

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
import com.lushaiedupls.data.remote.dto.AddMemberRequest
import com.lushaiedupls.data.remote.dto.AiSubjectOut
import com.lushaiedupls.data.remote.dto.ApproveRollNumbersRequest
import com.lushaiedupls.data.remote.dto.ChapterAttachmentOut
import com.lushaiedupls.data.remote.dto.ChapterListItem
import com.lushaiedupls.data.remote.dto.ChapterOut
import com.lushaiedupls.data.remote.dto.ChatHistoryResponse
import com.lushaiedupls.data.remote.dto.ChatRequest
import com.lushaiedupls.data.remote.dto.ChatResponse
import com.lushaiedupls.data.remote.dto.ClearChatHistoryResponse
import com.lushaiedupls.data.remote.dto.ExamPrepPyqsResponse
import com.lushaiedupls.data.remote.dto.MemberOut
import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.ProgressDashboardResponse
import com.lushaiedupls.data.remote.dto.ProgressUpdateRequest
import com.lushaiedupls.data.remote.dto.QuizAttemptSummary
import com.lushaiedupls.data.remote.dto.QuizStartResponse
import com.lushaiedupls.data.remote.dto.QuizSubmitRequest
import com.lushaiedupls.data.remote.dto.QuizSubmitResponse
import com.lushaiedupls.data.remote.dto.ResumeResponse
import com.lushaiedupls.data.remote.dto.SectionOut
import com.lushaiedupls.data.remote.dto.SetRollNumbersRequest
import com.lushaiedupls.data.remote.dto.SubjectPracticeQuestionsResponse
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.TeachingUnitUpdate
import com.lushaiedupls.data.remote.dto.UserSummary
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentRepositoryAiPrefetchTest {

    private inline fun <reified T> createDummyProxy(): T {
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, _, _ -> null } as T
    }

    private fun createDummyDeviceIdProvider(): DeviceIdProvider {
        val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(DeviceIdProvider::class.java) as DeviceIdProvider
    }

    private class FakeAiApi : AiApi {
        val subjectsCallCount = AtomicInteger(0)
        val chaptersCallCount = AtomicInteger(0)
        val questionsListCallCount = AtomicInteger(0)
        var lastQuestionsIds: String? = null
        var lastAttachmentsIds: String? = null
        var lastPyqsSubtopicIds: String? = null
        var lastPyqsChapterScope: Boolean? = null
        val chapterCallCount = AtomicInteger(0)
        val attachmentsCallCount = AtomicInteger(0)
        val pyqsCallCount = AtomicInteger(0)
        val sectionCallCount = AtomicInteger(0)
        val historyCallCount = AtomicInteger(0)
        val introCallCount = AtomicInteger(0)
        val dashboardCallCount = AtomicInteger(0)
        val resumeCallCount = AtomicInteger(0)
        val quizHistoryCallCount = AtomicInteger(0)

        override suspend fun subjects(): List<AiSubjectOut> {
            subjectsCallCount.incrementAndGet()
            return listOf(
                AiSubjectOut(
                    subject_id = "sub_chem",
                    stem_subject_id = "stem_chem",
                    name = "Chemistry",
                    code = "CHEM",
                    class_id = "cls_10",
                    class_name = "Class 10-A",
                ),
            )
        }

        override suspend fun chapters(subjectId: String): List<ChapterListItem> {
            chaptersCallCount.incrementAndGet()
            return listOf(
                ChapterListItem(
                    id = "ch_atomic",
                    textbook_id = "tb_chem",
                    chapter_number = 1,
                    title = "Atomic Structure",
                    is_active = true,
                ),
            )
        }

        override suspend fun questionsList(
            subjectId: String,
            chapterId: String?,
            subtopicIds: String?,
        ): SubjectPracticeQuestionsResponse {
            questionsListCallCount.incrementAndGet()
            lastQuestionsIds = subtopicIds
            return SubjectPracticeQuestionsResponse(sets = emptyList())
        }

        override suspend fun chapter(chapterId: String): ChapterOut {
            chapterCallCount.incrementAndGet()
            return ChapterOut(
                id = chapterId,
                textbook_id = "tb_chem",
                chapter_number = 1,
                title = "Atomic Structure",
                sections = listOf(
                    SectionOut(
                        id = "sec_1",
                        chapter_id = chapterId,
                        section_number = "1.1",
                        title = "Subatomic Particles",
                        sort_order = 1,
                        depth = 1,
                        content_blocks = emptyList(),
                    ),
                ),
            )
        }

        override suspend fun chapterAttachments(
            chapterId: String,
            subtopicIds: String?,
        ): List<ChapterAttachmentOut> {
            attachmentsCallCount.incrementAndGet()
            lastAttachmentsIds = subtopicIds
            return emptyList()
        }

        override suspend fun examPrepPyqs(
            chapterId: String,
            sectionId: String?,
            chapterScope: Boolean?,
            examCodes: String?,
            subtopicIds: String?,
        ): ExamPrepPyqsResponse {
            pyqsCallCount.incrementAndGet()
            lastPyqsSubtopicIds = subtopicIds
            lastPyqsChapterScope = chapterScope
            return ExamPrepPyqsResponse(hits = emptyList(), filter_codes_available = emptyList())
        }

        override suspend fun section(sectionId: String): SectionOut {
            sectionCallCount.incrementAndGet()
            return SectionOut(
                id = sectionId,
                chapter_id = "ch_atomic",
                section_number = "1.1",
                title = "Subatomic Particles",
                sort_order = 1,
                depth = 1,
                content_blocks = emptyList(),
            )
        }

        override suspend fun chat(chapterId: String, body: ChatRequest): ChatResponse =
            ChatResponse(message = "Hello")

        override suspend fun intro(chapterId: String, responseLanguage: String): ChatResponse {
            introCallCount.incrementAndGet()
            return ChatResponse(message = "Welcome to Atomic Structure!")
        }

        override suspend fun history(chapterId: String): ChatHistoryResponse {
            historyCallCount.incrementAndGet()
            return ChatHistoryResponse(chapter_id = chapterId, messages = emptyList())
        }

        override suspend fun clearHistory(chapterId: String): ClearChatHistoryResponse =
            ClearChatHistoryResponse(cleared_count = 1, chapter_id = chapterId)

        override suspend fun quizSection(sectionId: String): QuizStartResponse =
            QuizStartResponse(
                attempt_id = "att_1",
                section_id = sectionId,
                chapter_id = null,
                quiz_mode = "PRACTICE",
                attempt_number = 1,
                pool_group = 1,
                time_limit_seconds = null,
                total_questions = 0,
                questions = emptyList(),
            )

        override suspend fun quizChapter(chapterId: String): QuizStartResponse =
            QuizStartResponse(
                attempt_id = "att_2",
                section_id = null,
                chapter_id = chapterId,
                quiz_mode = "PRACTICE",
                attempt_number = 1,
                pool_group = 1,
                time_limit_seconds = null,
                total_questions = 0,
                questions = emptyList(),
            )

        override suspend fun quizSubmit(body: QuizSubmitRequest): QuizSubmitResponse =
            QuizSubmitResponse(
                attempt_id = body.attempt_id,
                quiz_mode = "PRACTICE",
                score = 10.0,
                max_score = 10.0,
                total_questions = 10,
                correct = 10,
                incorrect = 0,
                unanswered = 0,
                negative_total = 0.0,
                percentage = 100.0,
                time_taken_seconds = 60,
                results = emptyList(),
                suggestions = null,
            )

        override suspend fun quizHistory(): List<QuizAttemptSummary> {
            quizHistoryCallCount.incrementAndGet()
            return emptyList()
        }

        override suspend fun progressDashboard(): ProgressDashboardResponse {
            dashboardCallCount.incrementAndGet()
            return ProgressDashboardResponse(
                overall_progress_pct = 75.0,
                overall_mastery_pct = 80.0,
                quizzes_completed = 4,
                quick_check_attempts = 12,
            )
        }

        override suspend fun progressResume(): ResumeResponse {
            resumeCallCount.incrementAndGet()
            return ResumeResponse(
                chapter_id = "ch_atomic",
                chapter_title = "Atomic Structure",
                chapter_number = 1,
                section_id = "sec_1",
                section_title = "Subatomic Particles",
                section_number = "1.1",
                content_block_id = "cb_1",
                content_block_title = "Protons and Neutrons",
                content_block_type = "TEXT",
                progress_pct = 50.0,
                last_accessed_at = null,
            )
        }

        override suspend fun progressUpdate(body: ProgressUpdateRequest): JsonElement =
            JsonObject(emptyMap())
    }

    private class FakeTeachingUnitsApi : TeachingUnitsApi {
        val listCallCount = AtomicInteger(0)

        override suspend fun list(): List<TeachingUnitOut> {
            listCallCount.incrementAndGet()
            return listOf(
                TeachingUnitOut(
                    id = "unit_chem",
                    class_id = "cls_10",
                    subject_id = "sub_chem",
                    subject_name = "Chemistry",
                    class_name = "Class 10-A",
                ),
            )
        }

        override suspend fun get(unitId: String): TeachingUnitOut =
            throw UnsupportedOperationException()
        override suspend fun update(unitId: String, body: TeachingUnitUpdate): TeachingUnitOut =
            throw UnsupportedOperationException()
        override suspend fun members(unitId: String): List<MemberOut> =
            emptyList()
        override suspend fun addMember(unitId: String, body: AddMemberRequest): MemberOut =
            throw UnsupportedOperationException()
        override suspend fun removeMember(unitId: String, studentId: String): MessageResponse =
            throw UnsupportedOperationException()
        override suspend fun setRollNumbers(unitId: String, body: SetRollNumbersRequest): List<MemberOut> =
            emptyList()
        override suspend fun approveRollNumbers(unitId: String, body: ApproveRollNumbersRequest): List<MemberOut> =
            emptyList()
        override suspend fun parents(unitId: String): List<UserSummary> =
            emptyList()
    }

    @Test
    fun prefetchAiLearnCachesHubMetadataOnly() = runBlocking {
        val fakeAi = FakeAiApi()
        val fakeUnits = FakeTeachingUnitsApi()
        val repo = StudentRepository(
            overviewApi = createDummyProxy<OverviewApi>(),
            attendanceApi = createDummyProxy<AttendanceApi>(),
            calendarApi = createDummyProxy<CalendarApi>(),
            timetableApi = createDummyProxy<TimetableApi>(),
            notificationsApi = createDummyProxy<NotificationsApi>(),
            meApi = createDummyProxy<MeApi>(),
            classesApi = createDummyProxy<ClassesApi>(),
            aiApi = fakeAi,
            parentApi = createDummyProxy<ParentApi>(),
            teachingUnitsApi = fakeUnits,
            deviceIdProvider = createDummyDeviceIdProvider(),
            feesApi = createDummyProxy<FeesApi>(),
        )

        repo.prefetchAiLearn()

        assertTrue("Subjects should be fetched", fakeAi.subjectsCallCount.get() >= 1)
        assertTrue("Teaching units should be fetched", fakeUnits.listCallCount.get() >= 1)
        assertTrue("Dashboard progress should be fetched", fakeAi.dashboardCallCount.get() >= 1)
        assertTrue("Resume state should be fetched", fakeAi.resumeCallCount.get() >= 1)
        assertTrue("Quiz history should be fetched", fakeAi.quizHistoryCallCount.get() >= 1)
        assertEquals("Chapter lists should not be prefetched", 0, fakeAi.chaptersCallCount.get())
        assertEquals("Questions list should not be prefetched", 0, fakeAi.questionsListCallCount.get())
        assertEquals("Chapter details should not be prefetched", 0, fakeAi.chapterCallCount.get())
        assertEquals("Chapter attachments should not be prefetched", 0, fakeAi.attachmentsCallCount.get())
        assertEquals("PYQs should not be prefetched", 0, fakeAi.pyqsCallCount.get())
        assertEquals("Chat history should not be prefetched", 0, fakeAi.historyCallCount.get())
        assertEquals("Chat intro should not be prefetched", 0, fakeAi.introCallCount.get())
        assertEquals("Section content should not be prefetched", 0, fakeAi.sectionCallCount.get())

        val subjectsBefore = fakeAi.subjectsCallCount.get()
        val dashboardBefore = fakeAi.dashboardCallCount.get()

        val cachedSubjects = repo.aiSubjects()
        assertTrue(cachedSubjects is NetworkResult.Success)
        assertEquals(subjectsBefore, fakeAi.subjectsCallCount.get())

        val cachedDashboard = repo.progressDashboard()
        assertTrue(cachedDashboard is NetworkResult.Success)
        assertEquals(dashboardBefore, fakeAi.dashboardCallCount.get())

        val cachedChapters = repo.chapters("sub_chem")
        assertTrue(cachedChapters is NetworkResult.Success)
        assertEquals(1, fakeAi.chaptersCallCount.get())

        assertEquals("Chemistry", repo.getCachedAiSubjects()?.firstOrNull()?.name)
        assertEquals("Atomic Structure", repo.getCachedChapters("sub_chem")?.firstOrNull()?.title)
        assertEquals(75.0, repo.getCachedProgressDashboard()?.overall_progress_pct ?: 0.0, 0.01)

        repo.clearAiCache()
        repo.aiSubjects()
        assertEquals(subjectsBefore + 1, fakeAi.subjectsCallCount.get())
    }

    @Test
    fun prefetchAiChatWarmsOnlyOneChapterConversation() = runBlocking {
        val fakeAi = FakeAiApi()
        val repo = StudentRepository(
            overviewApi = createDummyProxy<OverviewApi>(),
            attendanceApi = createDummyProxy<AttendanceApi>(),
            calendarApi = createDummyProxy<CalendarApi>(),
            timetableApi = createDummyProxy<TimetableApi>(),
            notificationsApi = createDummyProxy<NotificationsApi>(),
            meApi = createDummyProxy<MeApi>(),
            classesApi = createDummyProxy<ClassesApi>(),
            aiApi = fakeAi,
            parentApi = createDummyProxy<ParentApi>(),
            teachingUnitsApi = createDummyProxy<TeachingUnitsApi>(),
            deviceIdProvider = createDummyDeviceIdProvider(),
            feesApi = createDummyProxy<FeesApi>(),
        )

        repo.prefetchAiChat(listOf("ch_atomic", "ch_ignored"))

        assertEquals(1, fakeAi.chapterCallCount.get())
        assertEquals(1, fakeAi.historyCallCount.get())
        assertEquals(1, fakeAi.introCallCount.get())
        assertEquals(0, fakeAi.attachmentsCallCount.get())
        assertEquals(0, fakeAi.pyqsCallCount.get())
    }

    @Test
    fun singleFlightDeduplicatesConcurrentInFlightRequests() = runBlocking {
        val fakeAi = object : AiApi by FakeAiApi() {
            val count = AtomicInteger(0)
            override suspend fun subjects(): List<AiSubjectOut> {
                count.incrementAndGet()
                kotlinx.coroutines.delay(50)
                return listOf(
                    AiSubjectOut(
                        subject_id = "sub_chem",
                        stem_subject_id = "stem_chem",
                        name = "Chemistry",
                        code = "CHEM",
                        class_id = "cls_10",
                        class_name = "Class 10-A",
                    ),
                )
            }
        }
        val repo = StudentRepository(
            overviewApi = createDummyProxy<OverviewApi>(),
            attendanceApi = createDummyProxy<AttendanceApi>(),
            calendarApi = createDummyProxy<CalendarApi>(),
            timetableApi = createDummyProxy<TimetableApi>(),
            notificationsApi = createDummyProxy<NotificationsApi>(),
            meApi = createDummyProxy<MeApi>(),
            classesApi = createDummyProxy<ClassesApi>(),
            aiApi = fakeAi,
            parentApi = createDummyProxy<ParentApi>(),
            teachingUnitsApi = createDummyProxy<TeachingUnitsApi>(),
            deviceIdProvider = createDummyDeviceIdProvider(),
            feesApi = createDummyProxy<FeesApi>(),
        )

        // Fire 10 concurrent requests for subjects simultaneously
        val results = coroutineScope {
            (1..10).map {
                async(kotlinx.coroutines.Dispatchers.IO) {
                    repo.aiSubjects(forceRefresh = true)
                }
            }.map { it.await() }
        }

        // All 10 callers should receive successful result
        results.forEach { result ->
            assertTrue(result is NetworkResult.Success)
            assertEquals("Chemistry", (result as NetworkResult.Success).data.first().name)
        }

        // But only ONE network request was actually fired because singleFlight joined them!
        assertEquals("Single-flight should coalesce concurrent requests into 1 network call", 1, fakeAi.count.get())
    }

    @Test
    fun examPrepPyqs_dropsSubtopicIdsWhenChapterScopeIsTrue() = runBlocking {
        val fakeAi = FakeAiApi()
        val repo = StudentRepository(
            overviewApi = createDummyProxy<OverviewApi>(),
            attendanceApi = createDummyProxy<AttendanceApi>(),
            calendarApi = createDummyProxy<CalendarApi>(),
            timetableApi = createDummyProxy<TimetableApi>(),
            notificationsApi = createDummyProxy<NotificationsApi>(),
            meApi = createDummyProxy<MeApi>(),
            classesApi = createDummyProxy<ClassesApi>(),
            aiApi = fakeAi,
            parentApi = createDummyProxy<ParentApi>(),
            teachingUnitsApi = createDummyProxy<TeachingUnitsApi>(),
            deviceIdProvider = createDummyDeviceIdProvider(),
            feesApi = createDummyProxy<FeesApi>(),
        )

        repo.examPrepPyqs(
            chapterId = "ch_atomic",
            chapterScope = true,
            subtopicIds = "sec_1,sec_2",
        )
        assertEquals(true, fakeAi.lastPyqsChapterScope)
        assertNull(fakeAi.lastPyqsSubtopicIds)

        repo.examPrepPyqs(
            chapterId = "ch_atomic",
            chapterScope = false,
            subtopicIds = "sec_1,sec_2",
        )
        assertEquals(false, fakeAi.lastPyqsChapterScope)
        assertEquals("sec_1,sec_2", fakeAi.lastPyqsSubtopicIds)
    }

    @Test
    fun questionsAndAttachments_passCommaSeparatedSubtopicIds() = runBlocking {
        val fakeAi = FakeAiApi()
        val repo = StudentRepository(
            overviewApi = createDummyProxy<OverviewApi>(),
            attendanceApi = createDummyProxy<AttendanceApi>(),
            calendarApi = createDummyProxy<CalendarApi>(),
            timetableApi = createDummyProxy<TimetableApi>(),
            notificationsApi = createDummyProxy<NotificationsApi>(),
            meApi = createDummyProxy<MeApi>(),
            classesApi = createDummyProxy<ClassesApi>(),
            aiApi = fakeAi,
            parentApi = createDummyProxy<ParentApi>(),
            teachingUnitsApi = createDummyProxy<TeachingUnitsApi>(),
            deviceIdProvider = createDummyDeviceIdProvider(),
            feesApi = createDummyProxy<FeesApi>(),
        )

        repo.questionsList(subjectId = "sub_chem", chapterId = "ch_atomic", subtopicIds = "sec_1,sec_2")
        assertEquals("sec_1,sec_2", fakeAi.lastQuestionsIds)

        repo.chapterAttachments(chapterId = "ch_atomic", subtopicIds = "sec_1")
        assertEquals("sec_1", fakeAi.lastAttachmentsIds)

        repo.questionsList(subjectId = "sub_chem", chapterId = "ch_atomic", subtopicIds = "  ")
        assertNull(fakeAi.lastQuestionsIds)
    }
}
