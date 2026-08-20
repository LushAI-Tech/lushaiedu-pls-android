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
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.needsAdminApproval
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.viewModelFactory
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

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val statsDeferred = async { studentRepository.progressDashboard() }
                val aiSubjectsDeferred = async { studentRepository.aiSubjects() }
                val unitsDeferred = async { teacherRepository.teachingUnits() }
                val statsResult = statsDeferred.await()
                val aiSubjectsResult = aiSubjectsDeferred.await()
                val unitsResult = unitsDeferred.await()

                val stats = when (statsResult) {
                    is NetworkResult.Success -> StudentUiMappers.aiHubStats(statsResult.data)
                    else -> StudentUiMappers.emptyAiHubStats()
                }
                val aiSubjects = (aiSubjectsResult as? NetworkResult.Success)?.data.orEmpty()
                val units = (unitsResult as? NetworkResult.Success)?.data.orEmpty()
                val fromAi = StudentUiMappers.aiSubjects(aiSubjects)
                val fromUnits = StudentUiMappers.teachingUnitSubjects(units)
                allSubjects = (fromAi + fromUnits)
                    .filter { it.name.isNotBlank() }
                    .distinctBy { "${it.className}|${it.id}" }
                val classes = TeacherUiMappers.classChips(units)
                    .map { it.label }
                    .ifEmpty {
                        allSubjects.map { it.className }.filter { it.isNotBlank() }.distinct()
                    }
                val selected = _uiState.value.selectedClass.takeIf { it in classes }
                    ?: classes.firstOrNull().orEmpty()
                val error = when {
                    allSubjects.isNotEmpty() -> null
                    aiSubjectsResult !is NetworkResult.Success &&
                        unitsResult !is NetworkResult.Success -> {
                        unitsResult.userMessage().ifBlank { aiSubjectsResult.userMessage() }
                    }
                    else -> null
                }
                val needsApproval = listOf(aiSubjectsResult, unitsResult, statsResult)
                    .any { it.needsAdminApproval() }
                applyFilter(
                    _uiState.value.copy(
                        isLoading = false,
                        stats = stats,
                        classOptions = classes,
                        selectedClass = selected,
                        needsApproval = needsApproval,
                        errorMessage = if (needsApproval) null else error,
                    ),
                )
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
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherAiHubViewModel(studentRepository, teacherRepository)
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
        val text = _uiState.value.draft.trim()
        if (text.isEmpty()) return
        appendUserMessage(text)
        _uiState.update { it.copy(draft = "") }
    }

    fun sendSuggestion(text: String) {
        appendUserMessage(text)
    }

    fun askAboutContent(item: AiMenuContentItem) {
        closeMenu()
        val prompt = if (item.title.isNotBlank()) {
            "Ask tutor about this: ${item.title}"
        } else {
            "Ask tutor about this"
        }
        appendUserMessage(prompt)
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

    fun clearChat() {
        _uiState.update {
            it.copy(
                messages = emptyList(),
                selectedQuickOption = null,
                showQuickCheck = false,
            )
        }
    }

    fun openMenu() {
        _uiState.update { it.copy(showMenu = true, menuTab = AiMenuTab.Chats) }
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
            resources = session.resources,
            quizHistory = session.quizHistory,
            language = language,
            selectedQuickOption = null,
            showQuickCheck = true,
        )
    }

    private fun appendUserMessage(text: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages + AiChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = text,
                    fromUser = true,
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
    viewModel: TeacherAiHubViewModel = viewModel(
        factory = TeacherAiHubViewModel.provideFactory(studentRepository, teacherRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StudentAiHubScreen(
        uiState = uiState,
        onSubjectClick = onSubjectClick,
        onClassSelected = viewModel::onClassSelected,
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
        onToggleSyllabus = viewModel::toggleSyllabusSelection,
        modifier = modifier,
    )
}
