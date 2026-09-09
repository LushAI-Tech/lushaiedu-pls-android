package com.lushaiedupls.data.repository

import com.lushaiedupls.data.remote.FeeMonth
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.RequestCoalescer
import com.lushaiedupls.data.remote.TtlCache
import com.lushaiedupls.data.remote.cachedCall
import com.lushaiedupls.data.remote.api.AttendanceApi
import com.lushaiedupls.data.remote.api.FeesApi
import com.lushaiedupls.data.remote.api.OverviewApi
import com.lushaiedupls.data.remote.api.ParentApi
import com.lushaiedupls.data.remote.api.TimetableApi
import com.lushaiedupls.data.remote.dto.AttendanceCalendar
import com.lushaiedupls.data.remote.dto.FeeHistoryResponse
import com.lushaiedupls.data.remote.dto.LinkedStudentOut
import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.ParentFeedbackCreateRequest
import com.lushaiedupls.data.remote.dto.ParentFeedbackOut
import com.lushaiedupls.data.remote.dto.ParentFeedbackUpdateRequest
import com.lushaiedupls.data.remote.dto.ParentLinkOut
import com.lushaiedupls.data.remote.dto.ParentOverview
import com.lushaiedupls.data.remote.dto.ParentRelationship
import com.lushaiedupls.data.remote.dto.RedeemLinkRequest
import com.lushaiedupls.data.remote.dto.StudentAttendanceSummary
import com.lushaiedupls.data.remote.dto.WeekView
import com.lushaiedupls.data.remote.safeApiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ParentRepository(
    private val parentApi: ParentApi,
    private val overviewApi: OverviewApi,
    private val attendanceApi: AttendanceApi,
    private val timetableApi: TimetableApi,
    private val feesApi: FeesApi,
) {
    private val _unreadNotificationCount = MutableStateFlow<Int?>(null)
    val unreadNotificationCount: StateFlow<Int?> = _unreadNotificationCount.asStateFlow()
    private val coalescer = RequestCoalescer()
    private val catalogCache = TtlCache(OVERVIEW_TTL_MS)

    fun setUnreadNotificationCount(count: Int) {
        _unreadNotificationCount.value = count.coerceAtLeast(0)
    }

    fun decrementUnreadNotificationCount() {
        val current = _unreadNotificationCount.value ?: 1
        setUnreadNotificationCount(current - 1)
    }

    fun clearCaches() {
        catalogCache.clear()
    }

    suspend fun overview(
        month: String? = null,
        forceRefresh: Boolean = false,
    ): NetworkResult<ParentOverview> {
        val result = cachedCall(
            catalogCache,
            coalescer,
            "overview_${month.orEmpty()}",
            forceRefresh,
            OVERVIEW_TTL_MS,
        ) {
            safeApiCall { overviewApi.parentOverview(month) }
        }
        if (result is NetworkResult.Success) {
            _unreadNotificationCount.value = result.data.unread_notifications
        }
        return result
    }

    suspend fun linkedStudents(forceRefresh: Boolean = false): NetworkResult<List<LinkedStudentOut>> =
        cachedCall(catalogCache, coalescer, "linked", forceRefresh, OVERVIEW_TTL_MS) {
            safeApiCall { parentApi.linkedStudents() }
        }

    suspend fun redeemLink(
        token: String,
        relationship: ParentRelationship = ParentRelationship.GUARDIAN,
    ): NetworkResult<ParentLinkOut> {
        val result = safeApiCall {
            parentApi.redeemLink(
                RedeemLinkRequest(
                    token = token.trim(),
                    relationship = relationship,
                ),
            )
        }
        if (result is NetworkResult.Success) {
            catalogCache.remove("linked")
            catalogCache.removePrefix("overview_")
        }
        return result
    }

    suspend fun revokeLink(linkId: String): NetworkResult<MessageResponse> {
        val result = safeApiCall { parentApi.revokeLink(linkId) }
        if (result is NetworkResult.Success) {
            catalogCache.remove("linked")
            catalogCache.removePrefix("overview_")
        }
        return result
    }

    suspend fun studentSummary(
        studentId: String,
        month: String? = null,
    ): NetworkResult<StudentAttendanceSummary> =
        safeApiCall { attendanceApi.studentSummary(studentId, month) }

    suspend fun studentCalendar(
        studentId: String,
        month: String? = null,
    ): NetworkResult<AttendanceCalendar> =
        safeApiCall { attendanceApi.studentCalendar(studentId, month) }

    suspend fun linkedTimetable(
        teachingUnitId: String? = null,
        studentId: String? = null,
    ): NetworkResult<WeekView> =
        safeApiCall {
            timetableApi.myTimetable(
                teachingUnitId = teachingUnitId,
                studentId = studentId?.takeIf { it.isNotBlank() },
            )
        }

    suspend fun studentFeeHistory(
        studentId: String,
        month: String? = FeeMonth.ALL,
    ): NetworkResult<FeeHistoryResponse> {
        if (studentId.isBlank()) {
            return NetworkResult.Error(400, "Parent must select a child.")
        }
        if (!FeeMonth.isListFilter(month)) return FeeMonth.invalidMonthError()
        return safeApiCall { feesApi.studentHistory(studentId, FeeMonth.listFilterOrNull(month)) }
    }

    suspend fun feedback(): NetworkResult<List<ParentFeedbackOut>> =
        safeApiCall { parentApi.feedback() }

    suspend fun createFeedback(
        subject: String,
        message: String,
        studentId: String? = null,
    ): NetworkResult<ParentFeedbackOut> = safeApiCall {
        parentApi.createFeedback(
            ParentFeedbackCreateRequest(
                student_id = studentId?.takeIf { it.isNotBlank() },
                subject = subject.trim(),
                message = message.trim(),
            ),
        )
    }

    suspend fun updateFeedback(
        feedbackId: String,
        subject: String? = null,
        message: String? = null,
        studentId: String? = null,
    ): NetworkResult<ParentFeedbackOut> = safeApiCall {
        parentApi.updateFeedback(
            feedbackId,
            ParentFeedbackUpdateRequest(
                student_id = studentId?.takeIf { it.isNotBlank() },
                subject = subject?.trim()?.takeIf { it.isNotEmpty() },
                message = message?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
    }

    suspend fun deleteFeedback(feedbackId: String): NetworkResult<MessageResponse> =
        safeApiCall { parentApi.deleteFeedback(feedbackId) }

    companion object {
        private const val OVERVIEW_TTL_MS = 30_000L
    }
}
