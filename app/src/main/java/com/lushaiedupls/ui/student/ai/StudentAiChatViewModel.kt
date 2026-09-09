package com.lushaiedupls.ui.student.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.mock.AiChatMessage
import com.lushaiedupls.data.mock.AiMenuContentItem
import com.lushaiedupls.data.mock.AiMenuTab
import com.lushaiedupls.data.mock.AiSyllabusItem
import com.lushaiedupls.data.mock.isQuestionAskMessage
import com.lushaiedupls.data.remote.AiQueryParams
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.aiScopeUserMessage
import com.lushaiedupls.data.remote.dto.SectionOut
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.common.viewModelFactory
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    private val cachedHint = chapterIdHint?.takeIf { it.isNotBlank() }
    private val initialChapter = cachedHint?.let { studentRepository.getCachedChapter(it) }
    private val initialHistory = cachedHint?.let { studentRepository.getCachedChatHistory(it) }
    private val initialIntro = cachedHint?.let { studentRepository.getCachedChatIntro(it, "en") }
    private val initialMessages = when {
        initialHistory != null && initialHistory.messages.isNotEmpty() ->
            StudentUiMappers.chatMessages(initialHistory.messages)
        initialIntro != null ->
            listOf(StudentUiMappers.chatResponseMessage(initialIntro))
        else -> emptyList()
    }
    private val initialSuggestions = when {
        initialHistory != null && initialHistory.messages.isNotEmpty() ->
            initialHistory.messages.lastOrNull { !it.role.equals("user", true) }?.suggestions.orEmpty()
        initialIntro != null -> initialIntro.suggestions
        else -> emptyList()
    }
    private val initialQuickCheck = when {
        initialHistory != null && initialHistory.messages.isNotEmpty() ->
            initialHistory.messages.lastOrNull { !it.role.equals("user", true) }?.concept_check?.let(StudentUiMappers::quickCheck)
        initialIntro != null -> initialIntro.concept_check?.let(StudentUiMappers::quickCheck)
        else -> null
    }
    private val initialSyllabus = initialChapter?.sections?.let(StudentUiMappers::syllabus).orEmpty()
    private val initialAtt = cachedHint?.let { studentRepository.getCachedAttachments(it) }
        ?.let(StudentUiMappers::attachments).orEmpty()
    private val initialPyqs = cachedHint?.let {
        studentRepository.getCachedExamPrepPyqs(it, chapterScope = true)
    }?.hits?.let(StudentUiMappers::examPrepPyqs).orEmpty()

    private val _uiState = MutableStateFlow(
        StudentAiChatUiState(
            chapterId = cachedHint.orEmpty(),
            chapterTitle = initialChapter?.chapter_number?.let(::chapterHeading).orEmpty(),
            isLoading = initialMessages.isEmpty(),
            isMenuContentLoading = initialPyqs.isEmpty() || initialAtt.isEmpty(),
            messages = initialMessages,
            suggestions = initialSuggestions,
            quickCheck = initialQuickCheck,
            showQuickCheck = initialQuickCheck != null,
            syllabus = initialSyllabus,
            resources = initialAtt,
            examPrepPyqs = initialPyqs,
        ),
    )
    val uiState: StateFlow<StudentAiChatUiState> = _uiState.asStateFlow()

    private var lastQuestionsKey: String? = null
    private var lastPyqsKey: String? = null
    private var lastResourcesKey: String? = null

    init {
        bootstrap()
    }

    fun onDraftChange(value: String) {
        _uiState.update { it.copy(draft = value) }
    }

    fun sendDraft() {
        val state = _uiState.value
        val text = state.draft.trim()
        val pending = state.pendingAsk
        if (text.isEmpty() && pending == null) return
        _uiState.update { it.copy(draft = "", pendingAsk = null) }
        sendPendingOrPlain(text, pending)
    }

    fun sendSuggestion(text: String) {
        val pending = _uiState.value.pendingAsk
        _uiState.update { it.copy(pendingAsk = null) }
        sendPendingOrPlain(text, pending)
    }

    fun askAboutContent(item: AiMenuContentItem) {
        val tab = questionSourceTab(_uiState.value, item)
            ?: _uiState.value.menuTab.takeIf {
                it == AiMenuTab.TextbookQuestions || it == AiMenuTab.ExamPreparation
            }
            ?: AiMenuTab.TextbookQuestions
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
        val link = resolveQuestionLink(message, _uiState.value) ?: return
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
        reloadScopedContentIfNeeded()
    }

    fun selectAllSyllabus() {
        _uiState.update { it.copy(selectedSyllabusIds = emptySet()) }
        reloadScopedContentIfNeeded()
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
                            pendingAsk = null,
                            scrollToQuestionId = null,
                            scrollToQuestionNonce = 0,
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
        _uiState.update { it.copy(menuTab = tab) }
        val chId = _uiState.value.chapterId
        if (chId.isBlank()) return
        when (tab) {
            AiMenuTab.ExamPreparation -> {
                loadQuizHistory()
                loadExamPrepPyqs(chId)
            }
            AiMenuTab.TextbookQuestions -> loadTextbookQuestions(chId)
            AiMenuTab.Resources -> loadResources(chId)
            AiMenuTab.Chats -> Unit
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
            _uiState.update { it.copy(chapterId = hint, isLoading = it.messages.isEmpty(), errorMessage = null) }
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
            if (_uiState.value.messages.isEmpty()) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
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

    fun loadExamPrepPyqs(chapterId: String) {
        val query = examPrepQuery()
        val key = pyqsRequestKey(chapterId, query)
        if (lastPyqsKey == key) return
        lastPyqsKey = key
        viewModelScope.launch {
            _uiState.update { it.copy(isMenuContentLoading = true, errorMessage = null) }
            val pyqResult = studentRepository.examPrepPyqs(
                chapterId = chapterId,
                sectionId = query.sectionId,
                chapterScope = query.chapterScope,
                examCodes = query.examCodes,
                subtopicIds = query.subtopicIds,
            )
            if (lastPyqsKey != key) return@launch
            when (pyqResult) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            examPrepPyqs = StudentUiMappers.examPrepPyqs(pyqResult.data.hits),
                            isMenuContentLoading = false,
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(examPrepPyqs = emptyList()) }
                    applyScopeError(pyqResult)
                }
            }
        }
    }

    fun loadResources(chapterId: String) {
        val subtopicIds = scopedSubtopicIdsQuery()
        val key = "${chapterId}_$subtopicIds"
        if (lastResourcesKey == key) return
        lastResourcesKey = key
        viewModelScope.launch {
            _uiState.update { it.copy(isMenuContentLoading = true, errorMessage = null) }
            when (val attResult = studentRepository.chapterAttachments(chapterId, subtopicIds = subtopicIds)) {
                is NetworkResult.Success -> {
                    if (lastResourcesKey != key) return@launch
                    val attItems = StudentUiMappers.attachments(attResult.data)
                    val selected = selectedSectionIds().toSet()
                    val scoped = if (subtopicIds == null) {
                        attItems
                    } else {
                        attItems.filter { it.sectionId.isBlank() || it.sectionId in selected }
                    }
                    _uiState.update { it.copy(resources = scoped, isMenuContentLoading = false) }
                }
                else -> {
                    if (lastResourcesKey != key) return@launch
                    _uiState.update { it.copy(resources = emptyList()) }
                    applyScopeError(attResult)
                }
            }
        }
    }

    fun loadTextbookQuestions(chapterId: String) {
        val subtopicIds = scopedSubtopicIdsQuery()
        val key = "${subjectId}_${chapterId}_$subtopicIds"
        if (lastQuestionsKey == key) return
        lastQuestionsKey = key
        viewModelScope.launch {
            _uiState.update {
                it.copy(isMenuContentLoading = true, errorMessage = null, textbookQuestions = emptyList())
            }
            val selectedIds = selectedSectionIds().toSet()
            var loadError: NetworkResult<*>? = null
            if (subjectId.isNotBlank()) {
                when (
                    val qResult = studentRepository.questionsList(
                        subjectId = subjectId,
                        chapterId = AiQueryParams.nonEmpty(chapterId),
                        subtopicIds = subtopicIds,
                    )
                ) {
                    is NetworkResult.Success -> {
                        val practiceQuestions = StudentUiMappers.textbookQuestionsFromPracticeSets(
                            qResult.data.sets,
                            chapterId,
                            selectedIds.takeIf { subtopicIds != null },
                        )
                        if (practiceQuestions.isNotEmpty()) {
                            _uiState.update { it.copy(textbookQuestions = practiceQuestions) }
                        }
                    }
                    else -> loadError = qResult
                }
            }
            when (val result = studentRepository.chapter(chapterId)) {
                is NetworkResult.Success -> {
                    val filled = fillSectionBlocks(result.data.sections)
                    val scoped = if (subtopicIds == null) filled else filled.filter { it.id in selectedIds }
                    val sectionQuestions = StudentUiMappers.textbookQuestions(scoped)
                    if (sectionQuestions.isNotEmpty()) {
                        _uiState.update { state ->
                            state.copy(
                                textbookQuestions = (state.textbookQuestions + sectionQuestions).distinctBy { it.id },
                            )
                        }
                    }
                }
                else -> if (loadError == null) loadError = result
            }
            if (lastQuestionsKey != key) return@launch
            if (loadError != null && _uiState.value.textbookQuestions.isEmpty()) {
                applyScopeError(loadError)
            } else {
                _uiState.update { it.copy(isMenuContentLoading = false) }
            }
        }
    }

    private fun loadSyllabus(chapterId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMenuContentLoading = true) }

            coroutineScope {
                val subtopicIds = scopedSubtopicIdsQuery()
                val examQuery = examPrepQuery()
                val questionsKey = "${subjectId}_${chapterId}_$subtopicIds"
                val resourcesKey = "${chapterId}_$subtopicIds"
                val pyqsKey = pyqsRequestKey(chapterId, examQuery)
                lastResourcesKey = resourcesKey
                lastPyqsKey = pyqsKey
                lastQuestionsKey = questionsKey

                // 1. Fetch Resources (Attach files) in parallel
                launch {
                    when (
                        val attResult = studentRepository.chapterAttachments(
                            chapterId,
                            subtopicIds = subtopicIds,
                        )
                    ) {
                        is NetworkResult.Success -> {
                            if (lastResourcesKey != resourcesKey) return@launch
                            val attItems = StudentUiMappers.attachments(attResult.data)
                            _uiState.update { it.copy(resources = attItems) }
                        }
                        else -> if (lastResourcesKey == resourcesKey) {
                            _uiState.update { it.copy(resources = emptyList()) }
                        }
                    }
                }

                // 2. Fetch Exam Prep (PYQs) in parallel
                launch {
                    val pyqResult = studentRepository.examPrepPyqs(
                        chapterId = chapterId,
                        sectionId = examQuery.sectionId,
                        chapterScope = examQuery.chapterScope,
                        examCodes = examQuery.examCodes,
                        subtopicIds = examQuery.subtopicIds,
                    )
                    if (lastPyqsKey != pyqsKey) return@launch
                    if (pyqResult is NetworkResult.Success) {
                        _uiState.update {
                            it.copy(examPrepPyqs = StudentUiMappers.examPrepPyqs(pyqResult.data.hits))
                        }
                    }
                }

                // 3. Fetch Practice Questions in parallel
                if (subjectId.isNotBlank()) {
                    launch {
                        when (
                            val qResult = studentRepository.questionsList(
                                subjectId = subjectId,
                                chapterId = AiQueryParams.nonEmpty(chapterId),
                                subtopicIds = subtopicIds,
                            )
                        ) {
                            is NetworkResult.Success -> {
                                if (lastQuestionsKey != questionsKey) return@launch
                                val practiceQuestions = StudentUiMappers.textbookQuestionsFromPracticeSets(
                                    qResult.data.sets,
                                    chapterId,
                                    selectedSectionIds().takeIf { subtopicIds != null },
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
                            if (lastQuestionsKey != questionsKey) return@launch
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

    private fun sendPendingOrPlain(text: String, pending: PendingAsk?) {
        if (pending == null) {
            sendMessage(text)
            return
        }
        val item = pending.item
        if (pending.tab == AiMenuTab.Resources) {
            sendMessage(
                text = text,
                sectionId = item.sectionId.takeIf { it.isNotBlank() },
                contentBlockId = item.id.takeIf { it.isNotBlank() },
                linkedResourceId = item.id.takeIf { it.isNotBlank() },
                linkedResourceTitle = item.title.takeIf { it.isNotBlank() },
            )
        } else {
            sendMessage(
                text = text,
                sectionId = item.sectionId.takeIf { it.isNotBlank() },
                contentBlockId = item.id.takeIf { it.isNotBlank() },
                linkedQuestionTab = pending.tab,
                linkedQuestionTitle = item.title.takeIf { it.isNotBlank() },
            )
        }
    }

    private fun sendMessage(
        text: String,
        sectionId: String? = null,
        contentBlockId: String? = null,
        linkedQuestionTab: AiMenuTab? = null,
        linkedQuestionTitle: String? = null,
        linkedResourceId: String? = null,
        linkedResourceTitle: String? = null,
    ) {
        val chapterId = _uiState.value.chapterId
        if (chapterId.isBlank()) return
        viewModelScope.launch {
            val userMsg = AiChatMessage(
                id = UUID.randomUUID().toString(),
                text = text,
                fromUser = true,
                linkedQuestionId = contentBlockId
                    ?.takeIf { it.isNotBlank() && linkedResourceId.isNullOrBlank() },
                linkedQuestionTab = linkedQuestionTab,
                linkedQuestionTitle = linkedQuestionTitle,
                linkedResourceId = linkedResourceId,
                linkedResourceTitle = linkedResourceTitle,
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

    private fun reloadScopedContentIfNeeded() {
        val chapterId = _uiState.value.chapterId
        if (chapterId.isBlank()) return
        when (_uiState.value.menuTab) {
            AiMenuTab.TextbookQuestions -> loadTextbookQuestions(chapterId)
            AiMenuTab.ExamPreparation -> loadExamPrepPyqs(chapterId)
            AiMenuTab.Resources -> loadResources(chapterId)
            AiMenuTab.Chats -> Unit
        }
    }

    private fun allSubtopicIds(): List<String> = _uiState.value.syllabus.map { it.id }

    private fun scopedSubtopicIdsQuery(): String? =
        AiQueryParams.subtopicIdsQuery(selectedSectionIds(), allSubtopicIds())

    private fun examPrepQuery(): AiQueryParams.ExamPrepQuery =
        AiQueryParams.examPrepQuery(
            selectedSubtopicIds = selectedSectionIds(),
            allSubtopicIds = allSubtopicIds(),
        )

    private fun pyqsRequestKey(chapterId: String, query: AiQueryParams.ExamPrepQuery): String =
        "${chapterId}_${query.chapterScope}_${query.subtopicIds}_${query.examCodes}_${query.sectionId}"

    private fun applyScopeError(result: NetworkResult<*>) {
        _uiState.update {
            it.copy(
                isMenuContentLoading = false,
                errorMessage = result.aiScopeUserMessage(),
            )
        }
    }

    private fun chapterHeading(number: Int?): String =
        if (number != null) "Chapter: $number" else "Chapter"

    private fun questionSourceTab(
        state: StudentAiChatUiState,
        item: AiMenuContentItem,
    ): AiMenuTab? = when {
        state.menuTab == AiMenuTab.TextbookQuestions -> AiMenuTab.TextbookQuestions
        state.menuTab == AiMenuTab.ExamPreparation -> AiMenuTab.ExamPreparation
        state.textbookQuestions.any { it.id == item.id } -> AiMenuTab.TextbookQuestions
        state.examPrepPyqs.any { it.id == item.id } -> AiMenuTab.ExamPreparation
        else -> null
    }

    private fun resolveQuestionLink(
        message: AiChatMessage,
        state: StudentAiChatUiState,
    ): Pair<String, AiMenuTab>? {
        if (!message.fromUser || !message.isQuestionAskMessage()) return null

        message.linkedQuestionId?.takeIf { it.isNotBlank() }?.let { id ->
            val tab = message.linkedQuestionTab ?: tabForQuestionId(state, id) ?: return null
            return id to tab
        }

        val title = message.text
            .substringAfter(":", "")
            .trim()
            .takeIf { it.isNotBlank() }
            ?: return null
        return findQuestionByTitle(state, title)
    }

    private fun tabForQuestionId(state: StudentAiChatUiState, id: String): AiMenuTab? = when {
        state.examPrepPyqs.any { it.id == id } -> AiMenuTab.ExamPreparation
        state.textbookQuestions.any { it.id == id } -> AiMenuTab.TextbookQuestions
        else -> null
    }

    private fun findQuestionByTitle(
        state: StudentAiChatUiState,
        title: String,
    ): Pair<String, AiMenuTab>? {
        val normalized = normalizeQuestionTitle(title)
        state.textbookQuestions
            .firstOrNull { normalizeQuestionTitle(it.title) == normalized }
            ?.let { return it.id to AiMenuTab.TextbookQuestions }
        state.examPrepPyqs
            .firstOrNull { normalizeQuestionTitle(it.title) == normalized }
            ?.let { return it.id to AiMenuTab.ExamPreparation }
        state.textbookQuestions
            .firstOrNull {
                val candidate = normalizeQuestionTitle(it.title)
                candidate.contains(normalized) || normalized.contains(candidate)
            }
            ?.let { return it.id to AiMenuTab.TextbookQuestions }
        state.examPrepPyqs
            .firstOrNull {
                val candidate = normalizeQuestionTitle(it.title)
                candidate.contains(normalized) || normalized.contains(candidate)
            }
            ?.let { return it.id to AiMenuTab.ExamPreparation }
        return null
    }

    private fun normalizeQuestionTitle(text: String): String =
        text.trim().lowercase(java.util.Locale.ROOT)

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
