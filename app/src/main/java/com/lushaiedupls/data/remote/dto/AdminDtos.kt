package com.lushaiedupls.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class AdminOverview(
    val month: String,
    val total_students: Int = 0,
    val total_teachers: Int = 0,
    val total_parents: Int = 0,
    val total_classes: Int = 0,
    val total_subjects: Int = 0,
    val subjects_ai_enabled: Int = 0,
    val total_units: Int = 0,
    val units_without_teacher: Int = 0,
    val attendance: AttendanceTotals,
)

@Serializable
data class ClassCreate(
    val name: String,
    val sort_order: Int = 0,
    val is_active: Boolean = true,
)

@Serializable
data class ClassUpdate(
    val name: String? = null,
    val sort_order: Int? = null,
    val is_active: Boolean? = null,
)

@Serializable
data class SubjectCreate(
    val class_id: String,
    val name: String,
    val code: String? = null,
    val sort_order: Int = 0,
    val is_active: Boolean = true,
    val stem_subject_id: String? = null,
)

@Serializable
data class SubjectUpdate(
    val name: String? = null,
    val code: String? = null,
    val sort_order: Int? = null,
    val is_active: Boolean? = null,
)

@Serializable(with = StemBindingRequest.Serializer::class)
data class StemBindingRequest(
    val stem_subject_id: String?,
) {
    object Serializer : KSerializer<StemBindingRequest> {
        override val descriptor = buildClassSerialDescriptor("StemBindingRequest") {
            element<String?>("stem_subject_id")
        }

        override fun serialize(encoder: Encoder, value: StemBindingRequest) {
            val output = encoder as JsonEncoder
            output.encodeJsonElement(
                buildJsonObject {
                    val id = value.stem_subject_id
                    if (id == null) put("stem_subject_id", JsonNull)
                    else put("stem_subject_id", JsonPrimitive(id))
                },
            )
        }

        override fun deserialize(decoder: Decoder): StemBindingRequest {
            val input = decoder as JsonDecoder
            val element = input.decodeJsonElement().jsonObject["stem_subject_id"]
            val id = if (element == null || element is JsonNull) null
            else element.jsonPrimitive.content
            return StemBindingRequest(id)
        }
    }
}

@Serializable
data class StemNode(
    val id: String,
    val name: String,
    val code: String? = null,
    val sort_order: Int? = null,
)

@Serializable
data class InviteCodeCreate(
    val role: UserRole,
    val note: String? = null,
    val expires_in_days: Int? = null,
)

@Serializable
data class InviteCodeOut(
    val id: String,
    val code: String,
    val role: UserRole,
    val note: String? = null,
    val expires_at: String,
    val used_at: String? = null,
    val used_by: String? = null,
    val revoked_at: String? = null,
    val created_at: String,
    val is_redeemable: Boolean = false,
)

@Serializable
data class UserStatusUpdate(
    val status: UserStatus,
)

@Serializable
data class ClassReassignRequest(
    val class_id: String,
)

@Serializable
data class PeriodCreate(
    val name: String,
    val start_time: String,
    val end_time: String,
    val sort_order: Int = 0,
    val is_active: Boolean = true,
)

@Serializable
data class PeriodUpdate(
    val name: String? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val sort_order: Int? = null,
    val is_active: Boolean? = null,
)

@Serializable
data class DeletedResponse(
    val deleted: Boolean = true,
    val id: String,
)

@Serializable
data class AdminFeedbackUpdateRequest(
    val status: FeedbackStatus? = null,
    val admin_notes: String? = null,
)

@Serializable
data class ClassMonthlyFeeUpsertRequest(
    val class_id: String,
    val month: String,
    val amount_paise: Int,
)

@Serializable
data class ClassMonthlyFeeOut(
    val id: String,
    val class_id: String,
    val month: String,
    val amount_paise: Int,
    val created_at: String,
    val updated_at: String,
)

@Serializable
data class LedgerGenerationRequest(
    val month: String,
)

@Serializable
data class FeeLedgerUpdateRequest(
    val payment_status: FeePaymentStatus,
    val amount_paise: Int,
)

@Serializable
data class AdminFeeSummaryOut(
    val month: String,
    val class_id: String? = null,
    val total_students: Int,
    val paid_students: Int,
    val not_paid_students: Int,
    val total_amount_paise: Int,
    val paid_amount_paise: Int,
    val pending_amount_paise: Int,
)

@Serializable
data class CalendarEventCreate(
    val title: String,
    val event_type: CalendarEventType,
    val start_date: String,
    val end_date: String,
    val description: String? = null,
)

@Serializable
data class CalendarEventUpdate(
    val title: String? = null,
    val event_type: CalendarEventType? = null,
    val start_date: String? = null,
    val end_date: String? = null,
    val description: String? = null,
)

@Serializable
data class PaginatedUsersResponse(
    val items: List<UserOut> = emptyList(),
    val page: Int,
    val limit: Int,
    val total: Int,
    val total_pages: Int,
)

@Serializable
data class AdminUserCreateRequest(
    val name: String,
    val phone: String,
    val role: UserRole,
    val dob: String,
    val email: String? = null,
    val gender: Gender? = null,
    val address: String? = null,
    val class_id: String? = null,
)

@Serializable
data class AdminUserCreateResponse(
    val user: UserOut,
    val temporary_password: String,
)

@Serializable
data class AdminUserEditRequest(
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val gender: Gender? = null,
    val address: String? = null,
    val role: UserRole? = null,
    val status: UserStatus? = null,
    val class_id: String? = null,
)
