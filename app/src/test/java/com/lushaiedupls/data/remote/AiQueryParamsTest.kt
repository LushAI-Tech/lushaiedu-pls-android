package com.lushaiedupls.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiQueryParamsTest {

    @Test
    fun commaSeparatedIds_joinsNonEmptyDistinctValues() {
        assertEquals(
            "id-1,id-2",
            AiQueryParams.commaSeparatedIds(listOf("id-1", " ", "id-2", "id-1")),
        )
        assertNull(AiQueryParams.commaSeparatedIds(listOf("  ", "")))
        assertNull(AiQueryParams.commaSeparatedIds(emptyList()))
    }

    @Test
    fun subtopicIdsQuery_omittedForEmptyOrAllChapterSelection() {
        val all = listOf("a", "b")
        assertNull(AiQueryParams.subtopicIdsQuery(emptyList(), all))
        assertNull(AiQueryParams.subtopicIdsQuery(all, all))
        assertEquals("a", AiQueryParams.subtopicIdsQuery(listOf("a"), all))
        assertEquals("a,b", AiQueryParams.subtopicIdsQuery(listOf("a", "b"), listOf("a", "b", "c")))
    }

    @Test
    fun examPrepQuery_allTopicsUsesChapterScopeWithoutSubtopicIds() {
        val query = AiQueryParams.examPrepQuery(
            selectedSubtopicIds = emptyList(),
            allSubtopicIds = listOf("s1", "s2"),
        )
        assertEquals(true, query.chapterScope)
        assertNull(query.subtopicIds)
        assertNull(query.sectionId)
    }

    @Test
    fun examPrepQuery_specificSubtopicsSendsIdsAndDisablesChapterScope() {
        val query = AiQueryParams.examPrepQuery(
            selectedSubtopicIds = listOf("s2", "s1"),
            allSubtopicIds = listOf("s1", "s2", "s3"),
        )
        assertEquals(false, query.chapterScope)
        assertEquals("s2,s1", query.subtopicIds)
        assertNull(query.sectionId)
    }

    @Test
    fun examPrepQuery_singleSectionModeSkipsSubtopicIds() {
        val query = AiQueryParams.examPrepQuery(
            selectedSubtopicIds = listOf("s1", "s2"),
            allSubtopicIds = listOf("s1", "s2", "s3"),
            singleSectionId = "s1",
        )
        assertEquals("s1", query.sectionId)
        assertNull(query.chapterScope)
        assertNull(query.subtopicIds)
    }

    @Test
    fun isChapterScope_trueWhenAllSelected() {
        assertTrue(AiQueryParams.isChapterScope(listOf("a", "b"), listOf("b", "a")))
        assertFalse(AiQueryParams.isChapterScope(listOf("a"), listOf("a", "b")))
    }
}
