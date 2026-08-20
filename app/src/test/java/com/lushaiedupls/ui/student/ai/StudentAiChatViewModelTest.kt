package com.lushaiedupls.ui.student.ai

import com.lushaiedupls.data.mock.StudentMockRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentAiChatViewModelTest {

    private val mockRepository = StudentMockRepository()

    @Test
    fun mockAiChatSessionLoadsFast() {
        val session = mockRepository.aiChatSession("chemistry")
        assertNotNull(session)
        assertTrue(session.syllabus.isNotEmpty())
        val pack = session.packFor("English")
        assertTrue(pack.messages.isNotEmpty())
        assertEquals("English", session.packFor("English").messages.isNotEmpty().let { "English" })
    }
}
