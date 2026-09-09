package com.lushaiedupls.ui.teacher

import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.remote.dto.DayOfWeek
import com.lushaiedupls.data.remote.dto.PeriodOut
import com.lushaiedupls.data.remote.dto.WeekSlot
import com.lushaiedupls.data.remote.dto.WeekView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TeacherTimetableSlotTest {

    @Test
    fun teacherTeachingTimetable_mapsSlotsToGridCells() {
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
                    room = "Room 101",
                ),
            ),
        )
        val weekView = WeekView(periods = periods, days = days)
        val timetable = TeacherUiMappers.teachingTimetable(weekView)

        assertEquals(2, timetable.timeSlots.size)
        assertEquals(6, timetable.days.size)

        // timeIndex 0 (Period 1) to dayIndex 0 (Monday) should contain the Physics cell
        val cell = timetable.cells[0 to 0]
        assertNotNull(cell)
        assertEquals("Physics", cell!!.subject)

        // timeIndex 1 (Period 2) to dayIndex 0 (Monday) should be empty
        assertNull(timetable.cells[1 to 0])
    }

    @Test
    fun classSetTimetable_keepsSlotSubjectId() {
        val periods = listOf(
            PeriodOut(id = "p1", name = "Period 1", start_time = "09:00", end_time = "09:45", sort_order = 1, is_active = true),
        )
        val weekView = WeekView(
            periods = periods,
            days = mapOf(
                "MON" to listOf(
                    WeekSlot(
                        slot_id = "s1",
                        teaching_unit_id = "u1",
                        class_name = "Class XII",
                        subject_name = "Physics",
                        subject_id = "sub-physics",
                        period_id = "p1",
                        period_name = "Period 1",
                        start_time = "09:00",
                        end_time = "09:45",
                        day_of_week = DayOfWeek.MON,
                        room = "Lab 2",
                    ),
                ),
            ),
        )

        val timetable = TeacherUiMappers.classSetTimetable(
            week = weekView,
            periods = periods,
            classLabels = listOf("Class XII"),
            myUnitIds = setOf("u1"),
        )

        val cell = timetable.cells[0 to 0]
        assertNotNull(cell)
        assertEquals("Physics", cell!!.subject)
        assertEquals("sub-physics", cell.subjectId)
        assertEquals("Lab 2", cell.detail)
    }
}
