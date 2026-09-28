package com.lushaiedupls.ui.teacher.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.mock.AiChatMessage
import com.lushaiedupls.data.mock.AiMenuContentItem
import com.lushaiedupls.data.mock.AiMenuTab
import com.lushaiedupls.data.mock.AiSubjectItem
import com.lushaiedupls.data.mock.AiSyllabusItem
import com.lushaiedupls.data.mock.TeacherMockRepository
import com.lushaiedupls.data.mock.isQuestionAskMessage
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.needsAdminApproval
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.common.reloadUiFlags
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.student.ai.PendingAsk
import com.lushaiedupls.ui.student.ai.StudentAiChatScreen
import com.lushaiedupls.ui.student.ai.StudentAiChatUiState
import com.lushaiedupls.ui.student.ai.StudentAiHubScreen
import com.lushaiedupls.ui.student.ai.StudentAiHubUiState
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeacherAiHubViewModel(
    private val studentRepository: StudentRepository,
    private val teacherRepository: TeacherRepository,
    private val userSessionStore: UserSessionStore? = null,
) : ViewModel() {

    private var allSubjects: List<AiSubjectItem> = emptyList()

    private val _uiState = MutableStateFlow(StudentAiHubUiState(isLoading = true))
    val uiState: StateFlow<StudentAiHubUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onClassSelected(classLabel: String) {
        applyFilter(_uiState.value.copy(selectedClass = classLabel))
    }

    fun selectInstitution(index: Int) {
        val institutionId = _uiState.value.institutionIds.getOrNull(index) ?: return
        if (institutionId == _uiState.value.selectedInstitutionId) return
        userSessionStore?.setInstitutionId(institutionId)
        _uiState.update {
            it.copy(
                selectedInstitutionId = institutionId,
                selectedClass = "",
                subjects = emptyList(),
                isLoading = true,
                errorMessage = null,
            )
        }
        refresh()
    }

    fun refresh(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val hasContent = allSubjects.isNotEmpty() ||
                _uiState.value.subjects.isNotEmpty() ||
                _uiState.value.classOptions.isNotEmpty()
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
                val sessionInstitutions = userSessionStore?.getInstitutionIds().orEmpty()
                    .filter { it.isNotBlank() }
                    .distinct()
                    .map { id -> id to "Institution" }
                val knownIds = _uiState.value.institutionIds.ifEmpty { sessionInstitutions.map { it.first } }
                val selectedHint = _uiState.value.selectedInstitutionId
                    ?: userSessionStore?.getInstitutionId()?.takeIf { it in knownIds }
                    ?: knownIds.firstOrNull()
                val initialQuery = StudentUiMappers.queryInstitutionId(knownIds, selectedHint)
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
                val unitsDeferred = async { teacherRepository.teachingUnits(forceRefresh = bypassCache) }
                var statsResult = statsDeferred.await()
                var aiSubjectsResult = aiSubjectsDeferred.await()
                val unitsResult = unitsDeferred.await()

                val units = (unitsResult as? NetworkResult.Success)?.data.orEmpty()
                var aiSubjects = (aiSubjectsResult as? NetworkResult.Success)?.data.orEmpty()
                var fromAi = if (aiSubjectsResult is NetworkResult.Success) {
                    StudentUiMappers.aiSubjects(aiSubjects)
                } else {
                    emptyList()
                }
                val fromUnits = StudentUiMappers.teachingUnitSubjects(units)
                var institutions = StudentUiMappers.mergeInstitutionOptions(
                    TeacherUiMappers.institutionChips(units).map { it.id to it.name },
                    StudentUiMappers.institutionOptions(fromAi.ifEmpty { fromUnits }),
                    sessionInstitutions,
                )
                val selectedInstitutionId = _uiState.value.selectedInstitutionId
                    ?.takeIf { id -> institutions.any { it.first == id } }
                    ?: userSessionStore?.getInstitutionId()
                        ?.takeIf { id -> institutions.any { it.first == id } }
                    ?: institutions.firstOrNull()?.first
                val scopedQuery = StudentUiMappers.queryInstitutionId(
                    institutionIds = institutions.map { it.first },
                    selectedInstitutionId = selectedInstitutionId,
                )
                if (scopedQuery != null && scopedQuery != initialQuery) {
                    aiSubjectsResult = studentRepository.aiSubjects(
                        forceRefresh = bypassCache,
                        institutionId = scopedQuery,
                    )
                    aiSubjects = (aiSubjectsResult as? NetworkResult.Success)?.data.orEmpty()
                    fromAi = if (aiSubjectsResult is NetworkResult.Success) {
                        StudentUiMappers.aiSubjects(aiSubjects)
                    } else {
                        emptyList()
                    }
                    statsResult = studentRepository.progressOverview(
                        forceRefresh = bypassCache,
                        institutionId = scopedQuery,
                    )
                    institutions = StudentUiMappers.mergeInstitutionOptions(
                        institutions,
                        StudentUiMappers.institutionOptions(fromAi),
                    )
                }
                if (selectedInstitutionId != null) {
                    userSessionStore?.setInstitutionId(selectedInstitutionId)
                }
                val scopedUnits = TeacherUiMappers.unitsForInstitution(units, scopedQuery)
                allSubjects = when {
                    aiSubjectsResult is NetworkResult.Success -> fromAi
                    fromUnits.isNotEmpty() -> StudentUiMappers.teachingUnitSubjects(scopedUnits)
                    else -> emptyList()
                }.filter { it.name.isNotBlank() }
                    .distinctBy { "${it.institutionId}|${it.classId}|${it.id}" }
                val visibleInstitutions = institutions.takeIf { it.size > 1 }.orEmpty()
                val classes = allSubjects.map { it.className }.filter { it.isNotBlank() }.distinct()
                    .ifEmpty {
                        if (aiSubjectsResult is NetworkResult.Success) {
                            emptyList()
                        } else {
                            TeacherUiMappers.classChips(scopedUnits).map { it.label }
                        }
                    }
                val selectedClass = _uiState.value.selectedClass.takeIf { it in classes }
                    ?: classes.firstOrNull().orEmpty()
                val stats = when (statsResult) {
                    is NetworkResult.Success -> StudentUiMappers.aiHubStats(statsResult.data)
                    else -> StudentUiMappers.emptyAiHubStats()
                }
                val error = when {
                    aiSubjectsResult is NetworkResult.Success ||
                        unitsResult is NetworkResult.Success -> null
                    else -> unitsResult.userMessage().ifBlank { aiSubjectsResult.userMessage() }
                }
                val needsApproval = listOf(aiSubjectsResult, unitsResult, statsResult)
                    .any { it.needsAdminApproval() }
                applyFilter(
                    _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        stats = stats,
                        classOptions = classes,
                        selectedClass = selectedClass,
                        institutions = visibleInstitutions.map { it.second },
                        institutionIds = visibleInstitutions.map { it.first },
                        selectedInstitutionId = selectedInstitutionId.takeIf {
                            visibleInstitutions.isNotEmpty()
                        },
                        needsApproval = needsApproval,
                        errorMessage = if (needsApproval) null else error,
                    ),
                )
                val firstSubjectId = allSubjects.firstOrNull()?.id
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

    private fun applyFilter(base: StudentAiHubUiState) {
        val selected = base.selectedClass
        val subjects = if (selected.isBlank()) {
            allSubjects
        } else {
            allSubjects.filter { it.className == selected }
        }
        _uiState.value = base.copy(subjects = subjects)
    }

    companion object {
        fun provideFactory(
            studentRepository: StudentRepository,
            teacherRepository: TeacherRepository,
            userSessionStore: UserSessionStore? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherAiHubViewModel(studentRepository, teacherRepository, userSessionStore)
        }
    }
}

