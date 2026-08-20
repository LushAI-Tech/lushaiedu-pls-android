package com.lushaiedupls.data.remote

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

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
    is NetworkResult.Error -> apiDetailMessage(body)
        ?: message.ifBlank { "Request failed ($code)" }
    is NetworkResult.Exception -> when (throwable) {
        is SocketTimeoutException ->
            "The server took too long to respond. Please try again."
        else -> throwable.localizedMessage ?: "Network error"
    }
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

private fun apiDetailMessage(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return Regex("\"detail\"\\s*:\\s*\"([^\"]+)\"")
        .find(body)
        ?.groupValues
        ?.getOrNull(1)
        ?.takeIf { it.isNotBlank() }
}

suspend fun <T> safeApiCall(call: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(call())
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
