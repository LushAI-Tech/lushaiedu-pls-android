package com.lushaiedupls.data.remote

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

data class ActiveDeviceSession(
    val accountLabel: String? = null,
    val deviceName: String? = null,
    val platform: String? = null,
    val lastActive: String? = null,
)

data class DeviceSessionConflict(
    val message: String,
    val devices: List<ActiveDeviceSession> = emptyList(),
    val accountLabel: String? = null,
    /** Present on login/Google 409 when the client can call device-conflict/resolve. */
    val conflictToken: String? = null,
)

data class PendingSignInNotice(
    val message: String,
    val conflict: DeviceSessionConflict? = null,
    val googleIdToken: String? = null,
)

fun NetworkResult<*>.deviceSessionConflictOrNull(): DeviceSessionConflict? {
    if (this !is NetworkResult.Error || !isDeviceSessionConflict()) return null
    val parsed = parseDeviceSessionConflict(body)
    if (parsed != null) return parsed
    val detail = apiDetailFromBody(body)?.trim()?.takeIf { it.isNotBlank() }
        ?: DEVICE_SESSION_CONFLICT_FALLBACK
    return DeviceSessionConflict(message = detail)
}

internal fun extractConflictToken(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return try {
        val root = ApiClient.json.parseToJsonElement(body).jsonObject
        val detailObject = root["detail"] as? JsonObject
        stringValue(detailObject, "conflict_token", "conflictToken")
            ?: stringValue(root, "conflict_token", "conflictToken")
    } catch (_: Exception) {
        null
    } ?: Regex("\"conflict_token\"\\s*:\\s*\"([^\"]+)\"")
        .find(body)
        ?.groupValues
        ?.getOrNull(1)
        ?.takeIf { it.isNotBlank() }
}

internal fun parseDeviceSessionConflict(body: String?): DeviceSessionConflict? {
    if (body.isNullOrBlank()) return null
    return try {
        val root = ApiClient.json.parseToJsonElement(body).jsonObject
        val detailEl = root["detail"]
        val detailObject = detailEl as? JsonObject
        val message = stringValue(detailEl)
            ?: stringValue(detailObject, "message", "detail", "msg")
            ?: stringValue(root, "message", "detail", "msg")
            ?: DEVICE_SESSION_CONFLICT_FALLBACK
        val conflictToken = extractConflictToken(body)
        val account = stringValue(root, "email", "account", "identifier", "user_email")
            ?: stringValue(detailObject, "email", "account", "identifier", "user_email")
        val deviceArrays = listOfNotNull(
            root.array("devices"),
            root.array("active_devices"),
            root.array("sessions"),
            detailObject?.array("devices"),
            detailObject?.array("active_devices"),
            detailObject?.array("sessions"),
        )
        val devices = deviceArrays.firstNotNullOfOrNull { arr ->
            arr.mapNotNull(::deviceFromJson).takeIf { it.isNotEmpty() }
        }.orEmpty()
        DeviceSessionConflict(
            message = message.trim(),
            devices = devices,
            accountLabel = account,
            conflictToken = conflictToken,
        )
    } catch (_: Exception) {
        null
    }
}

internal fun apiDetailFromBody(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return try {
        val detail = ApiClient.json.parseToJsonElement(body).jsonObject["detail"] ?: return null
        when (detail) {
            is JsonPrimitive -> detail.contentOrNull?.takeIf { it.isNotBlank() }
            is JsonObject -> stringValue(detail, "message", "detail", "msg")
            is JsonArray -> detail.firstNotNullOfOrNull { item ->
                val obj = item as? JsonObject ?: return@firstNotNullOfOrNull null
                stringValue(obj, "msg", "message")
            }
        }
    } catch (_: Exception) {
        Regex("\"detail\"\\s*:\\s*\"([^\"]+)\"")
            .find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }
    }
}

private fun deviceFromJson(item: kotlinx.serialization.json.JsonElement): ActiveDeviceSession? {
    val obj = item as? JsonObject ?: return null
    val deviceName = stringValue(obj, "device_name", "deviceName", "name")
    val platform = stringValue(obj, "platform")?.let(::prettyPlatform)
    val lastActive = stringValue(obj, "last_active_at", "last_active", "lastActive")
    val account = stringValue(obj, "email", "account", "user_email", "identifier")
    if (deviceName == null && platform == null && lastActive == null && account == null) return null
    return ActiveDeviceSession(
        accountLabel = account,
        deviceName = deviceName,
        platform = platform,
        lastActive = lastActive,
    )
}

private fun prettyPlatform(raw: String): String = when (raw.trim().uppercase()) {
    "ANDROID" -> "Android"
    "IOS" -> "iOS"
    "WEB" -> "Web"
    else -> raw
}

private fun stringValue(
    obj: JsonObject?,
    vararg keys: String,
): String? {
    if (obj == null) return null
    keys.forEach { key ->
        val value = (obj[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
        if (value != null) return value
    }
    return null
}

private fun stringValue(element: kotlinx.serialization.json.JsonElement?): String? =
    (element as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }

private fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray
