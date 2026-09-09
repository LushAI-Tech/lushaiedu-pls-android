package com.lushaiedupls.data.remote

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(
        val code: Int,
        val message: String,
        val body: String? = null,
    ) : NetworkResult<Nothing>
    data class Exception(val throwable: Throwable) : NetworkResult<Nothing>
}

/**
 * Wraps a Retrofit suspend call so ViewModels can branch on success / HTTP error / connectivity
 * without repeating try/catch.
 */
fun NetworkResult<*>.userMessage(): String = when (this) {
    is NetworkResult.Success -> ""
    is NetworkResult.Error -> institutionAwareMessage(code, body)
        ?: aiScopeAwareMessage(code, body)
        ?: stemBindingUnavailableMessage(code, body, message)
        ?: googlePasswordLinkBlockedMessage(code, body)
        ?: accountAlreadyExistsMessage(code, body)
        ?: feeTemplateConflictMessage(code, body)
        ?: feeTemplateNotFoundMessage(code, body)
        ?: apiDetailMessage(body)
        ?: message.ifBlank { "Request failed ($code)" }
    is NetworkResult.Exception -> when (throwable) {
        is CancellationException -> ""
        is SocketTimeoutException ->
            "The server took too long to respond. Please try again."
        else -> {
            val msg = throwable.localizedMessage.orEmpty()
            if (msg.contains("was cancelled", ignoreCase = true)) {
                ""
            } else {
                msg.ifBlank { "Network error" }
            }
        }
    }
}

/** True when register/login conflict indicates the account is already registered. */
fun NetworkResult<*>.isAccountAlreadyExists(): Boolean {
    if (this !is NetworkResult.Error) return false
    return accountAlreadyExistsMessage(code, body) != null
}

/**
 * True when Google sign-in is blocked because a password account exists and email
 * is not verified yet (backend refuses to attach google_sub until verify).
 */
fun NetworkResult<*>.isGooglePasswordLinkBlocked(): Boolean {
    if (this !is NetworkResult.Error) return false
    return googlePasswordLinkBlockedMessage(code, body) != null
}

/** 400/404 from chapter/subtopic-scoped AI endpoints. */
fun NetworkResult<*>.aiScopeUserMessage(): String {
    if (this is NetworkResult.Error && code in setOf(400, 404)) {
        return AiQueryParams.SCOPE_RESELECT_MESSAGE
    }
    return userMessage()
}

/** True when the API blocked this student until an admin approves the account. */
fun NetworkResult<*>.needsAdminApproval(): Boolean {
    if (this !is NetworkResult.Error) return false
    if (code == 403) return true
    val text = listOfNotNull(message, body, apiDetailMessage(body))
        .joinToString(" ")
        .lowercase()
    return listOf(
        "forbidden",
        "not approved",
        "pending_approval",
        "pending approval",
        "awaiting approval",
        "approval required",
    ).any { it in text }
}

private fun institutionAwareMessage(code: Int, body: String?): String? {
    val detail = apiDetailMessage(body).orEmpty()
    val combined = listOfNotNull(detail, body).joinToString(" ").lowercase()
    if (code == 400 && combined.contains("does not belong to the selected institution")) {
        return detail.ifBlank { "Selected class does not belong to the selected institution." }
    }
    if (code == 422 && (combined.contains("institution_id") || locIncludesInstitutionId(body))) {
        return "Select an institution first."
    }
    return null
}

private fun aiScopeAwareMessage(code: Int, body: String?): String? {
    if (code != 400 && code != 404) return null
    val combined = listOfNotNull(apiDetailMessage(body), body).joinToString(" ").lowercase()
    val mentionsScope = listOf(
        "subtopic",
        "section",
        "chapter_scope",
        "uuid",
        "invalid uuid",
        "not found under",
    ).any { it in combined }
    return if (mentionsScope) AiQueryParams.SCOPE_RESELECT_MESSAGE else null
}

private fun stemBindingUnavailableMessage(code: Int, body: String?, message: String = ""): String? {
    val detail = apiDetailMessage(body).orEmpty()
    val combined = listOfNotNull(detail, body, message).joinToString(" ")
    return friendlyStemBindingMessage(combined)
}

/** Rewrites STEM-binding / unbound-subject API copy into a student-friendly message. */
fun friendlyStemBindingMessage(raw: String): String? {
    if (raw.isBlank()) return null
    val combined = raw.lowercase()
    val unbound = combined.contains("stem-binding") ||
        combined.contains("no ai content bound") ||
        combined.contains("ai content bound yet") ||
        (combined.contains("/api/v1/admin/subjects") && combined.contains("stem"))
    if (!unbound) return null
    val subject = Regex("'([^']+)'\\s+has no ai content", RegexOption.IGNORE_CASE)
        .find(raw)
        ?.groupValues
        ?.getOrNull(1)
        ?.takeIf { it.isNotBlank() }
        ?: Regex("\"([^\"]+)\"\\s+has no ai content", RegexOption.IGNORE_CASE)
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }
    return if (subject != null) {
        "$subject isn’t set up for AI learning yet. Please check back later."
    } else {
        STEM_BINDING_UNAVAILABLE_MESSAGE
    }
}

const val STEM_BINDING_UNAVAILABLE_MESSAGE =
    "AI lessons for this subject aren’t available yet. Please check back later."

