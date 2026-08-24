package com.lushaiedupls.data.mapper

import com.lushaiedupls.data.remote.dto.DayOfWeek
import com.lushaiedupls.data.remote.dto.PeriodOut
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.TeachingUnitStatus
import com.lushaiedupls.data.remote.dto.WeekSlot
import com.lushaiedupls.data.remote.dto.WeekView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentUiMappersTimetableTest {

    @Test
    fun weeklyTimetable_includesSubjectsFromTeachingUnits() {
        val periods = listOf(
            PeriodOut(id = "p1", name = "Period 1", start_time = "09:00", end_time = "09:45", sort_order = 1, is_active = true),
            PeriodOut(id = "p2", name = "Period 2", start_time = "10:00", end_time = "10:45", sort_order = 2, is_active = true),
        )
        val days = mapOf(
            "MON" to listOf(
                WeekSlot(
                    slot_id = "s1",
                    teaching_unit_id = "u1",
                    class_name = "Class XII",
                    subject_name = "Physics",
                    period_id = "p1",
                    period_name = "Period 1",
                    start_time = "09:00",
                    end_time = "09:45",
                    day_of_week = DayOfWeek.MON,
                ),
            ),
        )
        val weekView = WeekView(periods = periods, days = days)

        val teachingUnits = listOf(
            TeachingUnitOut(
                id = "u1",
                class_id = "c1",
                subject_id = "sub1",
                class_name = "Class XII",
                subject_name = "Physics",
                status = TeachingUnitStatus.ACTIVE,
            ),
            TeachingUnitOut(
                id = "u2",
                class_id = "c1",
                subject_id = "sub2",
                class_name = "Class XII",
                subject_name = "Mathematics",
                status = TeachingUnitStatus.ACTIVE,
            ),
            TeachingUnitOut(
                id = "u3",
                class_id = "c1",
                subject_id = "sub3",
                class_name = "Class XII",
                subject_name = "Chemistry",
                status = TeachingUnitStatus.ACTIVE,
            ),
        )

        val timetable = StudentUiMappers.weeklyTimetable(weekView, teachingUnits)

        // All teaching unit subjects plus 'All' tab should be present
        assertEquals(listOf("All", "Chemistry", "Mathematics", "Physics"), timetable.subjects)

        // Physics has Monday Period 1 filled
        val physicsRows = timetable.cellsBySubject["Physics"]
        assertTrue(physicsRows != null)
        assertEquals("Physics", physicsRows!![0][0])
        assertEquals("Off", physicsRows[0][1])

        // All has Monday Period 1 filled with Physics
        val allRows = timetable.cellsBySubject["All"]
        assertTrue(allRows != null)
        assertEquals("Physics", allRows!![0][0])
        assertEquals("Off", allRows[0][1])

        // Mathematics has all Off
        val mathRows = timetable.cellsBySubject["Mathematics"]
        assertTrue(mathRows != null)
        assertEquals("Off", mathRows!![0][0])
        assertEquals("Off", mathRows[0][1])
    }
}
