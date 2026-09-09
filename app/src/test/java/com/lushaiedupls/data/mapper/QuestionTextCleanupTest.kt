package com.lushaiedupls.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test

class QuestionTextCleanupTest {

    @Test
    fun cleanQuestionText_removesGeneratedQuestionNumber() {
        assertEquals(
            "Define molality and molarity.",
            StudentUiMappers.cleanQuestionText("Q1. Define molality and molarity.", "Q1"),
        )
        assertEquals(
            "What is a solution?",
            StudentUiMappers.cleanQuestionText("Exercise 2: What is a solution?", null),
        )
        assertEquals(
            "State Henry's law.",
            StudentUiMappers.cleanQuestionText("3) State Henry's law.", null),
        )
    }
}
