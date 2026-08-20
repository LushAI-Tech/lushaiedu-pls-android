package com.lushaiedupls.ui.student.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.mock.AiChatMessage
import com.lushaiedupls.data.mock.AiMenuContentItem
import com.lushaiedupls.data.mock.AiMenuTab
import com.lushaiedupls.data.mock.AiSyllabusItem
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.SectionOut
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.common.viewModelFactory
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class StudentAiChatViewModel(
    private val studentRepository: StudentRepository,
    private val subjectId: String,
    private val chapterIdHint: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentAiChatUiState(isLoading = true))
    val uiState: StateFlow<StudentAiChatUiState> = _uiState.asStateFlow()

    init {
        bootstrap()
    }

    fun onDraftChange(value: String) {
        _uiState.update { it.copy(draft = value) }
    }

    fun sendDraft() {
        val text = _uiState.value.draft.trim()
        if (text.isEmpty()) return
        _uiState.update { it.copy(draft = "") }
        sendMessage(text)
    }

    fun sendSuggestion(text: String) {
        sendMessage(text)
    }

    fun askAboutContent(item: AiMenuContentItem) {
        closeMenu()
        _uiState.update { it.copy(menuTab = AiMenuTab.Chats) }
        val prompt = if (item.title.isNotBlank()) {
            "Ask tutor about this question: ${item.title}"
        } else {
            "Ask tutor about this question"
        }
        sendMessage(
            text = prompt,
            sectionId = item.sectionId.takeIf { it.isNotBlank() },
            contentBlockId = item.id.takeIf { it.isNotBlank() },
        )
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

    fun selectAllSyllabus(selectAll: Boolean) {
        _uiState.update { state ->
            state.copy(
                selectedSyllabusIds = if (selectAll) state.syllabus.map { it.id }.toSet() else emptySet(),
            )
        }
    }

    fun selectedSectionIds(): List<String> {
        val selected = _uiState.value.selectedSyllabusIds
        return _uiState.value.syllabus.map { it.id }.filter { it in selected }
    }

    fun clearChat() {
        val chapterId = _uiState.value.chapterId
        if (chapterId.isBlank()) return
        viewModelScope.launch {
            when (val result = studentRepository.clearChat(chapterId)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            messages = emptyList(),
                            selectedQuickOption = null,
                            quickCheckAnswered = false,
                            quickCheckCorrect = null,
                            quickCheckExplanation = null,
                            showQuickCheck = false,
                            suggestions = emptyList(),
                            quickCheck = null,
                        )
                    }
                    loadIntro(chapterId, _uiState.value.language)
                }
                else -> _uiState.update { it.copy(errorMessage = result.userMessage()) }
            }
        }
    }

    fun openMenu() {
        _uiState.update { it.copy(showMenu = true) }
        loadQuizHistory()
    }

    fun closeMenu() {
        _uiState.update { it.copy(showMenu = false) }
    }

    fun selectMenuTab(tab: AiMenuTab) {
        _uiState.update { it.copy(menuTab = tab, showMenu = false) }
        if (tab == AiMenuTab.ExamPreparation) {
            loadQuizHistory()
        }
    }

    fun selectQuickOption(option: String) {
        val check = _uiState.value.quickCheck
        if (check == null) {
            _uiState.update { it.copy(selectedQuickOption = option) }
            return
        }
        if (_uiState.value.quickCheckAnswered) return
        val selectedIndex = check.options.indexOf(option)
        val isCorrect = check.correctIndex?.let { it == selectedIndex }
        _uiState.update {
            it.copy(
                selectedQuickOption = option,
                quickCheckAnswered = true,
                quickCheckCorrect = isCorrect,
                quickCheckExplanation = check.explanation,
            )
        }
    }

    fun setLanguage(language: String) {
        if (language == _uiState.value.language) return
        val chapterId = _uiState.value.chapterId
        _uiState.update { it.copy(language = language) }
        if (chapterId.isNotBlank()) {
            reloadConversation(chapterId, language)
        }
    }

    fun dismissQuickCheck() {
        _uiState.update { it.copy(showQuickCheck = false) }
    }

    private fun bootstrap() {
        val hint = chapterIdHint?.takeIf { it.isNotBlank() }
        if (hint != null) {
            _uiState.update { it.copy(chapterId = hint, isLoading = true, errorMessage = null) }
            loadSyllabus(hint)
            reloadConversation(hint, _uiState.value.language)

            // Resolve chapter title in background without blocking chat UI
            viewModelScope.launch {
                val chaptersResult = studentRepository.chapters(subjectId)
                if (chaptersResult is NetworkResult.Success) {
                    val chapter = chaptersResult.data.find { it.id == hint }
                    if (chapter != null) {
                        _uiState.update { it.copy(chapterTitle = chapterHeading(chapter.chapter_number)) }
                    }
                }
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            supervisorScope {
                val resumeDeferred = async { studentRepository.progressResume() }
                val chaptersDeferred = async { studentRepository.chapters(subjectId) }

                val resume = resumeDeferred.await()
                val chaptersResult = chaptersDeferred.await()
                val activeChapters = (chaptersResult as? NetworkResult.Success)?.data
                    ?.filter { it.is_active }
                    .orEmpty()
                val chapterId = when {
                    resume is NetworkResult.Success &&
                        activeChapters.any { it.id == resume.data?.chapter_id } ->
                        resume.data?.chapter_id
                    activeChapters.isNotEmpty() ->
                        activeChapters.minByOrNull { it.chapter_number }?.id
                    else -> null
                }
                val chapterTitle = when {
                    chaptersResult is NetworkResult.Success ->
                        chaptersResult.data.find { it.id == chapterId }
                            ?.let { chapterHeading(it.chapter_number) }
                    resume is NetworkResult.Success &&
                        resume.data?.chapter_id == chapterId ->
                        chapterHeading(resume.data?.chapter_number)
                    else -> null
                }
                if (chapterId.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (chaptersResult !is NetworkResult.Success) {
                                chaptersResult.userMessage()
                            } else {
                                "No chapters available for this subject."
                            },
                        )
                    }
                    return@supervisorScope
                }
                _uiState.update {
                    it.copy(chapterId = chapterId, chapterTitle = chapterTitle.orEmpty())
                }
                loadSyllabus(chapterId)
                reloadConversation(chapterId, _uiState.value.language)
            }
        }
    }

    private fun reloadConversation(chapterId: String, language: String) {
        viewModelScope.launch {
            when (val history = studentRepository.chatHistory(chapterId)) {
                is NetworkResult.Success -> {
                    val messages = StudentUiMappers.chatMessages(history.data.messages)
                    if (messages.isEmpty()) {
                        loadIntro(chapterId, language)
                    } else {
                        val lastAssistant = history.data.messages.lastOrNull {
                            !it.role.equals("user", true) && !it.role.equals("student", true)
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                messages = messages,
                                suggestions = lastAssistant?.suggestions.orEmpty(),
                                quickCheck = StudentUiMappers.quickCheck(lastAssistant?.concept_check),
                                selectedQuickOption = null,
                                quickCheckAnswered = false,
                                quickCheckCorrect = null,
                                quickCheckExplanation = null,
                                showQuickCheck = lastAssistant?.concept_check != null,
                            )
                        }
                    }
                }
                else -> {
                    loadIntro(chapterId, language)
                }
            }
        }
    }

    private suspend fun loadIntro(chapterId: String, language: String) {
        when (
            val intro = studentRepository.chatIntro(
                chapterId,
                StudentUiMappers.apiLanguage(language),
            )
        ) {
            is NetworkResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        messages = listOf(StudentUiMappers.chatResponseMessage(intro.data)),
                        suggestions = intro.data.suggestions,
                        quickCheck = StudentUiMappers.quickCheck(intro.data.concept_check),
                        selectedQuickOption = null,
                        quickCheckAnswered = false,
                        quickCheckCorrect = null,
                        quickCheckExplanation = null,
                        showQuickCheck = intro.data.concept_check != null,
                    )
                }
            }
            else -> _uiState.update {
                it.copy(isLoading = false, errorMessage = intro.userMessage())
            }
        }
    }

    private fun loadSyllabus(chapterId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMenuContentLoading = true) }

            // 1. Fetch Resources (Attach files) in parallel - instant
            launch {
                when (val attResult = studentRepository.chapterAttachments(chapterId)) {
                    is NetworkResult.Success -> {
                        val attItems = StudentUiMappers.attachments(attResult.data)
                        _uiState.update { it.copy(resources = attItems) }
                    }
                    else -> _uiState.update { it.copy(resources = emptyList()) }
                }
            }

            // 2. Fetch Exam Prep (PYQs) in parallel - instant
            launch {
                val pyqResult = studentRepository.examPrepPyqs(chapterId = chapterId)
                if (pyqResult is NetworkResult.Success && pyqResult.data.hits.isNotEmpty()) {
                    val pyqItems = StudentUiMappers.examPrepPyqs(pyqResult.data.hits)
                    _uiState.update { it.copy(examPrepPyqs = pyqItems) }
                } else {
                    val scopedPyqResult = studentRepository.examPrepPyqs(chapterId = chapterId, chapterScope = true)
                    if (scopedPyqResult is NetworkResult.Success && scopedPyqResult.data.hits.isNotEmpty()) {
                        val pyqItems = StudentUiMappers.examPrepPyqs(scopedPyqResult.data.hits)
                        _uiState.update { it.copy(examPrepPyqs = pyqItems) }
                    }
                }
            }

            // 3. Fetch Practice Questions in parallel
            if (subjectId.isNotBlank()) {
                launch {
                    when (val qResult = studentRepository.questionsList(subjectId)) {
                        is NetworkResult.Success -> {
                            val practiceQuestions = StudentUiMappers.textbookQuestionsFromPracticeSets(
                                qResult.data.sets,
                                chapterId,
                            )
                            if (practiceQuestions.isNotEmpty()) {
                                _uiState.update { state ->
                                    val combined = (practiceQuestions + state.textbookQuestions).distinctBy { it.id }
                                    state.copy(textbookQuestions = combined)
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            }

            // 4. Fetch Chapter outline & section content blocks in parallel
            launch {
                when (val result = studentRepository.chapter(chapterId)) {
                    is NetworkResult.Success -> {
                        val sections = result.data.sections
                        _uiState.update {
                            it.copy(
                                chapterTitle = it.chapterTitle.ifBlank {
                                    chapterHeading(result.data.chapter_number)
                                },
                                syllabus = StudentUiMappers.syllabus(sections),
                            )
                        }
                        val filled = fillSectionBlocks(sections)
                        val sectionQuestions = StudentUiMappers.textbookQuestions(filled)
                        if (sectionQuestions.isNotEmpty()) {
                            _uiState.update { state ->
                                val combined = (state.textbookQuestions + sectionQuestions).distinctBy { it.id }
                                state.copy(textbookQuestions = combined)
                            }
                        }
                    }
                    else -> Unit
                }
            }

            // 5. Quiz history
            launch {
                loadQuizHistory()
            }

            _uiState.update { it.copy(isMenuContentLoading = false) }
        }
    }

    private suspend fun fillSectionBlocks(sections: List<SectionOut>): List<SectionOut> {
        val flat = StudentUiMappers.flattenSections(sections)
        return supervisorScope {
            flat.map { section ->
                async {
                    if (section.content_blocks.isNotEmpty()) {
                        section.copy(subsections = emptyList())
                    } else {
                        when (val result = studentRepository.section(section.id)) {
                            is NetworkResult.Success -> result.data.copy(subsections = emptyList())
                            else -> section.copy(subsections = emptyList())
                        }
                    }
                }
            }.awaitAll()
        }
    }

    private fun loadQuizHistory() {
        viewModelScope.launch {
            when (val result = studentRepository.quizHistory()) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(quizHistory = StudentUiMappers.quizHistory(result.data))
                }
                else -> Unit
            }
        }
    }

    private fun sendMessage(
        text: String,
        sectionId: String? = null,
        contentBlockId: String? = null,
    ) {
        val chapterId = _uiState.value.chapterId
        if (chapterId.isBlank()) return
        viewModelScope.launch {
            val userMsg = AiChatMessage(
                id = UUID.randomUUID().toString(),
                text = text,
                fromUser = true,
            )
            _uiState.update {
                it.copy(
                    messages = it.messages + userMsg,
                    isSending = true,
                    errorMessage = null,
                )
            }
            when (
                val result = studentRepository.chat(
                    chapterId = chapterId,
                    message = text,
                    language = StudentUiMappers.apiLanguage(_uiState.value.language),
                    sectionId = sectionId,
                    contentBlockId = contentBlockId,
                )
            ) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            messages = it.messages + StudentUiMappers.chatResponseMessage(result.data),
                            suggestions = result.data.suggestions,
                            quickCheck = StudentUiMappers.quickCheck(result.data.concept_check),
                            selectedQuickOption = null,
                            quickCheckAnswered = false,
                            quickCheckCorrect = null,
                            quickCheckExplanation = null,
                            showQuickCheck = result.data.concept_check != null,
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(isSending = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    private fun chapterHeading(number: Int?): String =
        if (number != null) "Chapter: $number" else "Chapter"

    companion object {
        fun provideFactory(
            studentRepository: StudentRepository,
            subjectId: String,
            chapterIdHint: String? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            StudentAiChatViewModel(studentRepository, subjectId, chapterIdHint)
        }
    }
}
