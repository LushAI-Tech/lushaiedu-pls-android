package com.lushaiedupls.data.repository

import com.lushaiedupls.data.mapper.TimetableSubjectParser
import com.lushaiedupls.data.remote.FeeMonth
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.RequestCoalescer
import com.lushaiedupls.data.remote.TtlCache
import com.lushaiedupls.data.remote.cachedCall
import com.lushaiedupls.data.remote.api.AdminApi
import com.lushaiedupls.data.remote.api.CalendarApi
import com.lushaiedupls.data.remote.api.NotificationsApi
import com.lushaiedupls.data.remote.api.OverviewApi
import com.lushaiedupls.data.remote.api.TeachingUnitsApi
import com.lushaiedupls.data.remote.api.TimetableApi
import com.lushaiedupls.data.remote.dto.AdminFeedbackUpdateRequest
import com.lushaiedupls.data.remote.dto.AdminFeeSummaryOut
import com.lushaiedupls.data.remote.dto.AdminOverview
import com.lushaiedupls.data.remote.dto.AdminParentLinkCreateRequest
import com.lushaiedupls.data.remote.dto.AdminUserCreateRequest
import com.lushaiedupls.data.remote.dto.AdminUserCreateResponse
import com.lushaiedupls.data.remote.dto.AdminUserEditRequest
import com.lushaiedupls.data.remote.dto.CalendarEventCreate
import com.lushaiedupls.data.remote.dto.CalendarEventOut
import com.lushaiedupls.data.remote.dto.CalendarEventUpdate
import com.lushaiedupls.data.remote.dto.ClassCreate
import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.ClassReassignRequest
import com.lushaiedupls.data.remote.dto.ClassUpdate
import com.lushaiedupls.data.remote.dto.DeletedResponse
import com.lushaiedupls.data.remote.dto.FeeLedgerBulkDeleteRequest
import com.lushaiedupls.data.remote.dto.FeeLedgerBulkDeleteResponse
import com.lushaiedupls.data.remote.dto.FeeLedgerOut
import com.lushaiedupls.data.remote.dto.FeeLedgerUpdateRequest
import com.lushaiedupls.data.remote.dto.FeePaymentStatus
import com.lushaiedupls.data.remote.dto.FeedbackStatus
import com.lushaiedupls.data.remote.dto.InstitutionCreate
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.InstitutionUpdate
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
import com.lushaiedupls.data.remote.dto.ParentLinkOut
import com.lushaiedupls.data.remote.dto.ParentRelationship
import com.lushaiedupls.data.remote.dto.PeriodCreate
import com.lushaiedupls.data.remote.dto.PeriodOut
import com.lushaiedupls.data.remote.dto.PeriodUpdate
import com.lushaiedupls.data.remote.dto.SetSlotsRequest
import com.lushaiedupls.data.remote.dto.SlotInput
import com.lushaiedupls.data.remote.dto.SlotOut
import com.lushaiedupls.data.remote.dto.StemBindingRequest
import com.lushaiedupls.data.remote.dto.StemNode
import com.lushaiedupls.data.remote.dto.SubjectCreate
import com.lushaiedupls.data.remote.dto.SubjectMonthlyFeeOut
import com.lushaiedupls.data.remote.dto.SubjectMonthlyFeeUpsertRequest
import com.lushaiedupls.data.remote.dto.SubjectOut
import com.lushaiedupls.data.remote.dto.SubjectUpdate
import com.lushaiedupls.data.remote.dto.TeacherAssignment
import com.lushaiedupls.data.remote.dto.TeacherInstitutionAssignmentRequest
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.UnreadCountResponse
import com.lushaiedupls.data.remote.dto.UserOut
import com.lushaiedupls.data.remote.dto.UserRole
import com.lushaiedupls.data.remote.dto.UserStatus
import com.lushaiedupls.data.remote.dto.UserStatusUpdate
import com.lushaiedupls.data.remote.dto.WeekView
import com.lushaiedupls.data.remote.safeApiCall
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
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
    private val _pendingApprovalCount = MutableStateFlow(0)
    val pendingApprovalCount: StateFlow<Int> = _pendingApprovalCount.asStateFlow()

    private val classesCache = ConcurrentHashMap<String, List<ClassOut>>()
    private val subjectsCache = ConcurrentHashMap<String, List<SubjectOut>>()
    private val prefetchingClasses = AtomicBoolean(false)
    private val coalescer = RequestCoalescer()
    private val catalogCache = TtlCache(CATALOG_TTL_MS)

    fun setUnreadNotificationCount(count: Int) {
        _unreadNotificationCount.value = count.coerceAtLeast(0)
    }

    fun decrementUnreadNotificationCount() {
        val current = _unreadNotificationCount.value ?: 1
        setUnreadNotificationCount(current - 1)
    }

    suspend fun refreshPendingApprovalCount() {
        _pendingApprovalCount.value = fetchPendingApprovalCount()
    }

    private suspend fun fetchPendingApprovalCount(): Int {
        var page = 1
        var pending = 0
        while (page <= 20) {
            val result = safeApiCall {
                adminApi.listUsers(
                    role = null,
                    status = UserStatus.PENDING_APPROVAL.name,
                    query = null,
                    includeDeleted = false,
                    page = page,
                    limit = 100,
                )
            }
            val data = (result as? NetworkResult.Success)?.data ?: break
            val pendingInPage = data.items.count { it.status == UserStatus.PENDING_APPROVAL }
            pending += pendingInPage
            val filterIgnored = data.items.any { it.status != UserStatus.PENDING_APPROVAL }
            val lastPage = data.items.isEmpty() || page >= data.total_pages || filterIgnored
            if (lastPage) {
                if (filterIgnored && page == 1) {
                    return countPendingAcrossAllUsers()
                }
                break
            }
            page++
        }
        return pending
    }

    private suspend fun countPendingAcrossAllUsers(): Int {
        var page = 1
        var pending = 0
        while (page <= 20) {
            val result = safeApiCall {
                adminApi.listUsers(
                    role = null,
                    status = null,
                    query = null,
                    includeDeleted = false,
                    page = page,
                    limit = 100,
                )
            }
            val data = (result as? NetworkResult.Success)?.data ?: break
            pending += data.items.count { it.status == UserStatus.PENDING_APPROVAL }
            if (data.items.isEmpty() || page >= data.total_pages) break
            page++
        }
        return pending
    }

    suspend fun overview(
        month: String? = null,
        institutionId: String? = null,
        forceRefresh: Boolean = false,
    ): NetworkResult<AdminOverview> =
        cachedCall(
            catalogCache,
            coalescer,
            "overview_${month.orEmpty()}_${institutionId.orEmpty()}",
            forceRefresh,
            OVERVIEW_TTL_MS,
        ) {
            safeApiCall { overviewApi.adminOverview(month, institutionId) }
        }

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

    suspend fun getUser(userId: String): NetworkResult<UserOut> =
        safeApiCall { adminApi.getUser(userId) }

    suspend fun createUser(body: AdminUserCreateRequest): NetworkResult<AdminUserCreateResponse> =
        safeApiCall { adminApi.createUser(body) }

    suspend fun editUser(userId: String, body: AdminUserEditRequest): NetworkResult<UserOut> =
        safeApiCall { adminApi.editUser(userId, body) }

    suspend fun approveUser(userId: String): NetworkResult<UserOut> {
        val result = safeApiCall { adminApi.approveUser(userId) }
        if (result is NetworkResult.Success) refreshPendingApprovalCount()
        return result
    }

    suspend fun rejectUser(userId: String): NetworkResult<UserOut> {
        val result = safeApiCall { adminApi.rejectUser(userId) }
        if (result is NetworkResult.Success) refreshPendingApprovalCount()
        return result
    }

    suspend fun updateUserStatus(userId: String, status: UserStatus): NetworkResult<UserOut> =
        safeApiCall { adminApi.updateUserStatus(userId, UserStatusUpdate(status)) }

    suspend fun reassignClass(userId: String, classId: String): NetworkResult<UserOut> =
        safeApiCall { adminApi.reassignClass(userId, ClassReassignRequest(classId)) }

    suspend fun assignTeacherInstitution(
        teacherId: String,
        institutionId: String,
        assignments: List<TeacherAssignment>,
        replaceExisting: Boolean = true,
    ): NetworkResult<UserOut> = safeApiCall {
        adminApi.assignTeacherInstitution(
            teacherId,
            TeacherInstitutionAssignmentRequest(
                institution_id = institutionId,
                assignments = assignments,
                replace_existing = replaceExisting,
            ),
        )
    }

    suspend fun createParentLink(
        parentUserId: String,
        studentUserId: String,
        relationship: ParentRelationship = ParentRelationship.GUARDIAN,
    ): NetworkResult<ParentLinkOut> = safeApiCall {
        adminApi.createParentLink(
            AdminParentLinkCreateRequest(
                parent_user_id = parentUserId,
                student_user_id = studentUserId,
                relationship = relationship,
            ),
        )
    }

    fun cachedClasses(
        includeInactive: Boolean = true,
        institutionId: String? = null,
    ): List<ClassOut>? {
        if (institutionId.isNullOrBlank()) return null
        return classesCache[classesKey(includeInactive, institutionId)]
            ?: if (!includeInactive) {
                classesCache[classesKey(true, institutionId)]?.filter { it.is_active }
            } else {
                null
            }
    }

    fun cachedSubjects(
        classId: String,
        includeInactive: Boolean = true,
        institutionId: String? = null,
    ): List<SubjectOut>? {
        if (institutionId.isNullOrBlank()) return null
        return subjectsCache[subjectsKey(classId, includeInactive, institutionId)]
    }

    fun clearCaches() {
        classesCache.clear()
        subjectsCache.clear()
        catalogCache.clear()
        _pendingApprovalCount.value = 0
    }

    /**
     * Warms institutions + classes for one institution. Does not prefetch every subject's list.
     */
    suspend fun prefetchClassesPage(institutionId: String? = null) {
        if (!prefetchingClasses.compareAndSet(false, true)) return
        try {
            val institutions = when (val result = listInstitutions(includeInactive = true)) {
                is NetworkResult.Success -> result.data
                else -> return
            }
            val targetId = institutionId
                ?.takeIf { id -> institutions.any { it.id == id } }
                ?: institutions.firstOrNull()?.id
                ?: return
            listClasses(includeInactive = true, institutionId = targetId)
        } catch (_: Exception) {
            // Best-effort prefetch
        } finally {
            prefetchingClasses.set(false)
        }
    }

    suspend fun listInstitutions(
        includeInactive: Boolean = true,
        forceRefresh: Boolean = false,
    ): NetworkResult<List<InstitutionOut>> =
        cachedCall(
            catalogCache,
            coalescer,
            "institutions_$includeInactive",
            forceRefresh,
            INSTITUTION_TTL_MS,
        ) {
            safeApiCall { adminApi.listInstitutions(includeInactive) }
        }

    suspend fun createInstitution(body: InstitutionCreate): NetworkResult<InstitutionOut> {
        val result = safeApiCall { adminApi.createInstitution(body) }
        if (result is NetworkResult.Success) invalidateInstitutions()
        return result
    }

    suspend fun updateInstitution(
        institutionId: String,
        body: InstitutionUpdate,
    ): NetworkResult<InstitutionOut> {
        val result = safeApiCall { adminApi.updateInstitution(institutionId, body) }
        if (result is NetworkResult.Success) invalidateInstitutions()
        return result
    }

    suspend fun deleteInstitution(institutionId: String): NetworkResult<DeletedResponse> {
        val result = safeApiCall { adminApi.deleteInstitution(institutionId) }
        if (result is NetworkResult.Success) {
            invalidateInstitutions()
            invalidateClasses()
            invalidateSubjects()
        }
        return result
    }

    suspend fun listClasses(
        includeInactive: Boolean = true,
        institutionId: String,
        forceRefresh: Boolean = false,
    ): NetworkResult<List<ClassOut>> {
        val key = classesKey(includeInactive, institutionId)
        if (!forceRefresh) {
            cachedClasses(includeInactive, institutionId)?.let { return NetworkResult.Success(it) }
        }
        return coalescer.run("classes_$key") {
            if (!forceRefresh) {
                cachedClasses(includeInactive, institutionId)?.let {
                    return@run NetworkResult.Success(it)
                }
            }
            val result = safeApiCall {
                adminApi.listClasses(
                    institutionId = institutionId,
                    includeInactive = includeInactive,
                )
            }
            if (result is NetworkResult.Success) {
                classesCache[key] = result.data
                if (includeInactive) {
                    classesCache[classesKey(false, institutionId)] = result.data.filter { it.is_active }
                }
            }
            result
        }
    }

    suspend fun listClassesAcrossInstitutions(
        includeInactive: Boolean = false,
        forceRefresh: Boolean = false,
    ): NetworkResult<List<ClassOut>> {
        val institutions = when (val result = listInstitutions(includeInactive = true)) {
            is NetworkResult.Success -> result.data.filter { includeInactive || it.is_active }
            is NetworkResult.Error -> return result
            is NetworkResult.Exception -> return result
        }
        if (institutions.isEmpty()) return NetworkResult.Success(emptyList())
        val all = mutableListOf<ClassOut>()
        var firstError: NetworkResult<List<ClassOut>>? = null
        for (institution in institutions) {
            when (
                val result = listClasses(
                    includeInactive = includeInactive,
                    institutionId = institution.id,
                    forceRefresh = forceRefresh,
                )
            ) {
                is NetworkResult.Success -> all += result.data
                else -> if (firstError == null) firstError = result
            }
        }
        return if (all.isNotEmpty() || firstError == null) {
            NetworkResult.Success(all.distinctBy { it.id })
        } else {
            firstError
        }
    }

    suspend fun createClass(body: ClassCreate): NetworkResult<ClassOut> {
        val result = safeApiCall { adminApi.createClass(body) }
        if (result is NetworkResult.Success) invalidateClasses()
        return result
    }

    suspend fun updateClass(
        classId: String,
        institutionId: String,
        body: ClassUpdate,
    ): NetworkResult<ClassOut> {
        val result = safeApiCall { adminApi.updateClass(classId, institutionId, body) }
        if (result is NetworkResult.Success) invalidateClasses()
        return result
    }

    suspend fun deleteClass(classId: String): NetworkResult<DeletedResponse> {
        val result = safeApiCall { adminApi.deleteClass(classId) }
        if (result is NetworkResult.Success) {
            invalidateClasses()
            invalidateSubjects(classId)
        }
        return result
    }

    suspend fun listSubjects(
        classId: String,
        institutionId: String,
        includeInactive: Boolean = true,
        forceRefresh: Boolean = false,
    ): NetworkResult<List<SubjectOut>> {
        val key = subjectsKey(classId, includeInactive, institutionId)
        if (!forceRefresh) {
            cachedSubjects(classId, includeInactive, institutionId)?.let {
                return NetworkResult.Success(it)
            }
        }
        return coalescer.run("subjects_$key") {
            if (!forceRefresh) {
                cachedSubjects(classId, includeInactive, institutionId)?.let {
                    return@run NetworkResult.Success(it)
                }
            }
            val result = safeApiCall {
                adminApi.listSubjects(
                    classId = classId,
                    institutionId = institutionId,
                    includeInactive = includeInactive,
                )
            }
            if (result is NetworkResult.Success) {
                subjectsCache[key] = result.data
                if (includeInactive) {
                    subjectsCache[subjectsKey(classId, false, institutionId)] =
                        result.data.filter { it.is_active }
                }
            }
            result
        }
    }

    suspend fun createSubject(body: SubjectCreate): NetworkResult<SubjectOut> {
        val result = safeApiCall { adminApi.createSubject(body) }
        if (result is NetworkResult.Success) invalidateSubjects(body.class_id)
        return result
    }

    suspend fun updateSubject(
        subjectId: String,
        institutionId: String,
        body: SubjectUpdate,
    ): NetworkResult<SubjectOut> {
        val result = safeApiCall { adminApi.updateSubject(subjectId, institutionId, body) }
        if (result is NetworkResult.Success) invalidateSubjects()
        return result
    }

    suspend fun deleteSubject(subjectId: String): NetworkResult<DeletedResponse> {
        val result = safeApiCall { adminApi.deleteSubject(subjectId) }
        if (result is NetworkResult.Success) invalidateSubjects()
        return result
    }

    suspend fun bindStem(subjectId: String, stemSubjectId: String?): NetworkResult<SubjectOut> {
        val result = safeApiCall { adminApi.bindStem(subjectId, StemBindingRequest(stemSubjectId)) }
        if (result is NetworkResult.Success) invalidateSubjects()
        return result
    }

    private fun invalidateInstitutions() {
        catalogCache.removePrefix("institutions_")
        catalogCache.removePrefix("overview_")
    }

    private fun invalidateClasses() {
        classesCache.clear()
        catalogCache.removePrefix("overview_")
    }

    private fun invalidateSubjects(classId: String? = null) {
        if (classId == null) {
            subjectsCache.clear()
            return
        }
        subjectsCache.keys.filter { it.startsWith("$classId|") }.forEach { subjectsCache.remove(it) }
    }

    private fun classesKey(includeInactive: Boolean, institutionId: String): String =
        "$includeInactive|$institutionId"

    private fun subjectsKey(
        classId: String,
        includeInactive: Boolean,
        institutionId: String,
    ): String = "$classId|$includeInactive|$institutionId"

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

    suspend fun feeSummary(month: String, classId: String? = null): NetworkResult<AdminFeeSummaryOut> {
        val validMonth = FeeMonth.yearMonthOrNull(month) ?: return FeeMonth.invalidMonthError()
        return safeApiCall { adminApi.feeSummary(validMonth, classId) }
    }

    suspend fun listLedgers(
        month: String? = null,
        classId: String? = null,
        studentId: String? = null,
        subjectId: String? = null,
        paymentStatus: FeePaymentStatus? = null,
    ): NetworkResult<List<FeeLedgerOut>> {
        if (!FeeMonth.isListFilter(month)) return FeeMonth.invalidMonthError()
        return safeApiCall {
            adminApi.listLedgers(
                month = FeeMonth.listFilterOrNull(month),
                classId = classId,
                studentId = studentId,
                subjectId = subjectId,
                paymentStatus = when (paymentStatus) {
                    FeePaymentStatus.PAID -> "paid"
                    FeePaymentStatus.NOT_PAID -> "not_paid"
                    null -> null
                },
            )
        }
    }

    suspend fun listSubjectMonthlyFees(
        month: String? = null,
        classId: String? = null,
        subjectId: String? = null,
    ): NetworkResult<List<SubjectMonthlyFeeOut>> {
        if (!FeeMonth.isListFilter(month)) return FeeMonth.invalidMonthError()
        return safeApiCall {
            adminApi.listSubjectMonthlyFees(
                month = FeeMonth.listFilterOrNull(month),
                classId = classId,
                subjectId = subjectId,
            )
        }
    }

    suspend fun getSubjectMonthlyFee(feeId: String): NetworkResult<SubjectMonthlyFeeOut> =
        safeApiCall { adminApi.getSubjectMonthlyFee(feeId) }

    suspend fun deleteSubjectMonthlyFee(feeId: String): NetworkResult<MessageResponse> =
        safeApiCall { adminApi.deleteSubjectMonthlyFee(feeId) }

    suspend fun upsertSubjectMonthlyFee(
        subjectId: String,
        month: String,
        amountPaise: Int,
    ): NetworkResult<SubjectMonthlyFeeOut> {
        val validMonth = FeeMonth.yearMonthOrNull(month) ?: return FeeMonth.invalidMonthError()
        return safeApiCall {
            adminApi.upsertSubjectMonthlyFee(
                SubjectMonthlyFeeUpsertRequest(
                    subject_id = subjectId,
                    month = validMonth,
                    amount_paise = amountPaise,
                ),
            )
        }
    }

    suspend fun generateLedgers(month: String): NetworkResult<MessageResponse> {
        val validMonth = FeeMonth.yearMonthOrNull(month) ?: return FeeMonth.invalidMonthError()
        return safeApiCall { adminApi.generateLedgers(LedgerGenerationRequest(validMonth)) }
    }

    suspend fun deleteLedgersBulk(
        month: String,
        classId: String? = null,
        subjectId: String? = null,
        studentId: String? = null,
        includePaid: Boolean = false,
    ): NetworkResult<FeeLedgerBulkDeleteResponse> {
        val validMonth = FeeMonth.yearMonthOrNull(month) ?: return FeeMonth.invalidMonthError()
        return safeApiCall {
            adminApi.deleteLedgersBulk(
                FeeLedgerBulkDeleteRequest(
                    month = validMonth,
                    class_id = classId?.takeIf { it.isNotBlank() },
                    subject_id = subjectId?.takeIf { it.isNotBlank() },
                    student_id = studentId?.takeIf { it.isNotBlank() },
                    include_paid = includePaid,
                ),
            )
        }
    }

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

    suspend fun periods(
        includeInactive: Boolean = true,
        institutionId: String,
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

    suspend fun teachingUnits(forceRefresh: Boolean = false): NetworkResult<List<TeachingUnitOut>> =
        cachedCall(catalogCache, coalescer, "units", forceRefresh) {
            safeApiCall { teachingUnitsApi.list() }
        }

    suspend fun members(unitId: String): NetworkResult<List<MemberOut>> =
        safeApiCall { teachingUnitsApi.members(unitId) }

    suspend fun timetableInstitutions(forceRefresh: Boolean = false): NetworkResult<List<InstitutionOut>> =
        cachedCall(catalogCache, coalescer, "tt_institutions", forceRefresh, INSTITUTION_TTL_MS) {
            safeApiCall { timetableApi.institutions() }
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

    suspend fun timetableClasses(
        institutionId: String,
        forceRefresh: Boolean = false,
    ): NetworkResult<List<ClassOut>> =
        cachedCall(catalogCache, coalescer, "tt_classes_$institutionId", forceRefresh) {
            safeApiCall { timetableApi.timetableClasses(institutionId) }
        }

    suspend fun timetableClassSubjects(
        classId: String,
        institutionId: String,
        forceRefresh: Boolean = false,
    ): NetworkResult<List<SubjectOut>> =
        cachedCall(catalogCache, coalescer, "tt_subjects_${classId}_$institutionId", forceRefresh) {
            safeApiCall {
                TimetableSubjectParser.parse(
                    timetableApi.timetableClassSubjects(classId, institutionId),
                )
            }
        }

    suspend fun setSlots(unitId: String, slots: List<SlotInput>): NetworkResult<List<SlotOut>> {
        val result = safeApiCall { timetableApi.setSlots(unitId, SetSlotsRequest(slots)) }
        if (result is NetworkResult.Success) {
            catalogCache.removePrefix("week_")
            catalogCache.removePrefix("me_")
        }
        return result
    }

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

    companion object {
        private const val CATALOG_TTL_MS = 60_000L
        private const val INSTITUTION_TTL_MS = 90_000L
        private const val OVERVIEW_TTL_MS = 30_000L
        private const val TIMETABLE_TTL_MS = 30_000L
    }
}
