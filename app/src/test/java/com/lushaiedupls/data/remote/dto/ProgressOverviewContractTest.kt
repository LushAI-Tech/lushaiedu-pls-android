package com.lushaiedupls.data.remote.dto

import com.lushaiedupls.data.remote.ApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressOverviewContractTest {

    private val json = ApiClient.json

    @Test
    fun selfOverview_decodesCompositeMasteryAndRenamedQuizFields() {
        val decoded = json.decodeFromString(
            ProgressOverviewResponse.serializer(),
            """
            {
              "scope": "self",
              "ai_available": true,
              "self_overview": {
                "totals": {
                  "subjects_in_progress": 3,
                  "chapters_in_progress": 7,
                  "overall_mastery_pct": 62.4,
                  "overall_quiz_mastery_pct": 55.0,
                  "overall_progress_pct": 48.0,
                  "mastered_sections": 12,
                  "total_sections": 30,
                  "quick_check_attempts": 9,
                  "quick_check_correct": 7,
                  "quick_check_accuracy_pct": 77.8,
                  "quizzes_completed": 4,
                  "last_accessed_at": "2026-09-16T10:00:00+00:00"
                },
                "subjects": [
                  {
                    "textbook_id": "tb1",
                    "subject_id": "sub1",
                    "subject_name": "Chemistry",
                    "class_name": "Class XI",
                    "progress_pct": 40.0,
                    "last_accessed_at": "2026-09-16T10:00:00+00:00",
                    "overall_mastery_pct": 58.0,
                    "quiz_mastery_pct": 50.0,
                    "mastered_sections": 4,
                    "total_sections": 10,
                    "chapters": [
                      {
                        "chapter_id": "ch1",
                        "title": "Atomic Structure",
                        "chapter_number": 1,
                        "quiz_mastery_pct": 60.0,
                        "mastered_sections": 2,
                        "total_sections": 3,
                        "quick_check_attempts": 2,
                        "quick_check_correct": 2,
                        "quick_check_accuracy_pct": 100.0,
                        "sections": [
                          {
                            "section_id": "sec1",
                            "section_title": "Introduction",
                            "section_number": "1.1",
                            "best_score": 90.0,
                            "latest_score": 80.0,
                            "attempts": 2,
                            "mastered": true,
                            "weak_concepts": ["isotopes"]
                          }
                        ]
                      }
                    ],
                    "quick_check_attempts": 5,
                    "quick_check_correct": 4,
                    "quick_check_accuracy_pct": 80.0
                  }
                ]
              }
            }
            """.trimIndent(),
        )

        assertEquals("self", decoded.scope)
        assertTrue(decoded.ai_available)
        val totals = decoded.totals()
        assertEquals(62.4, totals.overall_mastery_pct, 0.01)
        assertEquals(55.0, totals.overall_quiz_mastery_pct, 0.01)
        assertEquals(48.0, totals.overall_progress_pct, 0.01)
        assertEquals(4, totals.quizzes_completed)

        val subject = decoded.subjectOverview(subjectId = "sub1")
        requireNotNull(subject)
        assertEquals(58.0, subject.overall_mastery_pct, 0.01)
        assertEquals(50.0, subject.quiz_mastery_pct, 0.01)
        assertEquals("2026-09-16T10:00:00+00:00", subject.last_accessed_at)
        assertEquals(1, subject.chapters.size)
        assertEquals(60.0, subject.chapters.first().quiz_mastery_pct, 0.01)
        val section = subject.chapters.first().sections.first()
        assertEquals("Introduction", section.section_title)
        assertEquals("1.1", section.section_number)
        assertEquals("isotopes", section.weak_concepts.first())
    }
}
