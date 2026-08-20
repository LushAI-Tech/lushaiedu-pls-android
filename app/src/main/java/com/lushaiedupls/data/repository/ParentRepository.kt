package com.lushaiedupls.data.repository

import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.api.AttendanceApi
import com.lushaiedupls.data.remote.api.OverviewApi
import com.lushaiedupls.data.remote.api.ParentApi
import com.lushaiedupls.data.remote.dto.AttendanceCalendar
import com.lushaiedupls.data.remote.dto.LinkedStudentOut
import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.ParentLinkOut
import com.lushaiedupls.data.remote.dto.ParentOverview
import com.lushaiedupls.data.remote.dto.ParentRelationship
import com.lushaiedupls.data.remote.dto.RedeemLinkRequest
import com.lushaiedupls.data.remote.dto.StudentAttendanceSummary
import com.lushaiedupls.data.remote.safeApiCall

class ParentRepository(
    private val parentApi: ParentApi,
    private val overviewApi: OverviewApi,
    private val attendanceApi: AttendanceApi,
) {
    suspend fun overview(month: String? = null): NetworkResult<ParentOverview> =
        safeApiCall { overviewApi.parentOverview(month) }

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
}
