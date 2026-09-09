package com.lushaiedupls.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeeMonthTest {

    @Test
    fun yearMonth_acceptsValidValues() {
        assertTrue(FeeMonth.isYearMonth("2026-08"))
        assertTrue(FeeMonth.isYearMonth(" 2026-01 "))
        assertEquals("2026-12", FeeMonth.yearMonthOrNull("2026-12"))
    }

    @Test
    fun yearMonth_rejectsInvalidValues() {
        assertFalse(FeeMonth.isYearMonth("2026-13"))
        assertFalse(FeeMonth.isYearMonth("26-08"))
        assertFalse(FeeMonth.isYearMonth("all"))
        assertFalse(FeeMonth.isYearMonth("2026/08"))
        assertNull(FeeMonth.yearMonthOrNull("all"))
    }

    @Test
    fun listFilter_allowsAllAndYearMonth() {
        assertTrue(FeeMonth.isListFilter(null))
        assertTrue(FeeMonth.isListFilter(""))
        assertTrue(FeeMonth.isListFilter("all"))
        assertTrue(FeeMonth.isListFilter("ALL"))
        assertTrue(FeeMonth.isListFilter("2026-08"))
        assertFalse(FeeMonth.isListFilter("2026-13"))
        assertEquals(FeeMonth.ALL, FeeMonth.listFilterOrNull("ALL"))
        assertEquals("2026-08", FeeMonth.listFilterOrNull("2026-08"))
        assertNull(FeeMonth.listFilterOrNull("2026-13"))
        assertNull(FeeMonth.listFilterOrNull(null))
    }

    @Test
    fun invalidMonthError_is400() {
        val error = FeeMonth.invalidMonthError()
        assertEquals(400, error.code)
        assertEquals(FeeMonth.INVALID_MESSAGE, error.message)
    }

    @Test
    fun bulkDeleteResultMessage_keepsApiCopyWhenCountsPresent() {
        val message = FeeMonth.bulkDeleteResultMessage(
            apiMessage = "Deleted 8 of 10 matched ledger rows for 2026-09.",
            matchedCount = 10,
            deletedCount = 8,
            month = "2026-09",
        )
        assertEquals("Deleted 8 of 10 matched ledger rows for 2026-09.", message)
    }

    @Test
    fun bulkDeleteResultMessage_appendsCountsWhenMissing() {
        val message = FeeMonth.bulkDeleteResultMessage(
            apiMessage = "Rollback complete.",
            matchedCount = 10,
            deletedCount = 8,
            month = "2026-09",
        )
        assertEquals("Rollback complete. (matched 10, deleted 8)", message)
    }
}
