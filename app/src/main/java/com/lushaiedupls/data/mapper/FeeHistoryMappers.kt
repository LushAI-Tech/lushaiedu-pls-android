package com.lushaiedupls.data.mapper

import com.lushaiedupls.data.remote.dto.FeeHistoryResponse
import com.lushaiedupls.data.remote.dto.FeeLedgerOut
import com.lushaiedupls.data.remote.dto.FeePaymentStatus

data class FeeMonthGroup(
    val month: String,
    val rows: List<FeeLedgerOut>,
    val totalPaise: Int,
    val paidPaise: Int,
    val pendingPaise: Int,
)

object FeeHistoryMappers {
    fun monthGroups(rows: List<FeeLedgerOut>): List<FeeMonthGroup> {
        return rows.groupBy { it.month }
            .toSortedMap(compareByDescending { it })
            .map { (month, monthRows) ->
                val sorted = monthRows.sortedWith(
                    compareBy<FeeLedgerOut> { it.payment_status == FeePaymentStatus.PAID }
                        .thenBy { it.subject_name.orEmpty() }
                        .thenBy { it.class_name.orEmpty() },
                )
                FeeMonthGroup(
                    month = month,
                    rows = sorted,
                    totalPaise = sorted.sumOf { it.amount_paise },
                    paidPaise = sorted.filter { it.payment_status == FeePaymentStatus.PAID }
                        .sumOf { it.amount_paise },
                    pendingPaise = sorted.filter { it.payment_status != FeePaymentStatus.PAID }
                        .sumOf { it.amount_paise },
                )
            }
    }

    /**
     * Student fee history shows only subject-wise ledger rows. Legacy rows without
     * a subject name fall back to the month as the card title and are excluded.
     */
    fun subjectFeeHistoryOnly(history: FeeHistoryResponse): FeeHistoryResponse {
        val rows = history.rows.filter(::hasSubjectTitle)
        return history.copy(
            rows = rows,
            total_amount_paise = rows.sumOf { it.amount_paise },
            paid_amount_paise = rows.filter { it.payment_status == FeePaymentStatus.PAID }
                .sumOf { it.amount_paise },
            pending_amount_paise = rows.filter { it.payment_status != FeePaymentStatus.PAID }
                .sumOf { it.amount_paise },
        )
    }

    fun hasSubjectTitle(row: FeeLedgerOut): Boolean = subjectLabel(row).isNotBlank()

    fun subjectLabel(row: FeeLedgerOut, subjectCode: String? = null): String {
        val name = row.subject_name?.takeIf { it.isNotBlank() }
        val code = subjectCode?.takeIf { it.isNotBlank() }
        return listOfNotNull(name, code).joinToString(" · ")
    }
}
