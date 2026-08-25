package com.lushaiedupls.data.repository

import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.api.AdminApi
import com.lushaiedupls.data.remote.api.CalendarApi
import com.lushaiedupls.data.remote.api.NotificationsApi
import com.lushaiedupls.data.remote.api.OverviewApi
import com.lushaiedupls.data.remote.api.TeachingUnitsApi
import com.lushaiedupls.data.remote.api.TimetableApi
import com.lushaiedupls.data.remote.dto.AdminFeedbackUpdateRequest
import com.lushaiedupls.data.remote.dto.AdminFeeSummaryOut
import com.lushaiedupls.data.remote.dto.AdminOverview
import com.lushaiedupls.data.remote.dto.AdminUserCreateRequest
import com.lushaiedupls.data.remote.dto.AdminUserCreateResponse
import com.lushaiedupls.data.remote.dto.AdminUserEditRequest
import com.lushaiedupls.data.remote.dto.CalendarEventCreate
import com.lushaiedupls.data.remote.dto.CalendarEventOut
import com.lushaiedupls.data.remote.dto.CalendarEventUpdate
import com.lushaiedupls.data.remote.dto.ClassCreate
import com.lushaiedupls.data.remote.dto.ClassMonthlyFeeOut
import com.lushaiedupls.data.remote.dto.ClassMonthlyFeeUpsertRequest
import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.ClassReassignRequest
import com.lushaiedupls.data.remote.dto.ClassUpdate
import com.lushaiedupls.data.remote.dto.DeletedResponse
import com.lushaiedupls.data.remote.dto.FeeLedgerOut
import com.lushaiedupls.data.remote.dto.FeeLedgerUpdateRequest
import com.lushaiedupls.data.remote.dto.FeePaymentStatus
import com.lushaiedupls.data.remote.dto.FeedbackStatus
import com.lushaiedupls.data.remote.dto.InviteCodeCreate
import com.lushaiedupls.data.remote.dto.InviteCodeOut
import com.lushaiedupls.data.remote.dto.LedgerGenerationRequest
import com.lushaiedupls.data.remote.dto.MemberOut
import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.NotificationCreate
import com.lushaiedupls.data.remote.dto.NotificationOut
import com.lushaiedupls.data.remote.dto.NotificationUpdate
import com.lushaiedupls.data.remote.dto.PaginatedUsersResponse
import com.lushaiedupls.data.remote.dto.ParentFeedbackOut
import com.lushaiedupls.data.remote.dto.PeriodCreate
import com.lushaiedupls.data.remote.dto.PeriodOut
import com.lushaiedupls.data.remote.dto.PeriodUpdate
import com.lushaiedupls.data.remote.dto.SetSlotsRequest
import com.lushaiedupls.data.remote.dto.SlotInput
import com.lushaiedupls.data.remote.dto.SlotOut
import com.lushaiedupls.data.remote.dto.StemBindingRequest
import com.lushaiedupls.data.remote.dto.StemNode
import com.lushaiedupls.data.remote.dto.SubjectCreate
import com.lushaiedupls.data.remote.dto.SubjectOut
import com.lushaiedupls.data.remote.dto.SubjectUpdate
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.UnreadCountResponse
import com.lushaiedupls.data.remote.dto.UserOut
import com.lushaiedupls.data.remote.dto.UserRole
import com.lushaiedupls.data.remote.dto.UserStatus
import com.lushaiedupls.data.remote.dto.UserStatusUpdate
import com.lushaiedupls.data.remote.dto.WeekView
import com.lushaiedupls.data.remote.safeApiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminRepository(
    private val adminApi: AdminApi,
    private val overviewApi: OverviewApi,
    private val calendarApi: CalendarApi,
    private val timetableApi: TimetableApi,
    private val teachingUnitsApi: TeachingUnitsApi,
    private val notificationsApi: NotificationsApi,
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

    suspend fun overview(month: String? = null): NetworkResult<AdminOverview> =
        safeApiCall { overviewApi.adminOverview(month) }

    suspend fun unreadCount(): NetworkResult<UnreadCountResponse> {
        val res = safeApiCall { notificationsApi.unreadCount() }
        if (res is NetworkResult.Success) {
            _unreadNotificationCount.value = res.data.unread
        }
        return res
    }

    suspend fun listUsers(
        role: UserRole? = null,
        status: UserStatus? = null,
        query: String? = null,
        includeDeleted: Boolean = false,
        page: Int = 1,
        limit: Int = 50,
    ): NetworkResult<PaginatedUsersResponse> = safeApiCall {
        adminApi.listUsers(
            role = role?.name,
            status = status?.name,
            query = query?.trim()?.takeIf { it.isNotEmpty() },
            includeDeleted = includeDeleted,
            page = page.coerceAtLeast(1),
            limit = limit.coerceIn(1, 100),
        )
    }

    suspend fun createUser(body: AdminUserCreateRequest): NetworkResult<AdminUserCreateResponse> =
        safeApiCall { adminApi.createUser(body) }

    suspend fun editUser(userId: String, body: AdminUserEditRequest): NetworkResult<UserOut> =
        safeApiCall { adminApi.editUser(userId, body) }

    suspend fun approveUser(userId: String): NetworkResult<UserOut> =
        safeApiCall { adminApi.approveUser(userId) }

    suspend fun rejectUser(userId: String): NetworkResult<UserOut> =
        safeApiCall { adminApi.rejectUser(userId) }

    suspend fun updateUserStatus(userId: String, status: UserStatus): NetworkResult<UserOut> =
        safeApiCall { adminApi.updateUserStatus(userId, UserStatusUpdate(status)) }

    suspend fun reassignClass(userId: String, classId: String): NetworkResult<UserOut> =
        safeApiCall { adminApi.reassignClass(userId, ClassReassignRequest(classId)) }

    suspend fun listClasses(includeInactive: Boolean = true): NetworkResult<List<ClassOut>> =
        safeApiCall { adminApi.listClasses(includeInactive) }

    suspend fun createClass(body: ClassCreate): NetworkResult<ClassOut> =
        safeApiCall { adminApi.createClass(body) }

    suspend fun updateClass(classId: String, body: ClassUpdate): NetworkResult<ClassOut> =
        safeApiCall { adminApi.updateClass(classId, body) }

    suspend fun deleteClass(classId: String): NetworkResult<DeletedResponse> =
        safeApiCall { adminApi.deleteClass(classId) }

    suspend fun listSubjects(
        classId: String,
        includeInactive: Boolean = true,
    ): NetworkResult<List<SubjectOut>> =
        safeApiCall { adminApi.listSubjects(classId, includeInactive) }

    suspend fun createSubject(body: SubjectCreate): NetworkResult<SubjectOut> =
        safeApiCall { adminApi.createSubject(body) }

    suspend fun updateSubject(subjectId: String, body: SubjectUpdate): NetworkResult<SubjectOut> =
        safeApiCall { adminApi.updateSubject(subjectId, body) }

    suspend fun deleteSubject(subjectId: String): NetworkResult<DeletedResponse> =
        safeApiCall { adminApi.deleteSubject(subjectId) }

    suspend fun bindStem(subjectId: String, stemSubjectId: String?): NetworkResult<SubjectOut> =
        safeApiCall { adminApi.bindStem(subjectId, StemBindingRequest(stemSubjectId)) }

    suspend fun stemBoards(): NetworkResult<List<StemNode>> =
        safeApiCall { adminApi.stemBoards() }

    suspend fun stemGrades(boardId: String): NetworkResult<List<StemNode>> =
        safeApiCall { adminApi.stemGrades(boardId) }

    suspend fun stemSubjects(gradeId: String): NetworkResult<List<StemNode>> =
        safeApiCall { adminApi.stemSubjects(gradeId) }

    suspend fun listInvites(onlyRedeemable: Boolean = false): NetworkResult<List<InviteCodeOut>> =
        safeApiCall { adminApi.listInvites(onlyRedeemable) }

    suspend fun createInvite(body: InviteCodeCreate): NetworkResult<InviteCodeOut> =
        safeApiCall { adminApi.createInvite(body) }

    suspend fun revokeInvite(inviteId: String): NetworkResult<MessageResponse> =
        safeApiCall { adminApi.revokeInvite(inviteId) }

    suspend fun listFeedback(status: FeedbackStatus? = null): NetworkResult<List<ParentFeedbackOut>> =
        safeApiCall { adminApi.listFeedback(status?.name) }

    suspend fun updateFeedback(
        feedbackId: String,
        body: AdminFeedbackUpdateRequest,
    ): NetworkResult<ParentFeedbackOut> =
        safeApiCall { adminApi.updateFeedback(feedbackId, body) }

    suspend fun feeSummary(month: String, classId: String? = null): NetworkResult<AdminFeeSummaryOut> =
        safeApiCall { adminApi.feeSummary(month, classId) }

    suspend fun listLedgers(
        month: String? = null,
        classId: String? = null,
        paymentStatus: FeePaymentStatus? = null,
    ): NetworkResult<List<FeeLedgerOut>> = safeApiCall {
        adminApi.listLedgers(
            month = month,
            classId = classId,
            paymentStatus = when (paymentStatus) {
                FeePaymentStatus.PAID -> "paid"
                FeePaymentStatus.NOT_PAID -> "not_paid"
                null -> null
            },
        )
    }

    suspend fun upsertClassMonthlyFee(
        classId: String,
        month: String,
        amountPaise: Int,
    ): NetworkResult<ClassMonthlyFeeOut> = safeApiCall {
        adminApi.upsertClassMonthlyFee(
            ClassMonthlyFeeUpsertRequest(
                class_id = classId,
                month = month,
                amount_paise = amountPaise,
            ),
        )
    }

    suspend fun generateLedgers(month: String): NetworkResult<MessageResponse> =
        safeApiCall { adminApi.generateLedgers(LedgerGenerationRequest(month)) }

    suspend fun updateLedger(
        ledgerId: String,
        status: FeePaymentStatus,
        amountPaise: Int,
    ): NetworkResult<FeeLedgerOut> = safeApiCall {
        adminApi.updateLedger(
            ledgerId,
            FeeLedgerUpdateRequest(payment_status = status, amount_paise = amountPaise),
        )
    }

    suspend fun periods(includeInactive: Boolean = true): NetworkResult<List<PeriodOut>> =
        safeApiCall { timetableApi.periods(includeInactive) }

    suspend fun createPeriod(body: PeriodCreate): NetworkResult<PeriodOut> =
        safeApiCall { adminApi.createPeriod(body) }

    suspend fun updatePeriod(periodId: String, body: PeriodUpdate): NetworkResult<PeriodOut> =
        safeApiCall { adminApi.updatePeriod(periodId, body) }

    suspend fun deletePeriod(periodId: String): NetworkResult<DeletedResponse> =
        safeApiCall { adminApi.deletePeriod(periodId) }

    suspend fun teachingUnits(): NetworkResult<List<TeachingUnitOut>> =
        safeApiCall { teachingUnitsApi.list() }

    suspend fun members(unitId: String): NetworkResult<List<MemberOut>> =
        safeApiCall { teachingUnitsApi.members(unitId) }

    suspend fun timetable(teachingUnitId: String? = null): NetworkResult<WeekView> =
        safeApiCall { timetableApi.myTimetable(teachingUnitId) }

    suspend fun setSlots(unitId: String, slots: List<SlotInput>): NetworkResult<List<SlotOut>> =
        safeApiCall { timetableApi.setSlots(unitId, SetSlotsRequest(slots)) }

    suspend fun calendarEvents(
        from: String? = null,
        to: String? = null,
    ): NetworkResult<List<CalendarEventOut>> =
        safeApiCall { calendarApi.events(from, to) }

    suspend fun createCalendarEvent(body: CalendarEventCreate): NetworkResult<CalendarEventOut> =
        safeApiCall { calendarApi.createEvent(body) }

    suspend fun updateCalendarEvent(
        eventId: String,
        body: CalendarEventUpdate,
    ): NetworkResult<CalendarEventOut> =
        safeApiCall { calendarApi.updateEvent(eventId, body) }

    suspend fun deleteCalendarEvent(eventId: String): NetworkResult<MessageResponse> =
        safeApiCall { calendarApi.deleteEvent(eventId) }

    suspend fun notifications(): NetworkResult<List<NotificationOut>> =
        safeApiCall { notificationsApi.list() }

    suspend fun createNotification(body: NotificationCreate): NetworkResult<NotificationOut> =
        safeApiCall { notificationsApi.create(body) }

    suspend fun updateNotification(
        notificationId: String,
        body: NotificationUpdate,
    ): NetworkResult<NotificationOut> =
        safeApiCall { notificationsApi.update(notificationId, body) }

    suspend fun deleteNotification(notificationId: String): NetworkResult<MessageResponse> =
        safeApiCall { notificationsApi.delete(notificationId) }

    suspend fun markNotificationRead(id: String): NetworkResult<MessageResponse> {
        val res = safeApiCall { notificationsApi.markRead(id) }
        if (res is NetworkResult.Success) decrementUnreadNotificationCount()
        return res
    }
}
