package com.lushaiedupls.data.remote.api

import com.lushaiedupls.data.remote.dto.AiSubjectOut
import com.lushaiedupls.data.remote.dto.ChapterAttachmentOut
import com.lushaiedupls.data.remote.dto.ChapterListItem
import com.lushaiedupls.data.remote.dto.ChapterOut
import com.lushaiedupls.data.remote.dto.ChatHistoryResponse
import com.lushaiedupls.data.remote.dto.ChatRequest
import com.lushaiedupls.data.remote.dto.ChatResponse
import com.lushaiedupls.data.remote.dto.ClearChatHistoryResponse
import com.lushaiedupls.data.remote.dto.ExamPrepPyqsResponse
import com.lushaiedupls.data.remote.dto.ProgressDashboardResponse
import com.lushaiedupls.data.remote.dto.ProgressUpdateRequest
import com.lushaiedupls.data.remote.dto.QuizAttemptSummary
import com.lushaiedupls.data.remote.dto.QuizStartResponse
import com.lushaiedupls.data.remote.dto.QuizSubmitRequest
import com.lushaiedupls.data.remote.dto.QuizSubmitResponse
import com.lushaiedupls.data.remote.dto.ResumeResponse
import com.lushaiedupls.data.remote.dto.SectionOut
import com.lushaiedupls.data.remote.dto.SubjectPracticeQuestionsResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AiApi {
    @GET("api/v1/ai/subjects")
    suspend fun subjects(): List<AiSubjectOut>

    @GET("api/v1/ai/subjects/{subject_id}/chapters")
    suspend fun chapters(@Path("subject_id") subjectId: String): List<ChapterListItem>

    @GET("api/v1/ai/subjects/{subject_id}/questions-list")
    suspend fun questionsList(@Path("subject_id") subjectId: String): SubjectPracticeQuestionsResponse

    @GET("api/v1/ai/chapters/{chapter_id}")
    suspend fun chapter(@Path("chapter_id") chapterId: String): ChapterOut

    @GET("api/v1/ai/chapters/{chapter_id}/attach-files")
    suspend fun chapterAttachments(@Path("chapter_id") chapterId: String): List<ChapterAttachmentOut>

    @GET("api/v1/ai/chapters/{chapter_id}/exam-prep-pyqs")
    suspend fun examPrepPyqs(
        @Path("chapter_id") chapterId: String,
        @Query("section_id") sectionId: String? = null,
        @Query("chapter_scope") chapterScope: Boolean? = null,
        @Query("exam_codes") examCodes: String? = null,
    ): ExamPrepPyqsResponse

    @GET("api/v1/ai/sections/{section_id}")
    suspend fun section(@Path("section_id") sectionId: String): SectionOut

    @POST("api/v1/ai/chat/{chapter_id}")
    suspend fun chat(
        @Path("chapter_id") chapterId: String,
        @Body body: ChatRequest,
    ): ChatResponse

    @POST("api/v1/ai/chat/{chapter_id}/intro")
    suspend fun intro(
        @Path("chapter_id") chapterId: String,
        @Query("response_language") responseLanguage: String = "en",
    ): ChatResponse

    @GET("api/v1/ai/chat/{chapter_id}/history")
    suspend fun history(@Path("chapter_id") chapterId: String): ChatHistoryResponse

    @DELETE("api/v1/ai/chat/{chapter_id}/history")
    suspend fun clearHistory(@Path("chapter_id") chapterId: String): ClearChatHistoryResponse

    @GET("api/v1/ai/quiz/section/{section_id}")
    suspend fun quizSection(@Path("section_id") sectionId: String): QuizStartResponse

    @GET("api/v1/ai/quiz/chapter/{chapter_id}/practice")
    suspend fun quizChapter(@Path("chapter_id") chapterId: String): QuizStartResponse

    @POST("api/v1/ai/quiz/submit")
    suspend fun quizSubmit(@Body body: QuizSubmitRequest): QuizSubmitResponse

    @GET("api/v1/ai/quiz/history")
    suspend fun quizHistory(): List<QuizAttemptSummary>

    @GET("api/v1/ai/progress/dashboard")
    suspend fun progressDashboard(): ProgressDashboardResponse

    @GET("api/v1/ai/progress/resume")
    suspend fun progressResume(): ResumeResponse?

    @PUT("api/v1/ai/progress/update")
    suspend fun progressUpdate(@Body body: ProgressUpdateRequest): kotlinx.serialization.json.JsonElement
}
