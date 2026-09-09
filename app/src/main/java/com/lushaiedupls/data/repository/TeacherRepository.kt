package com.lushaiedupls.data.repository

import com.lushaiedupls.data.mapper.TimetableSubjectParser
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.RequestCoalescer
import com.lushaiedupls.data.remote.TtlCache
import com.lushaiedupls.data.remote.cachedCall
import com.lushaiedupls.data.remote.api.AttendanceApi
import com.lushaiedupls.data.remote.api.CalendarApi
import com.lushaiedupls.data.remote.api.MeApi
import com.lushaiedupls.data.remote.api.NotificationsApi
import com.lushaiedupls.data.remote.api.OverviewApi
import com.lushaiedupls.data.remote.api.TeachingUnitsApi
import com.lushaiedupls.data.remote.api.TimetableApi
import com.lushaiedupls.data.remote.dto.AddMemberRequest
import com.lushaiedupls.data.remote.dto.ApproveRollNumbersRequest
import com.lushaiedupls.data.remote.dto.AttendanceStatus
import com.lushaiedupls.data.remote.dto.CalendarEventOut
import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.DayView
import com.lushaiedupls.data.remote.dto.DeletedResponse
import com.lushaiedupls.data.remote.dto.EntryInput
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.MemberOut
import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.NotificationAudience
import com.lushaiedupls.data.remote.dto.NotificationCreate
import com.lushaiedupls.data.remote.dto.NotificationOut
import com.lushaiedupls.data.remote.dto.NotificationUpdate
import com.lushaiedupls.data.remote.dto.PeriodCreate
import com.lushaiedupls.data.remote.dto.PeriodOut
import com.lushaiedupls.data.remote.dto.PeriodReminderUpdateRequest
import com.lushaiedupls.data.remote.dto.PeriodUpdate
import com.lushaiedupls.data.remote.dto.RollNumberAssignment
import com.lushaiedupls.data.remote.dto.RollOut
import com.lushaiedupls.data.remote.dto.RosterResponse
import com.lushaiedupls.data.remote.dto.SetRollNumbersRequest
import com.lushaiedupls.data.remote.dto.SubjectOut
import com.lushaiedupls.data.remote.dto.TeacherOverview
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.UnitAttendanceSummary
import com.lushaiedupls.data.remote.dto.UnreadCountResponse
import com.lushaiedupls.data.remote.dto.UpsertRollRequest
import com.lushaiedupls.data.remote.dto.UserOut
import com.lushaiedupls.data.remote.dto.UserSummary
import com.lushaiedupls.data.remote.dto.WeekView
import com.lushaiedupls.data.remote.safeApiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TeacherRepository(
    private val overviewApi: OverviewApi,
    private val teachingUnitsApi: TeachingUnitsApi,
    private val attendanceApi: AttendanceApi,
    private val calendarApi: CalendarApi,
    private val timetableApi: TimetableApi,
    private val notificationsApi: NotificationsApi,
    private val meApi: MeApi,
) {
    private val _unreadNotificationCount = MutableStateFlow<Int?>(null)
    val unreadNotificationCount: StateFlow<Int?> = _unreadNotificationCount.asStateFlow()
    private val coalescer = RequestCoalescer()
    private val catalogCache = TtlCache(CATALOG_TTL_MS)

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
        classId: String? = null,
        topLimit: Int = 5,
        forceRefresh: Boolean = false,
    ): NetworkResult<TeacherOverview> {
        val key = "overview_${month.orEmpty()}_${classId.orEmpty()}_$topLimit"
        val result = cachedCall(catalogCache, coalescer, key, forceRefresh, OVERVIEW_TTL_MS) {
            safeApiCall { overviewApi.teacherOverview(month, classId, topLimit) }
        }
        if (result is NetworkResult.Success) {
            _unreadNotificationCount.value = result.data.unread_notifications
        }
        return result
    }

    suspend fun teachingUnits(forceRefresh: Boolean = false): NetworkResult<List<TeachingUnitOut>> =
        cachedCall(catalogCache, coalescer, "units", forceRefresh) {
            safeApiCall { teachingUnitsApi.list() }
        }

    suspend fun teachingUnit(unitId: String): NetworkResult<TeachingUnitOut> =
        safeApiCall { teachingUnitsApi.get(unitId) }

    suspend fun members(unitId: String): NetworkResult<List<MemberOut>> =
        safeApiCall { teachingUnitsApi.members(unitId) }

    suspend fun addMember(unitId: String, studentId: String): NetworkResult<MemberOut> {
        val result = safeApiCall { teachingUnitsApi.addMember(unitId, AddMemberRequest(studentId)) }
        if (result is NetworkResult.Success) catalogCache.remove("units")
        return result
    }

    suspend fun removeMember(unitId: String, studentId: String): NetworkResult<MessageResponse> {
        val result = safeApiCall { teachingUnitsApi.removeMember(unitId, studentId) }
        if (result is NetworkResult.Success) catalogCache.remove("units")
        return result
    }

    suspend fun setRollNumbers(
        unitId: String,
        assignments: List<RollNumberAssignment>,
    ): NetworkResult<List<MemberOut>> {
        val result = safeApiCall {
            teachingUnitsApi.setRollNumbers(unitId, SetRollNumbersRequest(assignments))
        }
        if (result is NetworkResult.Success) catalogCache.remove("units")
        return result
    }

    suspend fun approveRollNumbers(
        unitId: String,
        studentIds: List<String>? = null,
    ): NetworkResult<List<MemberOut>> {
        val result = safeApiCall {
            teachingUnitsApi.approveRollNumbers(unitId, ApproveRollNumbersRequest(studentIds))
        }
        if (result is NetworkResult.Success) catalogCache.remove("units")
        return result
    }

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
        entries: List<Triple<String, AttendanceStatus, String?>>,
        extraLabel: String? = null,
    ): NetworkResult<RollOut> {
        val result = safeApiCall {
            attendanceApi.upsertRoll(
                UpsertRollRequest(
                    teaching_unit_id = unitId,
                    attendance_date = date,
                    period_id = if (isExtraClass) null else periodId,
                    is_extra_class = isExtraClass,
                    extra_label = if (isExtraClass) extraLabel else null,
                    entries = entries.map { (studentId, status, note) ->
                        EntryInput(
                            student_id = studentId,
                            status = status,
                            note = note?.trim()?.take(255)?.takeIf { it.isNotEmpty() },
                        )
                    },
                ),
            )
        }
        if (result is NetworkResult.Success) catalogCache.removePrefix("overview_")
        return result
    }

    suspend fun calendarEvents(
        from: String? = null,
        to: String? = null,
    ): NetworkResult<List<CalendarEventOut>> = safeApiCall {
        calendarApi.events(from, to)
    }

    suspend fun timetable(
        teachingUnitId: String? = null,
        institutionId: String? = null,
        forceRefresh: Boolean = false,
    ): NetworkResult<WeekView> =
        cachedCall(
            catalogCache,
            coalescer,
            "me_${teachingUnitId.orEmpty()}_${institutionId.orEmpty()}",
            forceRefresh,
            TIMETABLE_TTL_MS,
        ) {
            safeApiCall { timetableApi.myTimetable(teachingUnitId, institutionId) }
        }

    suspend fun weekTimetable(
        institutionId: String,
        classId: String? = null,
        forceRefresh: Boolean = false,
    ): NetworkResult<WeekView> =
        cachedCall(
            catalogCache,
            coalescer,
            "week_${institutionId}_${classId.orEmpty()}",
            forceRefresh,
            TIMETABLE_TTL_MS,
        ) {
            safeApiCall { timetableApi.weekTimetable(institutionId, classId) }
        }

    suspend fun institutions(forceRefresh: Boolean = false): NetworkResult<List<InstitutionOut>> =
        cachedCall(catalogCache, coalescer, "institutions", forceRefresh, INSTITUTION_TTL_MS) {
            safeApiCall { timetableApi.institutions() }
        }

    suspend fun timetableClasses(
        institutionId: String,
        forceRefresh: Boolean = false,
    ): NetworkResult<List<ClassOut>> =
        cachedCall(catalogCache, coalescer, "classes_$institutionId", forceRefresh) {
            safeApiCall { timetableApi.timetableClasses(institutionId) }
        }

    suspend fun timetableClassSubjects(
        classId: String,
        institutionId: String,
        forceRefresh: Boolean = false,
    ): NetworkResult<List<SubjectOut>> =
        cachedCall(catalogCache, coalescer, "subjects_${classId}_$institutionId", forceRefresh) {
            safeApiCall {
                TimetableSubjectParser.parse(
                    timetableApi.timetableClassSubjects(classId, institutionId),
                )
            }
        }

    suspend fun periods(
        institutionId: String,
        includeInactive: Boolean = false,
        forceRefresh: Boolean = false,
    ): NetworkResult<List<PeriodOut>> =
        cachedCall(
            catalogCache,
            coalescer,
            "periods_${institutionId}_$includeInactive",
            forceRefresh,
        ) {
            safeApiCall { timetableApi.periods(includeInactive, institutionId) }
        }

    suspend fun createPeriod(body: PeriodCreate): NetworkResult<PeriodOut> {
        val result = safeApiCall { timetableApi.createPeriod(body) }
        if (result is NetworkResult.Success) catalogCache.removePrefix("periods_")
        return result
    }

    suspend fun updatePeriod(periodId: String, body: PeriodUpdate): NetworkResult<PeriodOut> {
        val result = safeApiCall { timetableApi.updatePeriod(periodId, body) }
        if (result is NetworkResult.Success) catalogCache.removePrefix("periods_")
        return result
    }

    suspend fun deletePeriod(periodId: String): NetworkResult<DeletedResponse> {
        val result = safeApiCall { timetableApi.deletePeriod(periodId) }
        if (result is NetworkResult.Success) catalogCache.removePrefix("periods_")
        return result
    }

    suspend fun setSlots(
        unitId: String,
        slots: List<com.lushaiedupls.data.remote.dto.SlotInput>,
    ): NetworkResult<List<com.lushaiedupls.data.remote.dto.SlotOut>> {
        val result = safeApiCall {
            timetableApi.setSlots(unitId, com.lushaiedupls.data.remote.dto.SetSlotsRequest(slots))
        }
        if (result is NetworkResult.Success) invalidateTimetable()
        return result
    }

    suspend fun deleteSlot(slotId: String): NetworkResult<MessageResponse> {
        val result = safeApiCall { timetableApi.deleteSlot(slotId) }
        if (result is NetworkResult.Success) invalidateTimetable()
        return result
    }

    private fun invalidateTimetable() {
        catalogCache.removePrefix("week_")
        catalogCache.removePrefix("me_")
        catalogCache.removePrefix("overview_")
    }

    suspend fun profile(): NetworkResult<UserOut> = safeApiCall { meApi.me() }

    /**
     * Teachers only. Pass null to turn reminders off.
     * Server clamps/validates 5–120 and sends FCM; do not schedule locally.
     */
    suspend fun updatePeriodReminder(leadMinutes: Int?): NetworkResult<UserOut> = safeApiCall {
        val clamped = leadMinutes?.coerceIn(5, 120)
        meApi.updatePeriodReminder(PeriodReminderUpdateRequest(lead_minutes = clamped))
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

    suspend fun updateNotification(
        notificationId: String,
        title: String,
        body: String,
    ): NetworkResult<NotificationOut> = safeApiCall {
        notificationsApi.update(
            notificationId,
            NotificationUpdate(title = title, body = body),
        )
    }

    suspend fun deleteNotification(notificationId: String): NetworkResult<MessageResponse> =
        safeApiCall { notificationsApi.delete(notificationId) }

    companion object {
        private const val CATALOG_TTL_MS = 60_000L
        private const val INSTITUTION_TTL_MS = 90_000L
        private const val OVERVIEW_TTL_MS = 30_000L
        private const val TIMETABLE_TTL_MS = 30_000L
    }
}
