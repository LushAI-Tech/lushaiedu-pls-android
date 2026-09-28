package com.lushaiedupls.ui.teacher

import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.remote.dto.AttendanceTotals
import com.lushaiedupls.data.remote.dto.DayOfWeek
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.PeriodOut
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.TeachingUnitStatus
import com.lushaiedupls.data.remote.dto.UnitMonthAttendance
import com.lushaiedupls.data.remote.dto.UnitMonthDay
import com.lushaiedupls.data.remote.dto.WeekSlot
import com.lushaiedupls.data.remote.dto.WeekView
import java.time.YearMonth
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

    @Test
    fun assignedInstitutions_keepsOnlyTeacherUnits() {
        val catalog = listOf(
            InstitutionOut(id = "a", name = "Alpha", sort_order = 1),
            InstitutionOut(id = "b", name = "Beta", sort_order = 2),
            InstitutionOut(id = "c", name = "Gamma", sort_order = 3),
        )
        val units = listOf(
            TeachingUnitOut(
                id = "u1",
                class_id = "c1",
                subject_id = "s1",
                class_name = "XII",
                subject_name = "Chemistry",
                status = TeachingUnitStatus.ACTIVE,
                institution_id = "b",
                institution_name = "Beta",
            ),
        )
        val institutions = TeacherUiMappers.assignedInstitutions(units, catalog)
        assertEquals(listOf("b"), institutions.map { it.id })
        assertEquals("Beta", institutions.single().name)
    }

    @Test
    fun assignedClasses_keepsOnlyTeacherClassesForInstitution() {
        val units = listOf(
            TeachingUnitOut(
                id = "u1",
                class_id = "c1",
                subject_id = "s1",
                class_name = "XII",
                subject_name = "Chemistry",
                status = TeachingUnitStatus.ACTIVE,
                institution_id = "inst-1",
            ),
            TeachingUnitOut(
                id = "u2",
                class_id = "c2",
                subject_id = "s2",
                class_name = "XI",
                subject_name = "Physics",
                status = TeachingUnitStatus.ACTIVE,
                institution_id = "inst-2",
            ),
            TeachingUnitOut(
                id = "u3",
                class_id = "c1",
                subject_id = "s3",
                class_name = "XII",
                subject_name = "Math",
                status = TeachingUnitStatus.ACTIVE,
                institution_id = "inst-1",
            ),
        )
        val classes = TeacherUiMappers.assignedClasses(units, "inst-1")
        assertEquals(listOf("c1"), classes.map { it.id })
        assertEquals("Class XII", classes.single().name)
    }

    @Test
    fun markedRollDays_usesSparseMonthDays() {
        val monthView = UnitMonthAttendance(
            teaching_unit_id = "unit-chem",
            class_name = "Class XII",
            subject_name = "Chemistry",
            month = "2026-09",
            days = listOf(
                UnitMonthDay(
                    day = "2026-09-08",
                    totals = AttendanceTotals(present = 2, absent = 1, sessions = 3),
                ),
                UnitMonthDay(
                    day = "2026-09-18",
                    has_extra_class = true,
                    totals = AttendanceTotals(present = 1, sessions = 1),
                ),
            ),
        )
        val days = TeacherUiMappers.markedRollDays(
            monthView = monthView,
            month = YearMonth.of(2026, 9),
        )
        assertEquals(setOf(8, 18), days)
    }
}
