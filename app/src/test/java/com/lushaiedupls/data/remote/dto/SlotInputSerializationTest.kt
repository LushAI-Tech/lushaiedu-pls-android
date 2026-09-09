package com.lushaiedupls.data.remote.dto

import com.lushaiedupls.data.remote.ApiClient
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SlotInputSerializationTest {

    private val json = ApiClient.json

    @Test
    fun setSlotsRequest_minimalBodyOmitsSubjectId() {
        val body = SetSlotsRequest(
            slots = listOf(
                SlotInput.of(
                    subjectId = null,
                    periodId = "PERIOD_UUID",
                    dayOfWeek = DayOfWeek.MON,
                    room = "Room 1",
                ),
            ),
        )
        val encoded = json.encodeToString(SetSlotsRequest.serializer(), body)
        val slot = json.parseToJsonElement(encoded).jsonObject["slots"]!!.jsonArray[0].jsonObject
        assertEquals(
            listOf("period_id", "day_of_week", "room"),
            slot.keys.toList(),
        )
        assertEquals("PERIOD_UUID", slot["period_id"]!!.jsonPrimitive.content)
        assertEquals("MON", slot["day_of_week"]!!.jsonPrimitive.content)
        assertEquals("Room 1", slot["room"]!!.jsonPrimitive.content)
        assertFalse(slot.containsKey("subject_id"))
    }

    @Test
    fun setSlotsRequest_includesSubjectIdWhenProvided() {
        val body = SetSlotsRequest(
            slots = listOf(
                SlotInput.of(
                    subjectId = "11111111-1111-1111-1111-111111111111",
                    periodId = "22222222-2222-2222-2222-222222222222",
                    dayOfWeek = DayOfWeek.WED,
                    room = "  A-201  ",
                ),
            ),
        )

        val encoded = json.encodeToString(SetSlotsRequest.serializer(), body)
        val parsed = json.parseToJsonElement(encoded).jsonObject
        val slot = parsed["slots"]!!.jsonArray[0].jsonObject

        assertEquals(
            listOf("subject_id", "period_id", "day_of_week", "room"),
            slot.keys.toList(),
        )
        assertEquals("11111111-1111-1111-1111-111111111111", slot["subject_id"]!!.jsonPrimitive.content)
        assertEquals("22222222-2222-2222-2222-222222222222", slot["period_id"]!!.jsonPrimitive.content)
        assertEquals("WED", slot["day_of_week"]!!.jsonPrimitive.content)
        assertEquals("A-201", slot["room"]!!.jsonPrimitive.content)
        assertFalse(slot.containsKey("effective_from"))
        assertFalse(slot.containsKey("effective_until"))
    }

    @Test
    fun slotInput_omitsBlankOptionalFields() {
        val slot = json.encodeToJsonElement(
            SlotInput.serializer(),
            SlotInput.of(
                subjectId = " ",
                periodId = "22222222-2222-2222-2222-222222222222",
                dayOfWeek = DayOfWeek.MON,
                room = "",
            ),
        ).jsonObject

        assertFalse(slot.containsKey("subject_id"))
        assertFalse(slot.containsKey("room"))
        assertEquals("MON", slot["day_of_week"]!!.jsonPrimitive.content)
    }

    @Test
    fun weekSlot_toSlotInput_usesFallbackSubjectId() {
        val slot = WeekSlot(
            slot_id = "s1",
            teaching_unit_id = "u1",
            class_name = "Class XII",
            subject_name = "Physics",
            subject_id = null,
            period_id = "p1",
            period_name = "Period 1",
            start_time = "09:00",
            end_time = "09:45",
            day_of_week = DayOfWeek.FRI,
            room = "Lab 2",
        ).toSlotInput(fallbackSubjectId = "sub-physics")

        assertEquals("sub-physics", slot.subject_id)
        assertEquals("p1", slot.period_id)
        assertEquals(DayOfWeek.FRI, slot.day_of_week)
        assertEquals("Lab 2", slot.room)
        assertNull(slot.effective_from)
        assertNull(slot.effective_until)
    }
}
