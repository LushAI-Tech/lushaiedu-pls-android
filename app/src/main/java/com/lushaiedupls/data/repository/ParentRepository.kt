package com.lushaiedupls.data.repository

import com.lushaiedupls.data.remote.NetworkResult
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

    fun setUnreadNotificationCount(count: Int) {
        _unreadNotificationCount.value = count.coerceAtLeast(0)
    }

    fun decrementUnreadNotificationCount() {
        val current = _unreadNotificationCount.value ?: 1
        setUnreadNotificationCount(current - 1)
    }

    suspend fun overview(month: String? = null): NetworkResult<ParentOverview> {
        val res = safeApiCall { overviewApi.parentOverview(month) }
        if (res is NetworkResult.Success) {
            _unreadNotificationCount.value = res.data.unread_notifications
        }
        return res
    }

    suspend fun linkedStudents(): NetworkResult<List<LinkedStudentOut>> =
        safeApiCall { parentApi.linkedStudents() }

    suspend fun redeemLink(
        token: String,
        relationship: ParentRelationship = ParentRelationship.GUARDIAN,
    ): NetworkResult<ParentLinkOut> = safeApiCall {
        parentApi.redeemLink(
            RedeemLinkRequest(
                token = token.trim(),
                relationship = relationship,
            ),
        )
    }

    suspend fun revokeLink(linkId: String): NetworkResult<MessageResponse> =
        safeApiCall { parentApi.revokeLink(linkId) }

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

    suspend fun linkedTimetable(teachingUnitId: String? = null): NetworkResult<WeekView> =
        safeApiCall { timetableApi.myTimetable(teachingUnitId) }

    suspend fun studentFeeHistory(
        studentId: String,
        month: String? = "all",
    ): NetworkResult<FeeHistoryResponse> =
        safeApiCall { feesApi.studentHistory(studentId, month) }

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
}
