package com.lushaiedupls.data.mock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lightweight smoke checks for student mock data used across Student phases 1–10.
 */
class StudentMockRepositorySmokeTest {

    private val repo = StudentMockRepository()

    @Test
    fun homeMocksArePopulated() {
        assertEquals(4, repo.overviewMetrics().size)
        assertNotNull(repo.sessionSummary())
        assertTrue(repo.attendanceRecords().isNotEmpty())
    }

    @Test
    fun attendanceDashboardMatchesDesignCounts() {
        val dash = repo.attendanceDashboard()
        assertEquals(3, dash.primaryStats.size)
        assertEquals(3, dash.secondaryStats.size)
        assertTrue(dash.bySubject.isNotEmpty())
    }

    @Test
    fun calendarAndAiMocksArePopulated() {
        assertTrue(repo.calendarEvents().isNotEmpty())
        assertEquals(3, repo.aiSubjects().size)
        assertTrue(repo.aiChatSession("chemistry").packFor("English").messages.isNotEmpty())
        assertTrue(repo.aiChatSession("chemistry").syllabus.isNotEmpty())
    }

    @Test
    fun secondaryScreensMocksArePopulated() {
        assertTrue(repo.notifications().isNotEmpty())
        assertEquals(3, repo.chapters().size)
        assertEquals(5, repo.quizQuestions().size)
        assertFalse(repo.weeklyTimetable().subjects.isEmpty())
        assertTrue(repo.registeredDevices().isNotEmpty())
    }
}
