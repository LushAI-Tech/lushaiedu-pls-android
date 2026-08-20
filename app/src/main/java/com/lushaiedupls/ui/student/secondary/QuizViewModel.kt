package com.lushaiedupls.ui.student.secondary

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.mock.QuizQuestion
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.AnswerResult
import com.lushaiedupls.data.remote.dto.AnswerSubmission
import com.lushaiedupls.data.remote.dto.QuizSubmitResponse
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizAnswerResultUi(
    val questionId: String,
    val prompt: String,
    val studentAnswer: String,
    val correctAnswer: String?,
    val isCorrect: Boolean,
    val explanation: String?,
    val marksAwarded: Double,
)

data class QuizResultUi(
    val score: Double,
    val maxScore: Double,
    val correct: Int,
    val incorrect: Int,
    val unanswered: Int,
    val percentage: Double,
    val items: List<QuizAnswerResultUi>,
    val message: String?,
    val nextSteps: List<String>,
    val passed: Boolean?,
    val recommendedRetake: Boolean,
)

data class QuizUiState(
    val questions: List<QuizQuestion> = emptyList(),
    val attemptId: String = "",
    val selectedByQuestionId: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val result: QuizResultUi? = null,
    val errorMessage: String? = null,
)

class QuizViewModel(
    private val studentRepository: StudentRepository,
    private val chapterIdHint: String?,
    private val sectionIdHint: String?,
) : ViewModel() {

    private var attemptByQuestionId: Map<String, String> = emptyMap()
    private val startedAtElapsed = SystemClock.elapsedRealtime()

    private val _uiState = MutableStateFlow(QuizUiState(isLoading = true))
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectAnswer(questionId: String, answer: String) {
        _uiState.update {
            it.copy(selectedByQuestionId = it.selectedByQuestionId + (questionId to answer))
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    result = null,
                    selectedByQuestionId = emptyMap(),
                )
            }
            val sectionIds = sectionIdHint
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()
            if (sectionIds.isNotEmpty()) {
                loadSectionQuizzes(sectionIds)
                return@launch
            }
            val chapterId = resolveChapterId()
            if (chapterId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "No chapter available for quiz.")
                }
                return@launch
            }
            when (val result = studentRepository.quizChapter(chapterId)) {
                is NetworkResult.Success -> {
                    attemptByQuestionId = result.data.questions.associate { it.id to result.data.attempt_id }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            attemptId = result.data.attempt_id,
                            questions = StudentUiMappers.quizQuestions(result.data.questions),
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun submit() {
        if (_uiState.value.isSubmitting || _uiState.value.result != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val selected = _uiState.value.selectedByQuestionId
            val grouped = linkedMapOf<String, MutableList<AnswerSubmission>>()
            selected.forEach { (qid, answer) ->
                val attemptId = attemptByQuestionId[qid] ?: _uiState.value.attemptId
                if (attemptId.isBlank()) return@forEach
                grouped.getOrPut(attemptId) { mutableListOf() }.add(
                    AnswerSubmission(question_id = qid, student_answer = answer),
                )
            }
            if (grouped.isEmpty()) {
                val attemptId = _uiState.value.attemptId
                if (attemptId.isBlank()) {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = "No quiz attempt to submit.") }
                    return@launch
                }
                grouped[attemptId] = mutableListOf()
            }
            val elapsedSeconds = ((SystemClock.elapsedRealtime() - startedAtElapsed) / 1000L)
                .toInt()
                .coerceAtLeast(0)
            val responses = mutableListOf<QuizSubmitResponse>()
            var lastError: String? = null
            grouped.forEach { (attemptId, answers) ->
                when (
                    val result = studentRepository.submitQuiz(
                        attemptId = attemptId,
                        answers = answers,
                        timeTakenSeconds = elapsedSeconds,
                    )
                ) {
                    is NetworkResult.Success -> responses += result.data
                    else -> lastError = result.userMessage()
                }
            }
            if (responses.isEmpty()) {
                _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = lastError ?: "Could not submit quiz.")
                }
                return@launch
            }
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    errorMessage = lastError,
                    result = mergeResults(responses, it.questions, selected),
                )
            }
        }
    }

    private fun mergeResults(
        responses: List<QuizSubmitResponse>,
        questions: List<QuizQuestion>,
        selected: Map<String, String>,
    ): QuizResultUi {
        val promptById = questions.associate { it.id to it.prompt }
        val resultById = linkedMapOf<String, AnswerResult>()
        responses.flatMap { it.results }.forEach { row ->
            resultById.putIfAbsent(row.question_id, row)
        }
        val items = questions.map { question ->
            val row = resultById.remove(question.id)
            if (row != null) {
                toAnswerUi(row, question.prompt)
            } else {
                QuizAnswerResultUi(
                    questionId = question.id,
                    prompt = question.prompt,
                    studentAnswer = selected[question.id].orEmpty(),
                    correctAnswer = null,
                    isCorrect = false,
                    explanation = null,
                    marksAwarded = 0.0,
                )
            }
        } + resultById.values.map { row ->
            toAnswerUi(row, promptById[row.question_id].orEmpty())
        }
        val uniqueItems = items.distinctBy { it.questionId }
        val score = responses.sumOf { it.score }
        val maxScore = responses.sumOf { it.max_score }
        val percentage = if (maxScore > 0) (score / maxScore) * 100.0 else responses.last().percentage
        val suggestion = responses.mapNotNull { it.suggestions }.lastOrNull()
        val message = suggestion?.message?.trim()?.takeIf { it.isNotBlank() }
        val nextSteps = suggestion?.next_steps
            .orEmpty()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .filter { step -> message == null || !step.equals(message, ignoreCase = true) }
        val retake = suggestion?.recommended_retake == true &&
            message?.contains("retake", ignoreCase = true) != true
        return QuizResultUi(
            score = score,
            maxScore = maxScore,
            correct = responses.sumOf { it.correct },
            incorrect = responses.sumOf { it.incorrect },
            unanswered = responses.sumOf { it.unanswered },
            percentage = percentage,
            items = uniqueItems,
            message = message,
            nextSteps = nextSteps,
            passed = suggestion?.passed,
            recommendedRetake = retake,
        )
    }

    private fun toAnswerUi(row: AnswerResult, prompt: String): QuizAnswerResultUi =
        QuizAnswerResultUi(
            questionId = row.question_id,
            prompt = prompt,
            studentAnswer = row.student_answer,
            correctAnswer = row.correct_answer,
            isCorrect = row.is_correct,
            explanation = row.explanation,
            marksAwarded = row.marks_awarded,
        )

    private suspend fun loadSectionQuizzes(sectionIds: List<String>) {
        val questions = mutableListOf<QuizQuestion>()
        val attempts = linkedMapOf<String, String>()
        var lastError: String? = null
        var firstAttempt = ""
        for (sectionId in sectionIds) {
            when (val result = studentRepository.quizSection(sectionId)) {
                is NetworkResult.Success -> {
                    if (firstAttempt.isBlank()) firstAttempt = result.data.attempt_id
                    val mapped = StudentUiMappers.quizQuestions(result.data.questions)
                    mapped.forEach { question ->
                        attempts[question.id] = result.data.attempt_id
                    }
                    questions += mapped
                }
                else -> lastError = result.userMessage()
            }
        }
        attemptByQuestionId = attempts
        val uniqueQuestions = questions.distinctBy { it.id }
        if (uniqueQuestions.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = lastError ?: "No quiz questions.",
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    attemptId = firstAttempt,
                    questions = uniqueQuestions,
                    errorMessage = null,
                )
            }
        }
    }

    private suspend fun resolveChapterId(): String? {
        chapterIdHint?.takeIf { it.isNotBlank() }?.let { return it }
        val resume = studentRepository.progressResume()
        if (resume is NetworkResult.Success && !resume.data?.chapter_id.isNullOrBlank()) {
            return resume.data?.chapter_id
        }
        val subjects = studentRepository.aiSubjects()
        val subjectId = (subjects as? NetworkResult.Success)?.data?.firstOrNull()?.subject_id
            ?: return null
        val chapters = studentRepository.chapters(subjectId)
        return (chapters as? NetworkResult.Success)?.data
            ?.filter { it.is_active }
            ?.minByOrNull { it.chapter_number }
            ?.id
    }

    companion object {
        fun provideFactory(
            studentRepository: StudentRepository,
            chapterIdHint: String? = null,
            sectionIdHint: String? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            QuizViewModel(studentRepository, chapterIdHint, sectionIdHint)
        }
    }
}
