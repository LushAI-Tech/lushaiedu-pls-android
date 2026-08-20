package com.lushaiedupls.ui.teacher.secondary

import com.lushaiedupls.data.mock.TeacherAnnouncement
import com.lushaiedupls.data.mock.TeacherAnnouncementAudience

enum class AnnouncementPriority {
    Normal,
    Urgent,
}

data class TeacherAnnouncementsUiState(
    val announcements: List<TeacherAnnouncement> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class TeacherNewAnnouncementUiState(
    val audiences: List<TeacherAnnouncementAudience> = emptyList(),
    val selectedAudienceIds: Set<String> = emptySet(),
    val priority: AnnouncementPriority = AnnouncementPriority.Normal,
    val subject: String = "",
    val body: String = "",
    val sent: Boolean = false,
    val isSending: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val subjectCount: Int get() = subject.length
    val bodyCount: Int get() = body.length
    val canSend: Boolean
        get() = selectedAudienceIds.isNotEmpty() &&
            subject.isNotBlank() &&
            body.isNotBlank() &&
            subject.length <= 180 &&
            body.length <= 2000 &&
            !isSending
}
