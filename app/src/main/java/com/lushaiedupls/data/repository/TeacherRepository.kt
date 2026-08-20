package com.lushaiedupls.data.repository

import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.api.AttendanceApi
import com.lushaiedupls.data.remote.api.CalendarApi
import com.lushaiedupls.data.remote.api.NotificationsApi
import com.lushaiedupls.data.remote.api.OverviewApi
import com.lushaiedupls.data.remote.api.TeachingUnitsApi
import com.lushaiedupls.data.remote.api.TimetableApi
import com.lushaiedupls.data.remote.dto.AddMemberRequest
import com.lushaiedupls.data.remote.dto.AttendanceStatus
import com.lushaiedupls.data.remote.dto.CalendarEventOut
import com.lushaiedupls.data.remote.dto.DayView
import com.lushaiedupls.data.remote.dto.EntryInput
import com.lushaiedupls.data.remote.dto.MemberOut
import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.NotificationAudience
import com.lushaiedupls.data.remote.dto.NotificationCreate
import com.lushaiedupls.data.remote.dto.NotificationOut
import com.lushaiedupls.data.remote.dto.PeriodOut
import com.lushaiedupls.data.remote.dto.RollOut
import com.lushaiedupls.data.remote.dto.RosterResponse
import com.lushaiedupls.data.remote.dto.TeacherOverview
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.UnitAttendanceSummary
import com.lushaiedupls.data.remote.dto.UnreadCountResponse
import com.lushaiedupls.data.remote.dto.UpsertRollRequest
import com.lushaiedupls.data.remote.dto.UserSummary
import com.lushaiedupls.data.remote.dto.WeekView
import com.lushaiedupls.data.remote.safeApiCall

class TeacherRepository(
    private val overviewApi: OverviewApi,
    private val teachingUnitsApi: TeachingUnitsApi,
    private val attendanceApi: AttendanceApi,
    private val calendarApi: CalendarApi,
    private val timetableApi: TimetableApi,
    private val notificationsApi: NotificationsApi,
) {
    suspend fun overview(
        month: String? = null,
        classId: String? = null,
        topLimit: Int = 5,
    ): NetworkResult<TeacherOverview> = safeApiCall {
        overviewApi.teacherOverview(month, classId, topLimit)
    }

    suspend fun teachingUnits(): NetworkResult<List<TeachingUnitOut>> =
        safeApiCall { teachingUnitsApi.list() }

    suspend fun teachingUnit(unitId: String): NetworkResult<TeachingUnitOut> =
        safeApiCall { teachingUnitsApi.get(unitId) }

    suspend fun members(unitId: String): NetworkResult<List<MemberOut>> =
        safeApiCall { teachingUnitsApi.members(unitId) }

    suspend fun addMember(unitId: String, studentId: String): NetworkResult<MemberOut> =
        safeApiCall { teachingUnitsApi.addMember(unitId, AddMemberRequest(studentId)) }

    suspend fun parents(unitId: String): NetworkResult<List<UserSummary>> =
        safeApiCall { teachingUnitsApi.parents(unitId) }

    suspend fun unitSummary(
        unitId: String,
        month: String? = null,
    ): NetworkResult<UnitAttendanceSummary> = safeApiCall {
        attendanceApi.unitSummary(unitId, month)
    }

    suspend fun unitDay(unitId: String, date: String): NetworkResult<DayView> =
        safeApiCall { attendanceApi.unitDay(unitId, date) }

    suspend fun unitRoster(
        unitId: String,
        date: String,
        periodId: String? = null,
        isExtraClass: Boolean = false,
        extraLabel: String? = null,
    ): NetworkResult<RosterResponse> = safeApiCall {
        attendanceApi.unitRoster(
            unitId = unitId,
            date = date,
            periodId = periodId,
            isExtraClass = isExtraClass,
            extraLabel = extraLabel,
        )
    }

    suspend fun saveRoll(
        unitId: String,
        date: String,
        periodId: String?,
        isExtraClass: Boolean,
        entries: List<Pair<String, AttendanceStatus>>,
        extraLabel: String? = null,
    ): NetworkResult<RollOut> = safeApiCall {
        attendanceApi.upsertRoll(
            UpsertRollRequest(
                teaching_unit_id = unitId,
                attendance_date = date,
                period_id = if (isExtraClass) null else periodId,
                is_extra_class = isExtraClass,
                extra_label = if (isExtraClass) extraLabel else null,
                entries = entries.map { (studentId, status) ->
                    EntryInput(student_id = studentId, status = status)
                },
            ),
        )
    }

    suspend fun calendarEvents(
        from: String? = null,
        to: String? = null,
    ): NetworkResult<List<CalendarEventOut>> = safeApiCall {
        calendarApi.events(from, to)
    }

    suspend fun timetable(teachingUnitId: String? = null): NetworkResult<WeekView> =
        safeApiCall { timetableApi.myTimetable(teachingUnitId) }

    suspend fun periods(): NetworkResult<List<PeriodOut>> =
        safeApiCall { timetableApi.periods() }

    suspend fun notifications(limit: Int = 50, offset: Int = 0): NetworkResult<List<NotificationOut>> =
        safeApiCall { notificationsApi.list(limit, offset) }

    suspend fun unreadCount(): NetworkResult<UnreadCountResponse> =
        safeApiCall { notificationsApi.unreadCount() }

    suspend fun markNotificationRead(id: String): NetworkResult<MessageResponse> =
        safeApiCall { notificationsApi.markRead(id) }

    suspend fun createNotification(
        title: String,
        body: String,
        audience: NotificationAudience = NotificationAudience.STUDENTS,
        teachingUnitId: String? = null,
    ): NetworkResult<NotificationOut> = safeApiCall {
        notificationsApi.create(
            NotificationCreate(
                title = title,
                body = body,
                audience = audience,
                teaching_unit_id = teachingUnitId,
            ),
        )
    }
}