class TeacherAiChatViewModel(
    mockRepository: TeacherMockRepository,
    subjectId: String,
) : ViewModel() {
    private val session = mockRepository.aiChatSession(subjectId)
    private val defaultLanguage = "English"

    private val _uiState = MutableStateFlow(buildState(defaultLanguage))
    val uiState: StateFlow<StudentAiChatUiState> = _uiState.asStateFlow()

    fun onDraftChange(value: String) {
        _uiState.update { it.copy(draft = value) }
    }

    fun sendDraft() {
        val state = _uiState.value
        val text = state.draft.trim()
        val pending = state.pendingAsk
        if (text.isEmpty() && pending == null) return
        appendPendingOrPlain(text, pending)
        _uiState.update { it.copy(draft = "", pendingAsk = null) }
    }

    fun sendSuggestion(text: String) {
        val pending = _uiState.value.pendingAsk
        appendPendingOrPlain(text, pending)
        _uiState.update { it.copy(pendingAsk = null) }
    }

    fun askAboutContent(item: AiMenuContentItem) {
        val tab = when {
            _uiState.value.menuTab == AiMenuTab.TextbookQuestions -> AiMenuTab.TextbookQuestions
            _uiState.value.menuTab == AiMenuTab.ExamPreparation -> AiMenuTab.ExamPreparation
            _uiState.value.textbookQuestions.any { it.id == item.id } -> AiMenuTab.TextbookQuestions
            _uiState.value.examPrepPyqs.any { it.id == item.id } -> AiMenuTab.ExamPreparation
            else -> AiMenuTab.TextbookQuestions
        }
        startPendingAsk(item, tab)
    }

    fun askAboutResource(item: AiMenuContentItem) {
        startPendingAsk(item, AiMenuTab.Resources)
    }

    fun clearPendingAsk() {
        _uiState.update { it.copy(pendingAsk = null) }
    }

    private fun startPendingAsk(item: AiMenuContentItem, tab: AiMenuTab) {
        closeMenu()
        _uiState.update {
            it.copy(
                menuTab = AiMenuTab.Chats,
                pendingAsk = PendingAsk(item = item, tab = tab),
                composerFocusNonce = it.composerFocusNonce + 1,
            )
        }
    }

    fun returnToQuestion(message: AiChatMessage) {
        val state = _uiState.value
        if (!message.fromUser || !message.isQuestionAskMessage()) return

        val link = message.linkedQuestionId?.takeIf { it.isNotBlank() }?.let { id ->
            val tab = message.linkedQuestionTab ?: when {
                state.examPrepPyqs.any { it.id == id } -> AiMenuTab.ExamPreparation
                state.textbookQuestions.any { it.id == id } -> AiMenuTab.TextbookQuestions
                else -> return
            }
            id to tab
        } ?: run {
            val title = message.text.substringAfter(":", "").trim().takeIf { it.isNotBlank() } ?: return
            val normalized = title.lowercase()
            state.textbookQuestions.firstOrNull { it.title.lowercase() == normalized }
                ?.let { it.id to AiMenuTab.TextbookQuestions }
                ?: state.examPrepPyqs.firstOrNull { it.title.lowercase() == normalized }
                    ?.let { it.id to AiMenuTab.ExamPreparation }
                ?: return
        }

        _uiState.update {
            it.copy(
                menuTab = link.second,
                scrollToQuestionId = link.first,
                scrollToQuestionNonce = it.scrollToQuestionNonce + 1,
            )
        }
    }

    fun returnToResource(message: AiChatMessage) {
        val id = message.linkedResourceId?.takeIf { it.isNotBlank() } ?: return
        _uiState.update {
            it.copy(
                menuTab = AiMenuTab.Resources,
                scrollToQuestionId = id,
                scrollToQuestionNonce = it.scrollToQuestionNonce + 1,
            )
        }
    }

    fun openSection(item: AiSyllabusItem) {
        toggleSyllabusSelection(item)
    }

    fun toggleSyllabusSelection(item: AiSyllabusItem) {
        _uiState.update { state ->
            val next = state.selectedSyllabusIds.toMutableSet()
            if (!next.add(item.id)) next.remove(item.id)
            state.copy(selectedSyllabusIds = next)
        }
    }

    fun selectAllSyllabus() {
        _uiState.update { it.copy(selectedSyllabusIds = emptySet()) }
    }

    fun clearChat() {
        _uiState.update {
            it.copy(
                messages = emptyList(),
                selectedQuickOption = null,
                showQuickCheck = false,
                pendingAsk = null,
            )
        }
    }

    fun openMenu() {
        _uiState.update { it.copy(showMenu = true) }
    }

    fun closeMenu() {
        _uiState.update { it.copy(showMenu = false) }
    }

    fun selectMenuTab(tab: AiMenuTab) {
        _uiState.update { it.copy(menuTab = tab) }
    }

    fun selectQuickOption(option: String) {
        _uiState.update { it.copy(selectedQuickOption = option) }
    }

    fun setLanguage(language: String) {
        if (language == _uiState.value.language) return
        _uiState.update { buildState(language).copy(draft = it.draft, showMenu = it.showMenu, menuTab = it.menuTab) }
    }

    private fun buildState(language: String): StudentAiChatUiState {
        val pack = session.packFor(language)
        return StudentAiChatUiState(
            chapterTitle = "Chapter: 1",
            messages = pack.messages,
            suggestions = pack.suggestions,
            quickCheck = pack.quickCheck,
            syllabus = session.syllabus,
            textbookQuestions = session.textbookQuestions,
            examPrepPyqs = session.examPrepPyqs,
            resources = session.resources,
            quizHistory = session.quizHistory,
            language = language,
            selectedQuickOption = null,
            showQuickCheck = true,
        )
    }

    private fun appendPendingOrPlain(text: String, pending: PendingAsk?) {
        if (pending == null) {
            appendUserMessage(text)
            return
        }
        val item = pending.item
        if (pending.tab == AiMenuTab.Resources) {
            appendUserMessage(
                text = text,
                linkedResourceId = item.id.takeIf { it.isNotBlank() },
                linkedResourceTitle = item.title.takeIf { it.isNotBlank() },
            )
        } else {
            appendUserMessage(
                text = text,
                linkedQuestionId = item.id.takeIf { it.isNotBlank() },
                linkedQuestionTab = pending.tab,
                linkedQuestionTitle = item.title.takeIf { it.isNotBlank() },
            )
        }
    }

    private fun appendUserMessage(
        text: String,
        linkedQuestionId: String? = null,
        linkedQuestionTab: AiMenuTab? = null,
        linkedQuestionTitle: String? = null,
        linkedResourceId: String? = null,
        linkedResourceTitle: String? = null,
    ) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages + AiChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = text,
                    fromUser = true,
                    linkedQuestionId = linkedQuestionId,
                    linkedQuestionTab = linkedQuestionTab,
                    linkedQuestionTitle = linkedQuestionTitle,
                    linkedResourceId = linkedResourceId,
                    linkedResourceTitle = linkedResourceTitle,
                ),
            )
        }
    }

    companion object {
        fun provideFactory(
            mockRepository: TeacherMockRepository,
            subjectId: String,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherAiChatViewModel(mockRepository, subjectId)
        }
    }
}

