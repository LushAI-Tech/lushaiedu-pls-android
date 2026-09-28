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
import com.lushaiedupls.ui.common.reloadUiFlags
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
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

class ChaptersViewModel(
    private val studentRepository: StudentRepository,
    private val subjectIdHint: String?,
    private val subjectNameHint: String?,
) : ViewModel() {

    private val cachedChapters = subjectIdHint?.let { studentRepository.getCachedChapters(it) }.orEmpty()
    private val cachedOverview = studentRepository.getCachedProgressOverview(
        subjectId = subjectIdHint,
        textbookId = cachedChapters.firstOrNull { it.textbook_id.isNotBlank() }?.textbook_id,
    )

    private val _uiState = MutableStateFlow(
        ChaptersUiState(
            isLoading = cachedChapters.isEmpty(),
            subjectId = subjectIdHint.orEmpty(),
            subjectTitle = subjectNameHint.orEmpty(),
            chapters = StudentUiMappers.chapters(cachedChapters),
            stats = StudentUiMappers.chapterStats(
                overview = cachedOverview,
                subjectId = subjectIdHint,
                textbookId = cachedChapters.firstOrNull { it.textbook_id.isNotBlank() }?.textbook_id,
            ),
        ),
    )
    val uiState: StateFlow<ChaptersUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val hasContent = _uiState.value.chapters.isNotEmpty()
            val (loading, refreshing) = reloadUiFlags(_uiState.value.isLoading, hasContent)
            val bypassCache = forceRefresh || refreshing
            _uiState.update {
                it.copy(
                    isRefreshing = refreshing,
                    isLoading = loading,
                    errorMessage = null,
                )
            }
            coroutineScope {
                val resolvedSubjectId = subjectIdHint?.takeIf { it.isNotBlank() }
                val chaptersDeferred = if (!resolvedSubjectId.isNullOrBlank()) {
                    async { studentRepository.chapters(resolvedSubjectId, forceRefresh = bypassCache) }
                } else null

                val subjectsDeferred = async { studentRepository.aiSubjects(forceRefresh = bypassCache) }
                val overviewDeferred = async {
                    studentRepository.progressOverview(
                        forceRefresh = bypassCache,
                        subjectId = resolvedSubjectId,
                    )
                }

                val subjectId = resolvedSubjectId
                    ?: (subjectsDeferred.await() as? NetworkResult.Success)?.data?.firstOrNull()?.subject_id

                val subjectTitle = subjectNameHint?.takeIf { it.isNotBlank() }
                    ?: (subjectsDeferred.await() as? NetworkResult.Success)?.data
                        ?.find { it.subject_id == subjectId }?.name
                    ?: ""

                if (subjectId.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = "No AI subjects available.",
                        )
                    }
                    return@coroutineScope
                }

                val chDeferred = chaptersDeferred ?: async {
                    studentRepository.chapters(subjectId, forceRefresh = bypassCache)
                }

                // Handle chapters as soon as ready
                launch {
                    val chapters = chDeferred.await()
                    if (chapters is NetworkResult.Success) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                subjectId = subjectId,
                                subjectTitle = subjectTitle.ifBlank { it.subjectTitle },
                                chapters = StudentUiMappers.chapters(chapters.data),
                            )
                        }
                        val textbookId = chapters.data
                            .firstOrNull { it.textbook_id.isNotBlank() }
                            ?.textbook_id
                        if (!textbookId.isNullOrBlank()) {
                            studentRepository.progressResume(textbookId, forceRefresh = bypassCache)
                        }
                        val activeChapterIds = chapters.data.filter { it.is_active }.map { it.id }
                        studentRepository.preferredChatPrefetchChapterId(activeChapterIds)
                            ?.let { studentRepository.prefetchAiChat(listOf(it)) }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = if (_uiState.value.chapters.isEmpty()) chapters.userMessage() else null,
                            )
                        }
                    }
                }

                // Handle stats as soon as ready
                launch {
                    val overview = overviewDeferred.await()
                    if (overview is NetworkResult.Success) {
                        val stats = StudentUiMappers.chapterStats(
                            overview = overview.data,
                            subjectId = subjectId,
                            textbookId = studentRepository.textbookIdForSubject(subjectId),
                        ) ?: return@launch
                        _uiState.update { state -> state.copy(stats = stats) }
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
