package com.lushaiedupls.ui.parent.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.FeedbackStatus
import com.lushaiedupls.data.remote.dto.LinkedStudentOut
import com.lushaiedupls.data.remote.dto.ParentFeedbackOut
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParentFeedbackUiState(
    val items: List<ParentFeedbackOut> = emptyList(),
    val children: List<LinkedStudentOut> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val composing: Boolean = false,
    val editingId: String? = null,
    val subject: String = "",
    val message: String = "",
    val studentId: String? = null,
    val adminNotes: String? = null,
    val status: FeedbackStatus? = null,
)

class ParentFeedbackViewModel(
    private val parentRepository: ParentRepository,
    private val initialStudentId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentFeedbackUiState(isLoading = true))
    val uiState: StateFlow<ParentFeedbackUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val childrenDeferred = async { parentRepository.linkedStudents() }
                val itemsDeferred = async { parentRepository.feedback() }
                val childrenResult = childrenDeferred.await()
                val itemsResult = itemsDeferred.await()
                val children = (childrenResult as? NetworkResult.Success)?.data.orEmpty()
                when (itemsResult) {
                    is NetworkResult.Success -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            children = children,
                            items = sortFeedback(itemsResult.data),
                            errorMessage = null,
                        )
                    }
                    else -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            children = children,
                            errorMessage = itemsResult.userMessage(),
                        )
                    }
                }
            }
        }
    }

    fun startCreate() {
        _uiState.update {
            it.copy(
                composing = true,
                editingId = null,
                subject = "",
                message = "",
                studentId = initialStudentId
                    ?.takeIf { id -> it.children.any { child -> child.student.id == id } },
                adminNotes = null,
                status = null,
                errorMessage = null,
            )
        }
    }

    fun startEdit(item: ParentFeedbackOut) {
        _uiState.update {
            it.copy(
                composing = true,
                editingId = item.id,
                subject = item.subject,
                message = item.message,
                studentId = item.student?.id,
                adminNotes = item.admin_notes,
                status = item.status,
                errorMessage = null,
            )
        }
    }

    fun cancelComposer() {
        _uiState.update {
            it.copy(
                composing = false,
                editingId = null,
                subject = "",
                message = "",
                studentId = null,
                adminNotes = null,
                status = null,
                errorMessage = null,
                isSaving = false,
            )
        }
    }

    fun setSubject(value: String) {
        _uiState.update { it.copy(subject = value.take(200), errorMessage = null) }
    }

    fun setMessage(value: String) {
        _uiState.update { it.copy(message = value.take(5000), errorMessage = null) }
    }

    fun setStudentId(studentId: String?) {
        _uiState.update { it.copy(studentId = studentId) }
    }

    fun save() {
        val state = _uiState.value
        val subject = state.subject.trim()
        val message = state.message.trim()
        if (subject.isEmpty() || message.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Subject and message are required.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = if (state.editingId == null) {
                parentRepository.createFeedback(subject, message, state.studentId)
            } else {
                parentRepository.updateFeedback(
                    feedbackId = state.editingId,
                    subject = subject,
                    message = message,
                    studentId = state.studentId,
                )
            }
            when (result) {
                is NetworkResult.Success -> {
                    cancelComposer()
                    refresh()
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun deleteCurrent() {
        val id = _uiState.value.editingId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (val result = parentRepository.deleteFeedback(id)) {
                is NetworkResult.Success -> {
                    cancelComposer()
                    refresh()
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            parentRepository: ParentRepository,
            studentId: String?,
        ): ViewModelProvider.Factory = viewModelFactory {
            ParentFeedbackViewModel(parentRepository, studentId)
        }
    }
}

private fun sortFeedback(items: List<ParentFeedbackOut>): List<ParentFeedbackOut> {
    val order = mapOf(
        FeedbackStatus.UNSEEN to 0,
        FeedbackStatus.SEEN to 1,
    )
    return items.sortedWith(
        compareBy<ParentFeedbackOut> { order[it.status] ?: 9 }
            .thenByDescending { it.created_at },
    )
}
