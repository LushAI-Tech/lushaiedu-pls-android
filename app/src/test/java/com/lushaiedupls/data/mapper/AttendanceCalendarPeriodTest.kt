package com.lushaiedupls.data.mapper

import com.lushaiedupls.data.remote.dto.AttendanceCalendar
import com.lushaiedupls.data.remote.dto.AttendanceStatus
import com.lushaiedupls.data.remote.dto.AttendanceTotals
import com.lushaiedupls.data.remote.dto.DayStatus
import com.lushaiedupls.data.remote.dto.StudentAttendanceSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceCalendarPeriodTest {

    private val emptySummary = StudentAttendanceSummary(
        student_id = "s1",
        month = "2026-08",
        overall = AttendanceTotals(),
        regular = AttendanceTotals(),
        extra = AttendanceTotals(),
    )

    @Test
    fun attendanceDashboard_usesPeriodMetadataFromCalendar() {
        val calendar = AttendanceCalendar(
            month = "2026-08",
            days = listOf(
                DayStatus(
                    day = "2026-08-12",
                    status = AttendanceStatus.PRESENT,
                    is_extra_class = false,
                    teaching_unit_id = "unit-1",
                    subject_name = "Physics",
                    period_id = "p2",
                    period_name = "Period 2",
                    start_time = "10:30:00",
                    end_time = "11:15:00",
                ),
            ),
        )

        val dashboard = StudentUiMappers.attendanceDashboard(emptySummary, calendar)

        assertEquals(1, dashboard.sessions.size)
        assertEquals("Physics", dashboard.sessions.first().subject)
        assertEquals("10:30 AM - 11:15 AM", dashboard.sessions.first().periodLabel)
        assertTrue(!dashboard.sessions.first().isExtraClass)
    }

    @Test
    fun attendanceDashboard_marksExtraClassWhenPeriodFieldsNull() {
        val calendar = AttendanceCalendar(
            month = "2026-08",
            days = listOf(
                DayStatus(
                    day = "2026-08-13",
                    status = AttendanceStatus.ABSENT,
                    is_extra_class = true,
                    teaching_unit_id = "unit-1",
                    subject_name = "Chemistry",
                ),
            ),
        )

        val dashboard = StudentUiMappers.attendanceDashboard(emptySummary, calendar)

        assertEquals(1, dashboard.sessions.size)
        assertEquals("Chemistry", dashboard.sessions.first().subject)
        assertEquals("", dashboard.sessions.first().periodLabel)
        assertTrue(dashboard.sessions.first().isExtraClass)
    }

    @Test
    fun attendanceDashboard_periodNameWithoutTimesShowsNothing() {
        val calendar = AttendanceCalendar(
            month = "2026-08",
            days = listOf(
                DayStatus(
                    day = "2026-08-14",
                    status = AttendanceStatus.PRESENT,
                    teaching_unit_id = "unit-1",
                    subject_name = "Math",
                    period_name = "Period 1",
                ),
            ),
        )

        val dashboard = StudentUiMappers.attendanceDashboard(emptySummary, calendar)

        assertEquals("", dashboard.sessions.first().periodLabel)
    }
}