@Composable
fun TeacherAiHubRoute(
    studentRepository: StudentRepository,
    teacherRepository: TeacherRepository,
    onSubjectClick: (AiSubjectItem) -> Unit,
    modifier: Modifier = Modifier,
    userSessionStore: UserSessionStore? = null,
    viewModel: TeacherAiHubViewModel = viewModel(
        factory = TeacherAiHubViewModel.provideFactory(
            studentRepository,
            teacherRepository,
            userSessionStore,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StudentAiHubScreen(
        uiState = uiState,
        onSubjectClick = onSubjectClick,
        onClassSelected = viewModel::onClassSelected,
        onInstitutionSelected = viewModel::selectInstitution,
        onRefresh = { viewModel.refresh(forceRefresh = true) },
        modifier = modifier,
    )
}

@Composable
fun TeacherAiChatRoute(
    subjectId: String,
    mockRepository: TeacherMockRepository,
    onBack: () -> Unit,
    onTakeQuiz: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TeacherAiChatViewModel = viewModel(
        factory = TeacherAiChatViewModel.provideFactory(mockRepository, subjectId),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StudentAiChatScreen(
        uiState = uiState,
        onBack = onBack,
        onClearChat = viewModel::clearChat,
        onOpenMenu = viewModel::openMenu,
        onCloseMenu = viewModel::closeMenu,
        onMenuTabSelected = viewModel::selectMenuTab,
        onDraftChange = viewModel::onDraftChange,
        onSend = viewModel::sendDraft,
        onSuggestion = viewModel::sendSuggestion,
        onQuickOption = viewModel::selectQuickOption,
        onLanguageSelected = viewModel::setLanguage,
        onTakeQuiz = onTakeQuiz,
        onAskAboutContent = viewModel::askAboutContent,
        onAskAboutResource = viewModel::askAboutResource,
        onClearPendingAsk = viewModel::clearPendingAsk,
        onBackToQuestion = viewModel::returnToQuestion,
        onBackToResource = viewModel::returnToResource,
        onToggleSyllabus = viewModel::toggleSyllabusSelection,
        onSelectAllSyllabus = viewModel::selectAllSyllabus,
        modifier = modifier,
    )
}
