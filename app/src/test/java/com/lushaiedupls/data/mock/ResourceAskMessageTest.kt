package com.lushaiedupls.data.mock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceAskMessageTest {

    @Test
    fun isResourceAskMessageUsesLinkedResourceId() {
        val withResource = AiChatMessage(
            id = "1",
            text = "",
            fromUser = true,
            linkedResourceId = "att-1",
            linkedResourceTitle = "Clip",
        )
        val plain = AiChatMessage(id = "2", text = "Hello", fromUser = true)
        assertTrue(withResource.isResourceAskMessage())
        assertFalse(plain.isResourceAskMessage())
        assertFalse(withResource.isQuestionAskMessage())
    }

    @Test
    fun emptyQuestionAskStillLinksToQuestion() {
        val withQuestion = AiChatMessage(
            id = "3",
            text = "",
            fromUser = true,
            linkedQuestionId = "q-1",
            linkedQuestionTab = AiMenuTab.ExamPreparation,
            linkedQuestionTitle = "What is mole fraction?",
        )
        assertTrue(withQuestion.isQuestionAskMessage())
        assertFalse(withQuestion.isResourceAskMessage())
    }
}
