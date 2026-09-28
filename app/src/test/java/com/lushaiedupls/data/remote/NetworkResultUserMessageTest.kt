package com.lushaiedupls.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkResultUserMessageTest {

    @Test
    fun error400_usesClassInstitutionMismatchDetail() {
        val result = NetworkResult.Error(
            code = 400,
            message = "Bad Request",
            body = """{"detail":"Selected class does not belong to the selected institution."}""",
        )
        assertEquals(
            "Selected class does not belong to the selected institution.",
            result.userMessage(),
        )
    }

    @Test
    fun error422_missingInstitutionId_promptsToSelectInstitution() {
        val result = NetworkResult.Error(
            code = 422,
            message = "Unprocessable Entity",
            body = """{"detail":[{"type":"missing","loc":["query","institution_id"],"msg":"Field required"}]}""",
        )
        assertEquals("Select an institution first.", result.userMessage())
    }

    @Test
    fun error400_invalidSubtopicUuid_promptsToReselectChapter() {
        val result = NetworkResult.Error(
            code = 400,
            message = "Bad Request",
            body = """{"detail":"Invalid UUID format in subtopic_ids"}""",
        )
        assertEquals(AiQueryParams.SCOPE_RESELECT_MESSAGE, result.userMessage())
        assertEquals(AiQueryParams.SCOPE_RESELECT_MESSAGE, result.aiScopeUserMessage())
    }

    @Test
    fun error404_subtopicNotUnderChapter_promptsToReselectChapter() {
        val result = NetworkResult.Error(
            code = 404,
            message = "Not Found",
            body = """{"detail":"Section not found under selected chapter"}""",
        )
        assertEquals(AiQueryParams.SCOPE_RESELECT_MESSAGE, result.userMessage())
        assertEquals(AiQueryParams.SCOPE_RESELECT_MESSAGE, result.aiScopeUserMessage())
    }

    @Test
    fun error409_feeDelete_usesLedgerConflictCopy() {
        val result = NetworkResult.Error(
            code = 409,
            message = "Conflict",
            body = """{"detail":"Cannot delete fee because ledger rows already exist."}""",
        )
        assertEquals(FeeMonth.TEMPLATE_LOCKED_MESSAGE, result.userMessage())
    }

    @Test
    fun error409_emailAlreadyExists_usesAccountExistsCopy() {
        val result = NetworkResult.Error(
            code = 409,
            message = "Conflict",
            body = """{"detail":"Email already registered"}""",
        )
        assertEquals(ACCOUNT_ALREADY_EXISTS_EMAIL_MESSAGE, result.userMessage())
        assertEquals(true, result.isAccountAlreadyExists())
        assertEquals(false, result.isGooglePasswordLinkBlocked())
    }

    @Test
    fun error409_googlePasswordUnverified_usesLinkBlockedCopy() {
        val result = NetworkResult.Error(
            code = 409,
            message = "Conflict",
            body = """{"detail":"Account exists with a password. Sign in with password and verify email before linking Google."}""",
        )
        assertEquals(GOOGLE_PASSWORD_LINK_BLOCKED_MESSAGE, result.userMessage())
        assertEquals(true, result.isGooglePasswordLinkBlocked())
        assertEquals(false, result.isAccountAlreadyExists())
    }

    @Test
    fun error409_passwordAndEmailVerifiedFalse_usesLinkBlockedCopy() {
        val result = NetworkResult.Error(
            code = 409,
            message = "Conflict",
            body = """{"detail":"User has a password and email_verified is false"}""",
        )
        assertEquals(GOOGLE_PASSWORD_LINK_BLOCKED_MESSAGE, result.userMessage())
        assertEquals(true, result.isGooglePasswordLinkBlocked())
    }

    @Test
    fun error409_objectDetailWithConflictToken_showsNestedMessage() {
        val result = NetworkResult.Error(
            code = 409,
            message = "Conflict",
            body = """{"detail":{"message":"No login is allowed while another device is still active. Please sign out on that device first.","conflict_token":"ctok_1"}}""",
        )
        assertEquals(
            "No login is allowed while another device is still active. Please sign out on that device first.",
            result.loginUserMessage(),
        )
        assertEquals(true, result.isDeviceSessionConflict())
        assertEquals(false, result.isAccountAlreadyExists())
    }

    @Test
    fun error409_alreadySignedInOnAnotherDevice_showsDetailAndSignOutHint() {
        val result = NetworkResult.Error(
            code = 409,
            message = "Conflict",
            body = """{"detail":"This account is already signed in on another device."}""",
        )
        val expected =
            "This account is already signed in on another device.\n\n$DEVICE_SESSION_HINT"
        assertEquals(expected, result.userMessage())
        assertEquals(expected, result.loginUserMessage())
        assertEquals(true, result.isDeviceSessionConflict())
        assertEquals(false, result.isAccountAlreadyExists())
    }

    @Test
    fun error409_deviceLimit_showsApiDetailAndSignOutHint() {
        val result = NetworkResult.Error(
            code = 409,
            message = "Conflict",
            body = """{"detail":"Maximum number of devices reached for this account."}""",
        )
        val expected =
            "Maximum number of devices reached for this account.\n\n$DEVICE_SESSION_HINT"
        assertEquals(expected, result.loginUserMessage())
        assertEquals(true, result.isDeviceSessionConflict())
        assertEquals(false, result.isAccountAlreadyExists())
    }

    @Test
    fun error409_deviceLimitDetailAlreadyMentionsSignOut_doesNotDuplicateHint() {
        val detail =
            "Device limit reached. Open the other device → Account → Sign out."
        val result = NetworkResult.Error(
            code = 409,
            message = "Conflict",
            body = """{"detail":"$detail"}""",
        )
        assertEquals(detail, result.loginUserMessage())
        assertEquals(true, result.isDeviceSessionConflict())
    }

    @Test
    fun loginUserMessage_unknown409_showsDetailAndSignOutHint() {
        val result = NetworkResult.Error(
            code = 409,
            message = "Conflict",
            body = """{"detail":"Cannot complete sign-in right now."}""",
        )
        val expected = "Cannot complete sign-in right now.\n\n$DEVICE_SESSION_HINT"
        assertEquals(expected, result.loginUserMessage())
        assertEquals(false, result.isDeviceSessionConflict())
        assertEquals("Cannot complete sign-in right now.", result.userMessage())
    }

    @Test
    fun error400_userAlreadyExists_usesAccountExistsCopy() {
        val result = NetworkResult.Error(
            code = 400,
            message = "Bad Request",
            body = """{"detail":"User already exists"}""",
        )
        assertEquals(ACCOUNT_ALREADY_EXISTS_MESSAGE, result.userMessage())
        assertEquals(true, result.isAccountAlreadyExists())
    }

    @Test
    fun error400_stemBindingMissing_usesFriendlyCopy() {
        val result = NetworkResult.Error(
            code = 400,
            message = "Bad Request",
            body = """{"detail":"'Chemistry' has no AI content bound yet. An admin must bind it via PUT /api/v1/admin/subjects/{id}/stem-binding."}""",
        )
        assertEquals(
            "Chemistry isn’t set up for AI learning yet. Please check back later.",
            result.userMessage(),
        )
    }

    @Test
    fun friendlyStemBindingMessage_rewritesRawApiDetail() {
        val raw =
            "'Chemistry' has no AI content bound yet. An admin must bind it via PUT /api/v1/admin/subjects/{id}/stem-binding."
        assertEquals(
            "Chemistry isn’t set up for AI learning yet. Please check back later.",
            friendlyStemBindingMessage(raw),
        )
    }

    @Test
    fun error404_feeTemplate_usesNotFoundCopy() {
        val result = NetworkResult.Error(
            code = 404,
            message = "Not Found",
            body = """{"detail":"Fee template not found."}""",
        )
        assertEquals(FeeMonth.TEMPLATE_NOT_FOUND_MESSAGE, result.userMessage())
    }
}
