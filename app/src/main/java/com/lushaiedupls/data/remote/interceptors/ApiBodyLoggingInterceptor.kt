package com.lushaiedupls.data.remote.interceptors

import android.util.Log
import com.lushaiedupls.data.remote.ApiHttpLogger
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject

/**
 * Debug-only interceptor that prints clearly labeled request/response bodies.
 * Filter Logcat by tag [ApiHttpLogger.TAG] (`LushApi`).
 */
class ApiBodyLoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBodyText = request.body?.let(::readRequestBody)

        logBlock(
            title = "REQUEST BODY",
            lines = listOf(
                "${request.method} ${request.url}",
                formatBody(requestBodyText, request.body?.contentType()),
            ),
        )

        val response = chain.proceed(request)
        val responseBody = response.body
        val contentType = responseBody?.contentType()
        val responseBodyText = responseBody?.string().orEmpty()

        logBlock(
            title = "RESPONSE BODY",
            lines = listOf(
                "${response.code} ${request.method} ${request.url}",
                formatBody(responseBodyText, contentType),
            ),
        )

        // Body can only be consumed once — rebuild so Retrofit can still read it.
        val rebuilt = responseBodyText.toResponseBody(contentType)
        return response.newBuilder().body(rebuilt).build()
    }

    private fun readRequestBody(body: RequestBody): String {
        return try {
            val buffer = Buffer()
            body.writeTo(buffer)
            val charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
            buffer.readString(charset)
        } catch (e: Exception) {
            "<unable to read request body: ${e.message}>"
        }
    }

    private fun formatBody(raw: String?, contentType: MediaType?): String {
        if (raw.isNullOrBlank()) return "<empty>"
        val isJson = contentType?.subtype?.contains("json", ignoreCase = true) == true ||
            raw.trimStart().startsWith("{") ||
            raw.trimStart().startsWith("[")
        return if (isJson) prettyJson(raw) else raw
    }

    private fun prettyJson(raw: String): String {
        val trimmed = raw.trim()
        return try {
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                else -> raw
            }
        } catch (_: Exception) {
            raw
        }
    }

    private fun logBlock(title: String, lines: List<String>) {
        val message = buildString {
            appendLine("┌── $title ──")
            lines.forEach { line ->
                line.lineSequence().forEach { appendLine("│ $it") }
            }
            append("└────────────")
        }
        chunkLog(message)
    }

    private fun chunkLog(message: String) {
        if (message.length <= MAX_LOG_CHUNK) {
            Log.d(ApiHttpLogger.TAG, message)
            return
        }
        var start = 0
        while (start < message.length) {
            val end = minOf(start + MAX_LOG_CHUNK, message.length)
            Log.d(ApiHttpLogger.TAG, message.substring(start, end))
            start = end
        }
    }

    private companion object {
        const val MAX_LOG_CHUNK = 3500
    }
}
