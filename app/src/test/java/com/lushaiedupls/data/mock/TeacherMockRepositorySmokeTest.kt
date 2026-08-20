package com.lushaiedupls.data.mock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lightweight smoke checks for teacher mock data used across Teacher phases 1–10.
 */
class TeacherMockRepositorySmokeTest {

    private val repo = TeacherMockRepository()

    @Test
    fun homeAndGroupsMocksArePopulated() {
        assertTrue(repo.homeDashboard().classes.isNotEmpty())
        assertTrue(repo.groups().isNotEmpty())
        assertEquals(2, repo.profile().notificationCount)
    }

    @Test
    fun overviewAttendanceAndStudentsArePopulated() {
        assertTrue(repo.overviewDashboard().presentPercent >= 0)
        assertEquals(9, repo.studentsInClass().size)
        assertEquals(4, repo.attendanceClasses().size)
        assertTrue(repo.attendanceSession().students.isNotEmpty())
    }

    @Test
    fun calendarAcademicAndAiMocksArePopulated() {
        assertTrue(repo.calendarEvents().isNotEmpty())
        assertEquals(3, repo.chapters().size)
        assertEquals(5, repo.quizQuestions().size)
        assertEquals(6, repo.teachingTimetable().days.size)
        assertEquals(3, repo.aiSubjects().size)
        assertTrue(repo.aiChatSession("chemistry").syllabus.isNotEmpty())
    }

    @Test
    fun overlaysAndSecondaryMocksArePopulated() {
        assertEquals(8, repo.dayPeriods().size)
        assertEquals(4, repo.timetableSubjects().size)
        assertTrue(repo.notifications().isNotEmpty())
        assertTrue(repo.registeredDevices().isNotEmpty())
        assertFalse(repo.announcementAudiences().isEmpty())
    }
}
