package com.lushaiedupls.ui.student.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.mock.AiSubjectItem
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.AiSubjectOut
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.needsAdminApproval
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

class StudentAiHubViewModel(
    private val studentRepository: StudentRepository,
) : ViewModel() {

    private val initialAiSubjects = studentRepository.getCachedAiSubjects()
    private val initialUnits = studentRepository.getCachedTeachingUnits()
    private val initialOverview = studentRepository.getCachedProgressOverview()
    private val initialSubjects = when {
        !initialAiSubjects.isNullOrEmpty() -> StudentUiMappers.aiSubjects(initialAiSubjects)
        !initialUnits.isNullOrEmpty() -> StudentUiMappers.teachingUnitSubjects(initialUnits)
        else -> emptyList()
    }
    private val initialStats = initialOverview?.let(StudentUiMappers::aiHubStats)
        ?: StudentUiMappers.emptyAiHubStats()

    private val _uiState = MutableStateFlow(
        StudentAiHubUiState(
            isLoading = initialSubjects.isEmpty(),
            subjects = initialSubjects,
            stats = initialStats,
        ),
    )
    val uiState: StateFlow<StudentAiHubUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectInstitution(index: Int) {
        val institutionId = _uiState.value.institutionIds.getOrNull(index) ?: return
        if (institutionId == _uiState.value.selectedInstitutionId) return
        _uiState.update {
            it.copy(
                selectedInstitutionId = institutionId,
                subjects = emptyList(),
                isLoading = true,
                errorMessage = null,
            )
        }
        refresh()
    }

    fun refresh(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val hasContent = _uiState.value.subjects.isNotEmpty()
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
                val initialQuery = StudentUiMappers.queryInstitutionId(
                    institutionIds = _uiState.value.institutionIds,
                    selectedInstitutionId = _uiState.value.selectedInstitutionId,
                )
                val statsDeferred = async {
                    studentRepository.progressOverview(
                        forceRefresh = bypassCache,
                        institutionId = initialQuery,
                    )
                }
                val aiSubjectsDeferred = async {
                    studentRepository.aiSubjects(
                        forceRefresh = bypassCache,
                        institutionId = initialQuery,
                    )
                }
                val unitsDeferred = async { studentRepository.teachingUnits(forceRefresh = bypassCache) }

                var aiSubjectsResult = aiSubjectsDeferred.await()
                val unitsResult = unitsDeferred.await()
                val units = (unitsResult as? NetworkResult.Success)?.data.orEmpty()
                var aiSubjects = (aiSubjectsResult as? NetworkResult.Success)?.data.orEmpty()
                var subjects = resolveSubjects(aiSubjectsResult, aiSubjects, units)
                var institutions = StudentUiMappers.mergeInstitutionOptions(
                    StudentUiMappers.institutionOptions(subjects),
                )
                val selectedInstitutionId = resolveSelectedInstitution(
                    institutionIds = institutions.map { it.first },
                    current = _uiState.value.selectedInstitutionId,
                )
                val scopedQuery = StudentUiMappers.queryInstitutionId(
                    institutionIds = institutions.map { it.first },
                    selectedInstitutionId = selectedInstitutionId,
                )
                var statsResult = statsDeferred.await()
                if (scopedQuery != null && scopedQuery != initialQuery) {
                    aiSubjectsResult = studentRepository.aiSubjects(
                        forceRefresh = bypassCache,
                        institutionId = scopedQuery,
                    )
                    aiSubjects = (aiSubjectsResult as? NetworkResult.Success)?.data.orEmpty()
                    subjects = resolveSubjects(aiSubjectsResult, aiSubjects, units)
                    institutions = StudentUiMappers.mergeInstitutionOptions(
                        StudentUiMappers.institutionOptions(subjects),
                        institutions,
                    )
                    statsResult = studentRepository.progressOverview(
                        forceRefresh = bypassCache,
                        institutionId = scopedQuery,
                    )
                }
                val visibleInstitutions = institutions.takeIf { it.size > 1 }.orEmpty()
                val error = when {
                    aiSubjectsResult is NetworkResult.Success ||
                        unitsResult is NetworkResult.Success -> null
                    else -> unitsResult.userMessage().ifBlank { aiSubjectsResult.userMessage() }
                }
                val needsApproval = listOf(aiSubjectsResult, unitsResult, statsResult)
                    .any { it.needsAdminApproval() }
                val stats = if (statsResult is NetworkResult.Success) {
                    StudentUiMappers.aiHubStats(statsResult.data)
                } else {
                    _uiState.value.stats
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        stats = stats,
                        subjects = if (
                            aiSubjectsResult is NetworkResult.Success ||
                            unitsResult is NetworkResult.Success
                        ) {
                            subjects
                        } else {
                            subjects.ifEmpty { it.subjects }
                        },
                        institutions = visibleInstitutions.map { option -> option.second },
                        institutionIds = visibleInstitutions.map { option -> option.first },
                        selectedInstitutionId = selectedInstitutionId.takeIf {
                            visibleInstitutions.isNotEmpty()
                        },
                        needsApproval = needsApproval,
                        errorMessage = if (needsApproval) null else error,
                    )
                }
                val firstSubjectId = subjects.firstOrNull()?.id
                if (!firstSubjectId.isNullOrBlank()) {
                    val chResult = studentRepository.chapters(firstSubjectId, forceRefresh = bypassCache)
                    if (chResult is NetworkResult.Success) {
                        val textbookId = chResult.data
                            .firstOrNull { it.textbook_id.isNotBlank() }
                            ?.textbook_id
                        if (!textbookId.isNullOrBlank()) {
                            studentRepository.progressResume(textbookId, forceRefresh = bypassCache)
                        }
                        val activeChapterIds = chResult.data.filter { it.is_active }.map { it.id }
                        studentRepository.preferredChatPrefetchChapterId(activeChapterIds)
                            ?.let { studentRepository.prefetchAiChat(listOf(it)) }
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            studentRepository: StudentRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            StudentAiHubViewModel(studentRepository)
        }

        private fun resolveSubjects(
            aiSubjectsResult: NetworkResult<*>,
            aiSubjects: List<AiSubjectOut>,
            units: List<TeachingUnitOut>,
        ): List<AiSubjectItem> = when {
            aiSubjectsResult is NetworkResult.Success -> StudentUiMappers.aiSubjects(aiSubjects)
            units.isNotEmpty() -> StudentUiMappers.teachingUnitSubjects(units)
            else -> emptyList()
        }

        private fun resolveSelectedInstitution(
            institutionIds: List<String>,
            current: String?,
        ): String? = current?.takeIf { it in institutionIds } ?: institutionIds.firstOrNull()
    }
}
