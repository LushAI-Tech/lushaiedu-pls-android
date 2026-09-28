package com.lushaiedupls.data.mapper

import com.lushaiedupls.data.remote.dto.AiSnapshot
import com.lushaiedupls.data.remote.dto.AttendanceTotals
import com.lushaiedupls.data.remote.dto.ProgressChapterOverview
import com.lushaiedupls.data.remote.dto.ProgressOverviewResponse
import com.lushaiedupls.data.remote.dto.ProgressOverviewTotals
import com.lushaiedupls.data.remote.dto.ProgressSubjectOverview
import com.lushaiedupls.data.remote.dto.SelfProgressOverview
import com.lushaiedupls.data.remote.dto.StudentOverview
import com.lushaiedupls.data.remote.dto.UserSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudentUiMappersProgressTest {

    @Test
    fun aiHubStats_usesCompositeOverallMastery() {
        val stats = StudentUiMappers.aiHubStats(sampleOverview())
        assertEquals("62%", stats[0].value)
        assertEquals("Mastery", stats[0].label)
        assertEquals("48%", stats[1].value)
        assertEquals("4", stats[2].value)
    }

    @Test
    fun overviewMetrics_usesCompositeOverallMasteryFromProgressOverview() {
        val metrics = StudentUiMappers.overviewMetrics(
            overview = StudentOverview(
                student = UserSummary(id = "u1", name = "Ada"),
                subject_count = 3,
                ai = AiSnapshot(
                    stem_mastery_pct = 10.0,
                    reading_progress_pct = 11.0,
                    quizzes_completed = 1,
                ),
                overall = AttendanceTotals(),
            ),
            progress = sampleOverview(),
        )
        assertEquals("62%", metrics[1].value)
        assertEquals("Stem Mastery", metrics[1].label)
        assertEquals("48%", metrics[2].value)
        assertEquals("4", metrics[3].value)
    }

    @Test
    fun chapterStats_prefersSubjectCompositeMasteryOverTotals() {
        val stats = StudentUiMappers.chapterStats(
            overview = sampleOverview(),
            subjectId = "sub1",
        )
        requireNotNull(stats)
        assertEquals("58%", stats.mastery)
        assertEquals("40%", stats.reading)
        assertEquals("4", stats.quizCount)
        assertEquals("5", stats.quickCheckCount)
    }

    @Test
    fun chapterStats_doesNotFallBackToOverallTotalsForAnotherSubject() {
        val stats = StudentUiMappers.chapterStats(
            overview = sampleOverview(
                extraSubject = ProgressSubjectOverview(
                    textbook_id = "tb2",
                    subject_id = "sub2",
                    progress_pct = 10.0,
                    overall_mastery_pct = 11.0,
                ),
            ),
            subjectId = "missing",
        )
        assertNull(stats)
    }

    private fun sampleOverview(
        extraSubject: ProgressSubjectOverview? = null,
    ): ProgressOverviewResponse = ProgressOverviewResponse(
        scope = "self",
        ai_available = true,
        self_overview = SelfProgressOverview(
            totals = ProgressOverviewTotals(
                overall_mastery_pct = 62.4,
                overall_quiz_mastery_pct = 55.0,
                overall_progress_pct = 48.0,
                quizzes_completed = 4,
                quick_check_attempts = 9,
            ),
            subjects = listOfNotNull(
                ProgressSubjectOverview(
                    textbook_id = "tb1",
                    subject_id = "sub1",
                    progress_pct = 40.0,
                    overall_mastery_pct = 58.0,
                    quiz_mastery_pct = 50.0,
                    quick_check_attempts = 5,
                    chapters = listOf(
                        ProgressChapterOverview(
                            chapter_id = "ch1",
                            quiz_mastery_pct = 60.0,
                        ),
                    ),
                ),
                extraSubject,
            ),
        ),
    )
}
