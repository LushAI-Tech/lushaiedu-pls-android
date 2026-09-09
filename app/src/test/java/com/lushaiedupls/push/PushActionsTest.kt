package com.lushaiedupls.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PushActionsTest {

    @Test
    fun parse_pendingApproval_fromDataPayload() {
        val action = PushActions.parse(
            mapOf(
                "type" to "pending_approval",
                "user_id" to "user-123",
                "role" to "STUDENT",
            ),
        )
        assertTrue(action is PushAction.PendingApproval)
        val pending = action as PushAction.PendingApproval
        assertEquals("user-123", pending.userId)
        assertEquals("STUDENT", pending.role)
    }

    @Test
    fun parse_pendingApproval_acceptsPushTypeAlias() {
        val action = PushActions.parse(
            mapOf(
                "push_type" to "pending_approval",
                "user_id" to "parent-9",
                "role" to "PARENT",
            ),
        )
        val pending = action as PushAction.PendingApproval
        assertEquals("parent-9", pending.userId)
        assertEquals("PARENT", pending.role)
    }

    @Test
    fun parse_doesNotTreatInboxNotificationAsPendingApproval() {
        assertNull(
            PushActions.parse(
                mapOf(
                    "type" to "notification",
                    "notification_id" to "n1",
                ),
            ),
        )
    }

    @Test
    fun parse_ignoresTestAndUnknownTypes() {
        assertNull(PushActions.parse(mapOf("type" to "test")))
        assertNull(PushActions.parse(mapOf("type" to "period_reminder")))
        assertTrue(PushActions.isTest(mapOf("type" to "test")))
    }
}
