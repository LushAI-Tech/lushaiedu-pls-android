package com.lushaiedupls.data.remote.dto

import com.lushaiedupls.data.mapper.FeeHistoryMappers
import com.lushaiedupls.data.remote.ApiClient
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeeSubjectContractTest {

    private val json = ApiClient.json

    @Test
    fun subjectMonthlyFee_encodesRequiredFields() {
        val body = SubjectMonthlyFeeUpsertRequest(
            subject_id = "sub-1",
            month = "2026-08",
            amount_paise = 50_000,
        )
        val parsed = json.parseToJsonElement(
            json.encodeToString(SubjectMonthlyFeeUpsertRequest.serializer(), body),
        ).jsonObject
        assertEquals("sub-1", parsed["subject_id"]!!.jsonPrimitive.content)
        assertEquals("2026-08", parsed["month"]!!.jsonPrimitive.content)
        assertEquals("50000", parsed["amount_paise"]!!.jsonPrimitive.content)
    }

    @Test
    fun subjectMonthlyFeeOut_decodesListFields() {
        val decoded = json.decodeFromString(
            SubjectMonthlyFeeOut.serializer(),
            """
            {
              "id": "fee-1",
              "subject_id": "sub-1",
              "class_id": "c1",
              "class_name": "X",
              "subject_name": "Physics",
              "month": "2026-08",
              "amount_paise": 50000,
              "created_at": "2026-08-01T00:00:00Z",
              "updated_at": "2026-08-02T00:00:00Z"
            }
            """.trimIndent(),
        )
        assertEquals("fee-1", decoded.id)
        assertEquals("sub-1", decoded.subject_id)
        assertEquals("c1", decoded.class_id)
        assertEquals("X", decoded.class_name)
        assertEquals("Physics", decoded.subject_name)
        assertEquals("2026-08", decoded.month)
        assertEquals(50_000, decoded.amount_paise)
    }

    @Test
    fun feeHistory_decodesTotalsAndSubjectFields() {
        val decoded = json.decodeFromString(
            FeeHistoryResponse.serializer(),
            """
            {
              "month": "all",
              "rows": [{
                "id": "l1",
                "student": {"id": "s1", "name": "Ava"},
                "class_id": "c1",
                "class_name": "X",
                "subject_id": "sub-1",
                "subject_name": "Physics",
                "month": "2026-08",
                "amount_paise": 50000,
                "payment_status": "paid",
                "updated_at": "2026-08-01T00:00:00Z"
              }],
              "total_amount_paise": 50000,
              "paid_amount_paise": 50000,
              "pending_amount_paise": 0
            }
            """.trimIndent(),
        )
        assertEquals(50_000, decoded.total_amount_paise)
        assertEquals(50_000, decoded.paid_amount_paise)
        assertEquals(0, decoded.pending_amount_paise)
        assertEquals("sub-1", decoded.rows.single().subject_id)
        assertEquals("Physics", decoded.rows.single().subject_name)
    }

    @Test
    fun monthGroups_keepsPartialPaymentsSeparate() {
        val student = UserSummary(id = "s1", name = "Ava")
        val rows = listOf(
            ledger(student, "2026-08", "Physics", 50_000, FeePaymentStatus.PAID),
            ledger(student, "2026-08", "Chemistry", 40_000, FeePaymentStatus.NOT_PAID),
            ledger(student, "2026-07", "Physics", 50_000, FeePaymentStatus.PAID),
        )
        val groups = FeeHistoryMappers.monthGroups(rows)
        assertEquals(listOf("2026-08", "2026-07"), groups.map { it.month })
        val august = groups.first()
        assertEquals(90_000, august.totalPaise)
        assertEquals(50_000, august.paidPaise)
        assertEquals(40_000, august.pendingPaise)
        assertEquals(listOf("Chemistry", "Physics"), august.rows.map { it.subject_name })
        assertTrue(august.rows.first().payment_status == FeePaymentStatus.NOT_PAID)
    }

    @Test
    fun subjectFeeHistoryOnly_keepsOnlyRowsWithSubjectTitles() {
        val student = UserSummary(id = "s1", name = "Ava")
        val history = FeeHistoryResponse(
            month = "all",
            rows = listOf(
                ledger(student, "2026-08", "Physics", 50_000, FeePaymentStatus.NOT_PAID),
                ledger(student, "2026-07", null, 50_000, FeePaymentStatus.NOT_PAID),
                ledger(student, "2026-06", "Chemistry", 40_000, FeePaymentStatus.PAID),
            ),
            total_amount_paise = 140_000,
            paid_amount_paise = 40_000,
            pending_amount_paise = 100_000,
        )

        val filtered = FeeHistoryMappers.subjectFeeHistoryOnly(history)

        assertEquals(listOf("Physics", "Chemistry"), filtered.rows.map { it.subject_name })
        assertEquals(90_000, filtered.total_amount_paise)
        assertEquals(40_000, filtered.paid_amount_paise)
        assertEquals(50_000, filtered.pending_amount_paise)
    }

    private fun ledger(
        student: UserSummary,
        month: String,
        subject: String?,
        amount: Int,
        status: FeePaymentStatus,
    ) = FeeLedgerOut(
        id = "$month-${subject ?: "flat"}",
        student = student,
        month = month,
        amount_paise = amount,
        payment_status = status,
        updated_at = "2026-08-01T00:00:00Z",
        subject_name = subject,
    )

    @Test
    fun ledgerBulkDelete_encodesOptionalFiltersAndIncludePaid() {
        val body = FeeLedgerBulkDeleteRequest(
            month = "2026-09",
            class_id = "c1",
            subject_id = "sub-1",
            include_paid = false,
        )
        val parsed = json.parseToJsonElement(
            json.encodeToString(FeeLedgerBulkDeleteRequest.serializer(), body),
        ).jsonObject
        assertEquals("2026-09", parsed["month"]!!.jsonPrimitive.content)
        assertEquals("c1", parsed["class_id"]!!.jsonPrimitive.content)
        assertEquals("sub-1", parsed["subject_id"]!!.jsonPrimitive.content)
        assertEquals("false", parsed["include_paid"]!!.jsonPrimitive.content)
        assertEquals(false, parsed.containsKey("student_id"))
    }

    @Test
    fun ledgerBulkDelete_decodesCounts() {
        val decoded = json.decodeFromString(
            FeeLedgerBulkDeleteResponse.serializer(),
            """
            {
              "message": "Deleted 8 of 10 matched ledger rows for 2026-09.",
              "matched_count": 10,
              "deleted_count": 8
            }
            """.trimIndent(),
        )
        assertEquals("Deleted 8 of 10 matched ledger rows for 2026-09.", decoded.message)
        assertEquals(10, decoded.matched_count)
        assertEquals(8, decoded.deleted_count)
    }
}
