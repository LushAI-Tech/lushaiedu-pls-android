package com.lushaiedupls.ui.admin.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.AdminFeedbackUpdateRequest
import com.lushaiedupls.data.remote.dto.FeedbackStatus
import com.lushaiedupls.data.remote.dto.ParentFeedbackOut
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminFeedbackUiState(
    val items: List<ParentFeedbackOut> = emptyList(),
    val filter: FeedbackStatus? = FeedbackStatus.UNSEEN,
    val selected: ParentFeedbackOut? = null,
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class AdminFeedbackViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminFeedbackUiState(isLoading = true))
    val uiState: StateFlow<AdminFeedbackUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun setFilter(status: FeedbackStatus?) {
        _uiState.update { it.copy(filter = status, selected = null) }
        refresh()
    }

    fun select(item: ParentFeedbackOut) {
        _uiState.update { it.copy(selected = item, notes = item.admin_notes.orEmpty()) }
    }

    fun closeDetail() {
        _uiState.update { it.copy(selected = null) }
    }

    fun onNotes(value: String) = _uiState.update { it.copy(notes = value) }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = adminRepository.listFeedback(_uiState.value.filter)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, items = result.data)
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun markSeen() {
        val item = _uiState.value.selected ?: return
        update(item.id, AdminFeedbackUpdateRequest(status = FeedbackStatus.SEEN))
    }

    fun saveNotes() {
        val item = _uiState.value.selected ?: return
        update(
            item.id,
            AdminFeedbackUpdateRequest(admin_notes = _uiState.value.notes.trim().ifBlank { null }),
        )
    }

    private fun update(id: String, body: AdminFeedbackUpdateRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (val result = adminRepository.updateFeedback(id, body)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, selected = result.data, notes = result.data.admin_notes.orEmpty()) }
                    refresh()
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    companion object {
        fun provideFactory(adminRepository: AdminRepository): ViewModelProvider.Factory =
            viewModelFactory { AdminFeedbackViewModel(adminRepository) }
    }
}