private fun googlePasswordLinkBlockedMessage(code: Int, body: String?): String? {
    if (code != 409) return null
    val detail = apiDetailMessage(body).orEmpty()
    val combined = listOfNotNull(detail, body).joinToString(" ").lowercase()
    if (combined.isBlank()) return null
    val feeContext = listOf("ledger", "fee template", "fee_template", "subject-monthly")
        .any { it in combined }
    if (feeContext) return null
    val mentionsPassword = "password" in combined
    val mentionsVerifyGate = listOf(
        "verify email",
        "verify your email",
        "email_verified",
        "email verified",
        "not verified",
        "unverified",
        "sign in with password",
        "exists with a password",
        "account exists with a password",
        "with a password",
    ).any { it in combined }
    if (!mentionsPassword || !mentionsVerifyGate) return null
    return GOOGLE_PASSWORD_LINK_BLOCKED_MESSAGE
}

private fun accountAlreadyExistsMessage(code: Int, body: String?): String? {
    if (code !in setOf(400, 409, 422)) return null
    if (googlePasswordLinkBlockedMessage(code, body) != null) return null
    val detail = apiDetailMessage(body).orEmpty()
    val combined = listOfNotNull(detail, body).joinToString(" ").lowercase()
    if (combined.isBlank()) {
        return if (code == 409) ACCOUNT_ALREADY_EXISTS_MESSAGE else null
    }
    val feeContext = listOf("ledger", "fee template", "fee_template", "subject-monthly")
        .any { it in combined }
    if (feeContext) return null
    val exists = listOf(
        "already exist",
        "already registered",
        "already in use",
        "email already",
        "phone already",
        "user already",
        "account already",
        "duplicate",
        "taken",
    ).any { it in combined }
    if (!exists) return null
    return when {
        "phone" in combined && "email" !in combined -> ACCOUNT_ALREADY_EXISTS_PHONE_MESSAGE
        "email" in combined -> ACCOUNT_ALREADY_EXISTS_EMAIL_MESSAGE
        else -> ACCOUNT_ALREADY_EXISTS_MESSAGE
    }
}

const val GOOGLE_PASSWORD_LINK_BLOCKED_MESSAGE =
    "An account with this email already exists with a password. Sign in with your password, verify your email, then Sign in with Google to link."
const val ACCOUNT_ALREADY_EXISTS_MESSAGE =
    "An account with these details already exists. Please sign in instead."
const val ACCOUNT_ALREADY_EXISTS_EMAIL_MESSAGE =
    "An account with this email already exists. Please sign in instead."
const val ACCOUNT_ALREADY_EXISTS_PHONE_MESSAGE =
    "An account with this phone number already exists. Please sign in instead."

private fun feeTemplateConflictMessage(code: Int, body: String?): String? {
    if (code != 409) return null
    val combined = listOfNotNull(apiDetailMessage(body), body).joinToString(" ").lowercase()
    val mentionsFeeContext = combined.contains("ledger") ||
        combined.contains("fee") ||
        combined.contains("template") ||
        combined.contains("locked")
    if (!mentionsFeeContext) return null
    return if (combined.contains("already exist") ||
        combined.contains("ledger") ||
        combined.contains("locked") ||
        combined.isBlank()
    ) {
        FeeMonth.TEMPLATE_LOCKED_MESSAGE
    } else {
        apiDetailMessage(body)
    }
}

fun NetworkResult<*>.parentLinkAssignUserMessage(): String {
    if (this !is NetworkResult.Error) return userMessage()
    val detail = apiDetailMessage(body)
    return when (code) {
        400 -> detail
            ?: "Parent and student roles are required. Check that each user has the correct role."
        404 -> detail ?: "One of the selected users was not found."
        409 -> detail
            ?: "Cannot link these users. They may already be linked, the parent may be inactive, or the student may still need approval."
        403 -> detail ?: "Admin access is required to assign parent–student links."
        else -> userMessage()
    }
}

private fun feeTemplateNotFoundMessage(code: Int, body: String?): String? {
    if (code != 404) return null
    val combined = listOfNotNull(apiDetailMessage(body), body).joinToString(" ").lowercase()
    val mentionsFee = combined.contains("fee") ||
        combined.contains("template") ||
        combined.contains("subject-monthly") ||
        combined.contains("ledger")
    return if (mentionsFee || combined.isBlank()) {
        FeeMonth.TEMPLATE_NOT_FOUND_MESSAGE
    } else {
        null
    }
}

private fun locIncludesInstitutionId(body: String?): Boolean {
    if (body.isNullOrBlank()) return false
    return try {
        val detail = ApiClient.json.parseToJsonElement(body).jsonObject["detail"] as? JsonArray
            ?: return false
        detail.any { item ->
            val loc = (item as? JsonObject)?.get("loc") as? JsonArray ?: return@any false
            loc.any { (it as? JsonPrimitive)?.contentOrNull == "institution_id" }
        }
    } catch (_: Exception) {
        false
    }
}

private fun apiDetailMessage(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return try {
        val detail = ApiClient.json.parseToJsonElement(body).jsonObject["detail"] ?: return null
        when (detail) {
            is JsonPrimitive -> detail.contentOrNull?.takeIf { it.isNotBlank() }
            is JsonArray -> detail.firstNotNullOfOrNull { item ->
                val obj = item as? JsonObject ?: return@firstNotNullOfOrNull null
                (obj["msg"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            }
            else -> null
        }
    } catch (_: Exception) {
        Regex("\"detail\"\\s*:\\s*\"([^\"]+)\"")
            .find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }
    }
}

suspend fun <T> safeApiCall(call: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(call())
    } catch (e: CancellationException) {
        // Cooperative cancel must not become a UI error (e.g. refresh() cancelling a prior load).
        throw e
    } catch (e: HttpException) {
        NetworkResult.Error(
            code = e.code(),
            message = e.message(),
            body = e.response()?.errorBody()?.string(),
        )
    } catch (e: IOException) {
        NetworkResult.Exception(e)
    } catch (e: Throwable) {
        NetworkResult.Exception(e)
    }
}
