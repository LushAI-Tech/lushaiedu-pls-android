package com.lushaiedupls.ui.student.ai

import com.lushaiedupls.data.mock.AiChatMessage
import com.lushaiedupls.data.mock.AiMenuContentItem
import com.lushaiedupls.data.mock.AiMenuTab
import com.lushaiedupls.data.mock.AiQuickCheck
import com.lushaiedupls.data.mock.AiQuizHistoryItem
import com.lushaiedupls.data.mock.AiSyllabusItem

data class StudentAiChatUiState(
    val chapterId: String = "",
    val chapterTitle: String = "",
    val messages: List<AiChatMessage> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val quickCheck: AiQuickCheck? = null,
    val syllabus: List<AiSyllabusItem> = emptyList(),
    val textbookQuestions: List<AiMenuContentItem> = emptyList(),
    val examPrepPyqs: List<AiMenuContentItem> = emptyList(),
    val resources: List<AiMenuContentItem> = emptyList(),
    val quizHistory: List<AiQuizHistoryItem> = emptyList(),
    val draft: String = "",
    val language: String = "English",
    val selectedQuickOption: String? = null,
    val quickCheckAnswered: Boolean = false,
    val quickCheckCorrect: Boolean? = null,
    val quickCheckExplanation: String? = null,
    val showMenu: Boolean = false,
    val menuTab: AiMenuTab = AiMenuTab.Chats,
    val selectedSyllabusIds: Set<String> = emptySet(),
    val showQuickCheck: Boolean = false,
    val isLoading: Boolean = false,
    val isMenuContentLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)
