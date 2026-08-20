package com.lushaiedupls.ui.student.secondary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.mock.ChapterItem
import com.lushaiedupls.data.mock.SubjectChapterStats
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChaptersUiState(
    val stats: SubjectChapterStats? = null,
    val chapters: List<ChapterItem> = emptyList(),
    val subjectId: String = "",
    val subjectTitle: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class ChaptersViewModel(
    private val studentRepository: StudentRepository,
    private val subjectIdHint: String?,
    private val subjectNameHint: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChaptersUiState(isLoading = true))
    val uiState: StateFlow<ChaptersUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val subjectsDeferred = async { studentRepository.aiSubjects() }
                val dashboardDeferred = async { studentRepository.progressDashboard() }
                val subjects = subjectsDeferred.await()
                val dashboard = dashboardDeferred.await()
                val subjectId = subjectIdHint?.takeIf { it.isNotBlank() }
                    ?: (subjects as? NetworkResult.Success)?.data?.firstOrNull()?.subject_id
                val subjectTitle = subjectNameHint?.takeIf { it.isNotBlank() }
                    ?: (subjects as? NetworkResult.Success)?.data
                        ?.find { it.subject_id == subjectId }?.name
                    ?: ""
                if (subjectId.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (subjects !is NetworkResult.Success) {
                                subjects.userMessage()
                            } else {
                                "No AI subjects available."
                            },
                        )
                    }
                    return@coroutineScope
                }
                val chapters = studentRepository.chapters(subjectId)
                if (chapters is NetworkResult.Success) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            subjectId = subjectId,
                            subjectTitle = subjectTitle,
                            stats = (dashboard as? NetworkResult.Success)?.data
                                ?.let(StudentUiMappers::chapterStats)
                                ?: SubjectChapterStats("0%", "0%", "0", "0"),
                            chapters = StudentUiMappers.chapters(chapters.data),
                        )
                    }
                    val activeChapterIds = chapters.data.filter { it.is_active }.map { it.id }
                    if (activeChapterIds.isNotEmpty()) {
                        studentRepository.prefetchAiChat(activeChapterIds)
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = chapters.userMessage())
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            studentRepository: StudentRepository,
            subjectIdHint: String? = null,
            subjectNameHint: String? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            ChaptersViewModel(studentRepository, subjectIdHint, subjectNameHint)
        }
    }
}
