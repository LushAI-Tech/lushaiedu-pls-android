package com.lushaiedupls.ui.admin.fees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.AdminFeeSummaryOut
import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.FeeLedgerOut
import com.lushaiedupls.data.remote.dto.FeePaymentStatus
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

data class AdminFeeLedgerRow(
    val ledger: FeeLedgerOut,
    val rollNumber: Int,
)

data class AdminFeesUiState(
    val month: String = YearMonth.now().toString(),
    val classes: List<ClassOut> = emptyList(),
    val selectedClassId: String? = null,
    val summary: AdminFeeSummaryOut? = null,
    val ledgers: List<AdminFeeLedgerRow> = emptyList(),
    val amountRupees: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
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

    fun selectClass(classId: String) {
        _uiState.update { it.copy(selectedClassId = classId) }
        refresh()
    }

    fun selectMonth(month: String) {
        if (month == _uiState.value.month) return
        _uiState.update { it.copy(month = month) }
        refresh()
    }

    fun onAmountChange(value: String) {
        _uiState.update { it.copy(amountRupees = value.filter { ch -> ch.isDigit() }) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val month = _uiState.value.month
            when (val classesResult = adminRepository.listClasses(includeInactive = false)) {
                is NetworkResult.Success -> {
                    val classes = classesResult.data
                    val classId = _uiState.value.selectedClassId
                        ?.takeIf { id -> classes.any { it.id == id } }
                        ?: classes.firstOrNull()?.id
                    coroutineScope {
                        val summaryDeferred = async { adminRepository.feeSummary(month, classId) }
                        val ledgersDeferred = async { adminRepository.listLedgers(month, classId) }
                        val rollsDeferred = async { rollNumbersForClass(classId) }
                        when (val summary = summaryDeferred.await()) {
                            is NetworkResult.Success -> {
                                val ledgers =
                                    (ledgersDeferred.await() as? NetworkResult.Success)?.data.orEmpty()
                                val rolls = rollsDeferred.await()
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        classes = classes,
                                        selectedClassId = classId,
                                        summary = summary.data,
                                        ledgers = toLedgerRows(ledgers, rolls),
                                    )
                                }
                            }
                            else -> _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    classes = classes,
                                    selectedClassId = classId,
                                    errorMessage = summary.userMessage(),
                                )
                            }
                        }
                    }
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = classesResult.userMessage())
                }
            }
        }
    }

    fun saveClassFee() {
        val state = _uiState.value
        val classId = state.selectedClassId ?: return
        val rupees = state.amountRupees.toIntOrNull() ?: return
        if (rupees <= 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (
                val result = adminRepository.upsertClassMonthlyFee(
                    classId = classId,
                    month = state.month,
                    amountPaise = rupees * 100,
                )
            ) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, amountRupees = "") }
                    refresh()
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun generate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, infoMessage = null) }
            when (val result = adminRepository.generateLedgers(_uiState.value.month)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(isSaving = false, infoMessage = result.data.message)
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

    private suspend fun rollNumbersForClass(classId: String?): Map<String, Int> {
        if (classId == null) return emptyMap()
        val units = (adminRepository.teachingUnits() as? NetworkResult.Success)?.data.orEmpty()
        val unitId = units.firstOrNull { it.class_id == classId }?.id ?: return emptyMap()
        val members = (adminRepository.members(unitId) as? NetworkResult.Success)?.data.orEmpty()
        return members.mapNotNull { member ->
            member.roll_no?.takeIf { it > 0 }?.let { member.student.id to it }
        }.toMap()
    }

    private fun toLedgerRows(
        ledgers: List<FeeLedgerOut>,
        rolls: Map<String, Int>,
    ): List<AdminFeeLedgerRow> {
        val sorted = ledgers.sortedWith(
            compareBy<FeeLedgerOut> { rolls[it.student.id] ?: Int.MAX_VALUE }
                .thenBy { it.student.name },
        )
        return sorted.mapIndexed { index, ledger ->
            AdminFeeLedgerRow(
                ledger = ledger,
                rollNumber = rolls[ledger.student.id] ?: (index + 1),
            )
        }
    }

    companion object {
        fun provideFactory(adminRepository: AdminRepository): ViewModelProvider.Factory =
            viewModelFactory { AdminFeesViewModel(adminRepository) }
    }
}
