package com.lushaiedupls.data.remote

/**
 * Builds optional AI query params. Empty values are omitted so Retrofit skips them.
 */
object AiQueryParams {
    const val SCOPE_RESELECT_MESSAGE = "Please reselect chapter/subtopics and try again."

    fun commaSeparatedIds(ids: Collection<String>?): String? =
        ids.orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.joinToString(",")

    fun nonEmpty(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }

    fun isChapterScope(
        selectedSubtopicIds: Collection<String>,
        allSubtopicIds: Collection<String> = emptyList(),
    ): Boolean {
        val selected = selectedSubtopicIds.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        if (selected.isEmpty()) return true
        val all = allSubtopicIds.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        return all.isNotEmpty() && selected == all
    }

    /** Questions / attach-files: send only when a specific (not all-chapter) selection exists. */
    fun subtopicIdsQuery(
        selectedSubtopicIds: Collection<String>,
        allSubtopicIds: Collection<String> = emptyList(),
    ): String? {
        if (isChapterScope(selectedSubtopicIds, allSubtopicIds)) return null
        return commaSeparatedIds(selectedSubtopicIds)
    }

    data class ExamPrepQuery(
        val sectionId: String? = null,
        val chapterScope: Boolean? = null,
        val examCodes: String? = null,
        val subtopicIds: String? = null,
    )

    /**
     * Exam-prep scope:
     * - all chapter topics → chapter_scope=true, no subtopic_ids
     * - specific subtopics → chapter_scope=false + subtopic_ids
     * - single section_id mode → keep section_id, skip subtopic_ids
     * Never send chapter_scope=true together with subtopic_ids.
     */
    fun examPrepQuery(
        selectedSubtopicIds: Collection<String> = emptyList(),
        allSubtopicIds: Collection<String> = emptyList(),
        examCodes: String? = null,
        singleSectionId: String? = null,
    ): ExamPrepQuery {
        val codes = nonEmpty(examCodes)
        val sectionId = nonEmpty(singleSectionId)
        if (sectionId != null) {
            return ExamPrepQuery(
                sectionId = sectionId,
                examCodes = codes,
            )
        }
        return if (isChapterScope(selectedSubtopicIds, allSubtopicIds)) {
            ExamPrepQuery(
                chapterScope = true,
                examCodes = codes,
            )
        } else {
            ExamPrepQuery(
                chapterScope = false,
                examCodes = codes,
                subtopicIds = commaSeparatedIds(selectedSubtopicIds),
            )
        }
    }
}
