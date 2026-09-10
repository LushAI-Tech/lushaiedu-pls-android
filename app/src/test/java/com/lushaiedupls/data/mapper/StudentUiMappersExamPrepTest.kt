package com.lushaiedupls.data.mapper

import com.lushaiedupls.data.remote.dto.ExamPrepPyqHitOut
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudentUiMappersExamPrepTest {

    @Test
    fun examPrepPyqs_mapsMarksOntoQuestionCards() {
        val hits = listOf(
            pyqHit(id = "1", preview = "State Henry's law.", marks = JsonPrimitive(5)),
            pyqHit(id = "2", preview = "Define molality.", marks = JsonPrimitive("3 marks")),
            pyqHit(id = "3", preview = "Explain vapour pressure.", marks = null),
        )

        val items = StudentUiMappers.examPrepPyqs(hits)

        assertEquals("5", items[0].marksLabel)
        assertEquals("3 marks", items[1].marksLabel)
        assertNull(items[2].marksLabel)
        assertEquals("MBSE · 2024", items[0].subtitle)
    }

    @Test
    fun formatMarksLabel_handlesNumbersStringsAndMissingValues() {
        assertEquals("5", StudentUiMappers.formatMarksLabel(JsonPrimitive(5)))
        assertEquals("1", StudentUiMappers.formatMarksLabel(JsonPrimitive(1.0)))
        assertEquals("2.5", StudentUiMappers.formatMarksLabel(JsonPrimitive(2.5)))
        assertEquals("4", StudentUiMappers.formatMarksLabel(JsonPrimitive("4")))
        assertEquals("2+3", StudentUiMappers.formatMarksLabel(JsonPrimitive("2+3")))
        assertEquals("5 marks", StudentUiMappers.formatMarksLabel(JsonPrimitive("5 marks")))
        assertNull(StudentUiMappers.formatMarksLabel(null))
        assertNull(StudentUiMappers.formatMarksLabel(JsonNull))
        assertNull(StudentUiMappers.formatMarksLabel(JsonPrimitive(0)))
    }

    private fun pyqHit(
        id: String,
        preview: String,
        marks: kotlinx.serialization.json.JsonElement?,
    ) = ExamPrepPyqHitOut(
        content_block_id = id,
        paper_id = "paper-$id",
        paper_title = "MBSE",
        calendar_year = 2024,
        exam_code = "MBSE",
        exam_name = "MBSE",
        marks = marks,
        question_preview = preview,
        block_type = "question",
        similarity = 0.9,
    )
}
