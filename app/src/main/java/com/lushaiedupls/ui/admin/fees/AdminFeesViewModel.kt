package com.lushaiedupls.ui.admin.fees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.FeeHistoryMappers
import com.lushaiedupls.data.remote.FeeMonth
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.AdminFeeSummaryOut
import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.FeeLedgerOut
import com.lushaiedupls.data.remote.dto.FeePaymentStatus
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.SubjectMonthlyFeeOut
import com.lushaiedupls.data.remote.dto.SubjectOut
import com.lushaiedupls.data.remote.dto.UserSummary
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.common.viewModelFactory
import java.time.YearMonth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AdminFeesTab {
    Templates,
    Ledgers,
}

enum class FeeTemplateLockStatus {
    Editable,
    Locked,
}

data class AdminFeeTemplateRow(
    val fee: SubjectMonthlyFeeOut,
    val lockStatus: FeeTemplateLockStatus,
)

data class AdminFeeLedgerRow(
    val ledger: FeeLedgerOut,
    val rollNumber: Int,
    val subjectLabel: String,
)

data class AdminFeesUiState(
    val selectedTab: AdminFeesTab = AdminFeesTab.Templates,
    val month: String = YearMonth.now().toString(),
    val institutions: List<InstitutionOut> = emptyList(),
    val selectedInstitutionId: String? = null,
    val classes: List<ClassOut> = emptyList(),
    val selectedClassId: String? = null,
    val subjects: List<SubjectOut> = emptyList(),
    val selectedSubjectId: String? = null,
    val students: List<UserSummary> = emptyList(),
    val selectedStudentId: String? = null,
    val selectedPaymentStatus: FeePaymentStatus? = null,
    val summary: AdminFeeSummaryOut? = null,
    val templates: List<AdminFeeTemplateRow> = emptyList(),
    val ledgers: List<AdminFeeLedgerRow> = emptyList(),
    val amountRupees: String = "",
    val editingFeeId: String? = null,
    val isAddingTemplate: Boolean = false,
    val showBulkDeleteConfirm: Boolean = false,
    val includePaidInBulkDelete: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val infoMessage: String? = null,
)

class AdminFeesViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminFeesUiState(isLoading = true))
    val uiState: StateFlow<AdminFeesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectTab(tab: AdminFeesTab) {
        if (tab == _uiState.value.selectedTab) return
        _uiState.update {
            it.copy(
                selectedTab = tab,
                isAddingTemplate = false,
                editingFeeId = null,
                showBulkDeleteConfirm = false,
                includePaidInBulkDelete = false,
                errorMessage = null,
                successMessage = null,
                infoMessage = null,
            )
        }
    }

    fun selectInstitution(institutionId: String) {
        if (institutionId == _uiState.value.selectedInstitutionId) return
        _uiState.update {
            it.copy(
                selectedInstitutionId = institutionId,
                selectedClassId = null,
                selectedSubjectId = null,
                selectedStudentId = null,
                classes = emptyList(),
                subjects = emptyList(),
                students = emptyList(),
                templates = emptyList(),
                ledgers = emptyList(),
                amountRupees = "",
                editingFeeId = null,
                isAddingTemplate = false,
                showBulkDeleteConfirm = false,
                includePaidInBulkDelete = false,
                errorMessage = null,
                successMessage = null,
                infoMessage = null,
            )
        }
        refresh()
    }

    fun selectClass(classId: String) {
        if (classId == _uiState.value.selectedClassId) return
        _uiState.update {
            it.copy(
                selectedClassId = classId,
                selectedSubjectId = null,
                selectedStudentId = null,
                subjects = emptyList(),
                students = emptyList(),
                amountRupees = "",
                editingFeeId = null,
                isAddingTemplate = false,
                showBulkDeleteConfirm = false,
                includePaidInBulkDelete = false,
                errorMessage = null,
            )
        }
        refresh()
    }

    fun selectSubject(subjectId: String) {
        if (subjectId == _uiState.value.selectedSubjectId) return
        _uiState.update {
            it.copy(
                selectedSubjectId = subjectId,
                amountRupees = "",
                editingFeeId = null,
                isAddingTemplate = false,
                showBulkDeleteConfirm = false,
                includePaidInBulkDelete = false,
            )
        }
        refresh()
    }

    fun selectStudent(studentId: String?) {
        if (studentId == _uiState.value.selectedStudentId) return
        _uiState.update { it.copy(selectedStudentId = studentId) }
        refreshLedgersOnly()
    }

    fun selectPaymentStatus(status: FeePaymentStatus?) {
        if (status == _uiState.value.selectedPaymentStatus) return
        _uiState.update { it.copy(selectedPaymentStatus = status) }
        refreshLedgersOnly()
    }

    fun selectMonth(month: String) {
        if (month == _uiState.value.month) return
        _uiState.update {
            it.copy(
                month = month,
                amountRupees = "",
                editingFeeId = null,
                isAddingTemplate = false,
                showBulkDeleteConfirm = false,
                includePaidInBulkDelete = false,
            )
        }
        refresh()
    }

    fun onAmountChange(value: String) {
        _uiState.update { it.copy(amountRupees = value.filter { ch -> ch.isDigit() }) }
    }

    fun startEditTemplate(fee: SubjectMonthlyFeeOut) {
        _uiState.update {
            it.copy(
                selectedClassId = fee.class_id,
                selectedSubjectId = fee.subject_id,
                amountRupees = (fee.amount_paise / 100).toString(),
                editingFeeId = fee.id,
                isAddingTemplate = false,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun openAddTemplate() {
        _uiState.update {
            it.copy(
                isAddingTemplate = true,
                editingFeeId = null,
                amountRupees = "",
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun clearTemplateForm() {
        _uiState.update {
            it.copy(
                amountRupees = "",
                editingFeeId = null,
                isAddingTemplate = false,
                showBulkDeleteConfirm = false,
                includePaidInBulkDelete = false,
                errorMessage = null,
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = _uiState.value.institutions.isNotEmpty() ||
                _uiState.value.templates.isNotEmpty() ||
                _uiState.value.ledgers.isNotEmpty() ||
                _uiState.value.summary != null
            _uiState.update {
                it.copy(
                    isLoading = !hasContent,
                    isRefreshing = hasContent,
                    errorMessage = if (it.isAddingTemplate || it.editingFeeId != null) {
                        it.errorMessage
                    } else {
                        null
                    },
                    successMessage = if (it.isAddingTemplate || it.editingFeeId != null) {
                        it.successMessage
                    } else {
                        null
                    },
                )
            }
            val month = _uiState.value.month
            if (!FeeMonth.isYearMonth(month)) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = FeeMonth.INVALID_MESSAGE,
                    )
                }
                return@launch
            }
            val institutions = when (val result = adminRepository.listInstitutions(includeInactive = false)) {
                is NetworkResult.Success -> result.data
                    .filter { it.is_active }
                    .sortedWith(compareBy({ it.sort_order }, { it.name }))
                else -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = result.userMessage(),
                        )
                    }
                    return@launch
                }
            }
            val institutionId = _uiState.value.selectedInstitutionId
                ?.takeIf { id -> institutions.any { it.id == id } }
                ?: institutions.firstOrNull()?.id
            if (institutionId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        institutions = institutions,
                        classes = emptyList(),
                        subjects = emptyList(),
                        templates = emptyList(),
                        ledgers = emptyList(),
                        errorMessage = "Select an institution first.",
                    )
                }
                return@launch
            }
            val classes = when (
                val result = adminRepository.listClasses(
                    includeInactive = false,
                    institutionId = institutionId,
                )
            ) {
                is NetworkResult.Success -> result.data.sortedWith(
                    compareBy({ it.sort_order }, { it.name }),
                )
                else -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            institutions = institutions,
                            selectedInstitutionId = institutionId,
                            errorMessage = result.userMessage(),
                        )
                    }
                    return@launch
                }
            }
            val classId = _uiState.value.selectedClassId
                ?.takeIf { id -> classes.any { it.id == id } }
                ?: classes.firstOrNull()?.id
            if (classId == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        institutions = institutions,
                        selectedInstitutionId = institutionId,
                        classes = classes,
                        selectedClassId = null,
                        subjects = emptyList(),
                        selectedSubjectId = null,
                        summary = null,
                        templates = emptyList(),
                        ledgers = emptyList(),
                        errorMessage = null,
                    )
                }
                return@launch
            }
            val subjects = when (
                    val result = adminRepository.listSubjects(
                        classId = classId,
                        institutionId = institutionId,
                        includeInactive = false,
                    )
                ) {
                    is NetworkResult.Success -> result.data
                        .filter { it.is_active }
                        .sortedWith(compareBy({ it.sort_order }, { it.name }))
                    else -> emptyList()
                }
            val subjectId = _uiState.value.selectedSubjectId
                ?.takeIf { id -> subjects.any { it.id == id } }
                ?: subjects.firstOrNull()?.id
            val students = studentsForClass(classId)
            val selectedStudentId = _uiState.value.selectedStudentId
                ?.takeIf { id -> students.any { it.id == id } }
            _uiState.update {
                it.copy(
                    institutions = institutions,
                    selectedInstitutionId = institutionId,
                    classes = classes,
                    selectedClassId = classId,
                    subjects = subjects,
                    selectedSubjectId = subjectId,
                    students = students,
                    selectedStudentId = selectedStudentId,
                )
            }
            coroutineScope {
                val templatesDeferred = async {
                    adminRepository.listSubjectMonthlyFees(
                        month = month,
                        classId = classId,
                        subjectId = subjectId,
                    )
                }
                val lockLedgersDeferred = async {
                    adminRepository.listLedgers(month = month, classId = classId)
                }
                val ledgersDeferred = async {
                    adminRepository.listLedgers(
                        month = month,
                        classId = classId,
                        studentId = selectedStudentId,
                        subjectId = subjectId,
                        paymentStatus = _uiState.value.selectedPaymentStatus,
                    )
                }
                val summaryDeferred = async {
                    adminRepository.feeSummary(month, classId)
                }
                val rollsDeferred = async { rollNumbersForClass(classId) }
                val templatesResult = templatesDeferred.await()
                val subjectFees = (templatesResult as? NetworkResult.Success)?.data.orEmpty()
                    .sortedWith(compareBy({ it.class_name }, { it.subject_name }, { it.month }))
                val lockLedgers =
                    (lockLedgersDeferred.await() as? NetworkResult.Success)?.data.orEmpty()
                val templates = toTemplateRows(subjectFees, lockLedgers)
                val matchingFee = subjectFees.firstOrNull { it.subject_id == subjectId }
                val amountRupees = when {
                    _uiState.value.editingFeeId != null -> _uiState.value.amountRupees
                    matchingFee != null -> (matchingFee.amount_paise / 100).toString()
                    else -> _uiState.value.amountRupees
                }
                val ledgersResult = ledgersDeferred.await()
                val rolls = rollsDeferred.await()
                val ledgers = when (ledgersResult) {
                    is NetworkResult.Success -> toLedgerRows(ledgersResult.data, rolls, subjects)
                    else -> emptyList()
                }
                val summary = when (val result = summaryDeferred.await()) {
                    is NetworkResult.Success -> result.data
                    else -> null
                }
                val ledgerError = if (ledgersResult is NetworkResult.Error) {
                    ledgersResult.userMessage()
                } else {
                    null
                }
                val templateError = if (templatesResult is NetworkResult.Error) {
                    templatesResult.userMessage()
                } else {
                    null
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        summary = summary,
                        templates = templates,
                        ledgers = ledgers,
                        amountRupees = amountRupees,
                        errorMessage = ledgerError ?: templateError,
                    )
                }
            }
        }
    }

    private fun refreshLedgersOnly() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!FeeMonth.isYearMonth(state.month)) return@launch
            val hasContent = state.ledgers.isNotEmpty() || state.summary != null
            _uiState.update {
                it.copy(
                    isLoading = !hasContent,
                    isRefreshing = hasContent,
                    errorMessage = null,
                )
            }
            when (
                val result = adminRepository.listLedgers(
                    month = state.month,
                    classId = state.selectedClassId,
                    studentId = state.selectedStudentId,
                    subjectId = state.selectedSubjectId,
                    paymentStatus = state.selectedPaymentStatus,
                )
            ) {
                is NetworkResult.Success -> {
                    val rolls = rollNumbersForClass(state.selectedClassId)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            ledgers = toLedgerRows(result.data, rolls, state.subjects),
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = result.userMessage(),
                    )
                }
            }
        }
    }

    fun saveSubjectFee() {
        val state = _uiState.value
        val subjectId = state.selectedSubjectId ?: return
        val rupees = state.amountRupees.toIntOrNull() ?: return
        if (rupees <= 0) return
        if (!FeeMonth.isYearMonth(state.month)) {
            _uiState.update { it.copy(errorMessage = FeeMonth.INVALID_MESSAGE) }
            return
        }
        val editingTemplate = state.templates.firstOrNull { it.fee.id == state.editingFeeId }
        if (editingTemplate?.lockStatus == FeeTemplateLockStatus.Locked) {
            _uiState.update { it.copy(errorMessage = FeeMonth.TEMPLATE_LOCKED_MESSAGE) }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true, errorMessage = null, successMessage = null, infoMessage = null)
            }
            when (
                val result = adminRepository.upsertSubjectMonthlyFee(
                    subjectId = subjectId,
                    month = state.month,
                    amountPaise = rupees * 100,
                )
            ) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = FeeMonth.TEMPLATE_SAVED_MESSAGE,
                        )
                    }
                    refresh()
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun deleteSubjectFee(feeId: String) {
        if (feeId.isBlank()) return
        val locked = _uiState.value.templates
            .firstOrNull { it.fee.id == feeId }
            ?.lockStatus == FeeTemplateLockStatus.Locked
        if (locked) {
            _uiState.update { it.copy(errorMessage = FeeMonth.TEMPLATE_LOCKED_MESSAGE) }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true, errorMessage = null, successMessage = null, infoMessage = null)
            }
            when (val result = adminRepository.deleteSubjectMonthlyFee(feeId)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = FeeMonth.TEMPLATE_DELETED_MESSAGE,
                            editingFeeId = if (it.editingFeeId == feeId) null else it.editingFeeId,
                        )
                    }
                    refresh()
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun generate() {
        val month = _uiState.value.month
        if (!FeeMonth.isYearMonth(month)) {
            _uiState.update { it.copy(errorMessage = FeeMonth.INVALID_MESSAGE) }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true, errorMessage = null, successMessage = null, infoMessage = null)
            }
            when (val result = adminRepository.generateLedgers(month)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            infoMessage = result.data.message,
                            selectedTab = AdminFeesTab.Ledgers,
                        )
                    }
                    refresh()
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun openBulkDelete() {
        _uiState.update {
            it.copy(
                showBulkDeleteConfirm = true,
                includePaidInBulkDelete = false,
                errorMessage = null,
            )
        }
    }

    fun closeBulkDelete() {
        _uiState.update {
            it.copy(
                showBulkDeleteConfirm = false,
                includePaidInBulkDelete = false,
            )
        }
    }

    fun setIncludePaidInBulkDelete(includePaid: Boolean) {
        _uiState.update { it.copy(includePaidInBulkDelete = includePaid) }
    }

    fun confirmBulkDelete() {
        val state = _uiState.value
        if (!FeeMonth.isYearMonth(state.month)) {
            _uiState.update { it.copy(errorMessage = FeeMonth.INVALID_MESSAGE) }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true, errorMessage = null, successMessage = null, infoMessage = null)
            }
            when (
                val result = adminRepository.deleteLedgersBulk(
                    month = state.month,
                    classId = state.selectedClassId,
                    subjectId = state.selectedSubjectId,
                    includePaid = state.includePaidInBulkDelete,
                )
            ) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            showBulkDeleteConfirm = false,
                            includePaidInBulkDelete = false,
                            infoMessage = FeeMonth.bulkDeleteResultMessage(
                                apiMessage = result.data.message,
                                matchedCount = result.data.matched_count,
                                deletedCount = result.data.deleted_count,
                                month = state.month,
                            ),
                        )
                    }
                    refresh()
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun togglePaid(item: FeeLedgerOut) {
        val next = if (item.payment_status == FeePaymentStatus.PAID) {
            FeePaymentStatus.NOT_PAID
        } else {
            FeePaymentStatus.PAID
        }
        viewModelScope.launch {
            when (
                val result = adminRepository.updateLedger(item.id, next, item.amount_paise)
            ) {
                is NetworkResult.Success -> refresh()
                else -> _uiState.update { it.copy(errorMessage = result.userMessage()) }
            }
        }
    }

    private suspend fun studentsForClass(classId: String): List<UserSummary> {
        val units = (adminRepository.teachingUnits() as? NetworkResult.Success)?.data.orEmpty()
        val unitId = units.firstOrNull { it.class_id == classId }?.id ?: return emptyList()
        return (adminRepository.members(unitId) as? NetworkResult.Success)?.data.orEmpty()
            .map { it.student }
            .sortedBy { it.name }
    }

    private suspend fun rollNumbersForClass(classId: String?): Map<String, Int> {
        if (classId == null) return emptyMap()
        val units = (adminRepository.teachingUnits() as? NetworkResult.Success)?.data.orEmpty()
        val unitId = units.firstOrNull { it.class_id == classId }?.id ?: return emptyMap()
        val members = (adminRepository.members(unitId) as? NetworkResult.Success)?.data.orEmpty()
        return members.mapNotNull { member ->
            member.roll_no?.takeIf { it > 0 }?.let { member.student.id to it }
        }.toMap()
    }

    private fun toTemplateRows(
        fees: List<SubjectMonthlyFeeOut>,
        ledgers: List<FeeLedgerOut>,
    ): List<AdminFeeTemplateRow> {
        val lockedKeys = ledgers.mapNotNull { ledger ->
            val subjectId = ledger.subject_id ?: return@mapNotNull null
            val classId = ledger.class_id ?: return@mapNotNull null
            Triple(ledger.month, classId, subjectId)
        }.toSet()
        return fees.map { fee ->
            val locked = Triple(fee.month, fee.class_id, fee.subject_id) in lockedKeys
            AdminFeeTemplateRow(
                fee = fee,
                lockStatus = if (locked) {
                    FeeTemplateLockStatus.Locked
                } else {
                    FeeTemplateLockStatus.Editable
                },
            )
        }
    }

    private fun toLedgerRows(
        ledgers: List<FeeLedgerOut>,
        rolls: Map<String, Int>,
        subjects: List<SubjectOut>,
    ): List<AdminFeeLedgerRow> {
        val codeById = subjects.associate { it.id to it.code }
        val sorted = ledgers.sortedWith(
            compareBy<FeeLedgerOut> { rolls[it.student.id] ?: Int.MAX_VALUE }
                .thenBy { it.student.name }
                .thenBy { it.subject_name.orEmpty() },
        )
        return sorted.mapIndexed { index, ledger ->
            AdminFeeLedgerRow(
                ledger = ledger,
                rollNumber = rolls[ledger.student.id] ?: (index + 1),
                subjectLabel = FeeHistoryMappers.subjectLabel(
                    ledger,
                    subjectCode = ledger.subject_id?.let { codeById[it] },
                ),
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(adminRepository: AdminRepository): ViewModelProvider.Factory =
            viewModelFactory { AdminFeesViewModel(adminRepository) }
    }
}
