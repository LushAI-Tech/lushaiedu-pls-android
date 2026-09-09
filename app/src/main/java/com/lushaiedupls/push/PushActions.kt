package com.lushaiedupls.push

import android.content.Intent
import java.util.concurrent.atomic.AtomicLong

sealed interface PushAction {
    data class PendingApproval(
        val userId: String?,
        val role: String?,
        val eventId: Long = nextEventId(),
    ) : PushAction
}

object PushActions {
    const val TYPE_PENDING_APPROVAL = "pending_approval"
    const val TYPE_NOTIFICATION = "notification"
    const val TYPE_PERIOD_REMINDER = "period_reminder"
    const val TYPE_TEST = "test"

    const val EXTRA_TYPE = "type"
    const val EXTRA_PUSH_TYPE = "push_type"
    const val EXTRA_USER_ID = "user_id"
    const val EXTRA_ROLE = "role"
    const val EXTRA_NOTIFICATION_ID = "notification_id"
    const val EXTRA_SLOT_ID = "slot_id"
    const val EXTRA_TEACHING_UNIT_ID = "teaching_unit_id"

    fun typeOf(data: Map<String, String>): String? =
        data[EXTRA_TYPE]?.takeIf { it.isNotBlank() }
            ?: data[EXTRA_PUSH_TYPE]?.takeIf { it.isNotBlank() }

    fun isTest(data: Map<String, String>): Boolean = typeOf(data) == TYPE_TEST

    fun parse(data: Map<String, String>): PushAction? = when (typeOf(data)) {
        TYPE_PENDING_APPROVAL -> PushAction.PendingApproval(
            userId = data[EXTRA_USER_ID]?.takeIf { it.isNotBlank() },
            role = data[EXTRA_ROLE]?.takeIf { it.isNotBlank() },
        )
        TYPE_TEST -> null
        else -> null
    }

    fun fromIntent(intent: Intent?): PushAction? {
        if (intent == null) return null
        val extras = intent.extras ?: return null
        val data = buildMap {
            for (key in extras.keySet()) {
                if (key.isNullOrBlank()) continue
                extras.getString(key)?.takeIf { it.isNotBlank() }?.let { put(key, it) }
            }
        }
        return parse(data)
    }
}

private val EventIds = AtomicLong(0)

private fun nextEventId(): Long = EventIds.incrementAndGet()
