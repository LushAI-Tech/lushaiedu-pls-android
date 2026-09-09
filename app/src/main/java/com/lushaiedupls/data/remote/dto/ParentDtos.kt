package com.lushaiedupls.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FeeLedgerOut(
    val id: String,
    val student: UserSummary,
    val class_id: String? = null,
    val class_name: String? = null,
    val subject_id: String? = null,
    val subject_name: String? = null,
    val month: String,
    val amount_paise: Int,
    val payment_status: FeePaymentStatus,
    val paid_at: String? = null,
    val updated_at: String,
)

@Serializable
data class FeeHistoryResponse(
    val month: String,
    val rows: List<FeeLedgerOut> = emptyList(),
    val total_amount_paise: Int = 0,
    val paid_amount_paise: Int = 0,
    val pending_amount_paise: Int = 0,
)

@Serializable
data class ParentFeedbackOut(
    val id: String,
    val parent: UserSummary,
    val student: UserSummary? = null,
    val subject: String,
    val message: String,
    val status: FeedbackStatus,
    val admin_notes: String? = null,
    val closed_at: String? = null,
    val created_at: String,
    val updated_at: String,
)

@Serializable
data class ParentFeedbackCreateRequest(
    val student_id: String? = null,
    val subject: String,
    val message: String,
)

@Serializable
data class ParentFeedbackUpdateRequest(
    val student_id: String? = null,
    val subject: String? = null,
    val message: String? = null,
)
