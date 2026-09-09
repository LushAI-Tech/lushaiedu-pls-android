package com.lushaiedupls.data.remote

/**
 * Fee APIs accept `YYYY-MM` for a specific month, or `all` on list/history filters.
 */
object FeeMonth {
    const val ALL = "all"
    const val INVALID_MESSAGE = "Please select a valid month in YYYY-MM format."
    const val TEMPLATE_LOCKED_MESSAGE =
        "This month’s fee template is locked because ledger rows already exist."
    const val TEMPLATE_NOT_FOUND_MESSAGE =
        "This fee row no longer exists. Refresh and try again."
    const val TEMPLATE_SAVED_MESSAGE = "Fee template saved successfully."
    const val TEMPLATE_DELETED_MESSAGE = "Fee template deleted successfully."
    /** @deprecated Use [TEMPLATE_LOCKED_MESSAGE] */
    const val DELETE_CONFLICT_MESSAGE = TEMPLATE_LOCKED_MESSAGE

    fun bulkDeleteResultMessage(
        apiMessage: String,
        matchedCount: Int,
        deletedCount: Int,
        month: String,
    ): String {
        val fallback = "Deleted $deletedCount of $matchedCount matched ledger rows for $month."
        val base = apiMessage.trim().ifBlank { fallback }
        val hasCounts = base.contains(matchedCount.toString()) &&
            base.contains(deletedCount.toString())
        return if (hasCounts) base else "$base (matched $matchedCount, deleted $deletedCount)"
    }

    private val YearMonthPattern = Regex("""^\d{4}-(0[1-9]|1[0-2])$""")

    fun isYearMonth(value: String): Boolean = YearMonthPattern.matches(value.trim())

    fun isListFilter(value: String?): Boolean {
        val trimmed = value?.trim().orEmpty()
        return trimmed.isEmpty() || trimmed.equals(ALL, ignoreCase = true) || isYearMonth(trimmed)
    }

    fun yearMonthOrNull(value: String): String? =
        value.trim().takeIf { isYearMonth(it) }

    fun listFilterOrNull(value: String?): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (trimmed.equals(ALL, ignoreCase = true)) return ALL
        return trimmed.takeIf { isYearMonth(it) }
    }

    fun invalidMonthError(): NetworkResult.Error =
        NetworkResult.Error(code = 400, message = INVALID_MESSAGE)
}
