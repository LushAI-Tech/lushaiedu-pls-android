package com.lushaiedupls.data.remote

object ResourcePlayback {
    private val youtubeIdRegex = Regex(
        """(?:youtube\.com/(?:watch\?(?:.*&)?v=|embed/|shorts/|live/)|youtu\.be/)([A-Za-z0-9_-]{11})""",
        RegexOption.IGNORE_CASE,
    )

    fun youtubeId(url: String?): String? {
        val value = url?.trim().orEmpty()
        if (value.isEmpty()) return null
        return youtubeIdRegex.find(value)?.groupValues?.getOrNull(1)
    }

    fun youtubeThumbnail(id: String): String =
        "https://img.youtube.com/vi/$id/hqdefault.jpg"

    fun youtubeEmbedUrl(id: String): String =
        "https://www.youtube-nocookie.com/embed/$id?autoplay=1&modestbranding=1&rel=0&playsinline=1"

    fun isDirectVideoUrl(url: String?): Boolean {
        val path = url?.substringBefore('?')?.lowercase().orEmpty()
        return path.endsWith(".mp4") ||
            path.endsWith(".webm") ||
            path.endsWith(".m3u8") ||
            path.endsWith(".mov") ||
            path.endsWith(".3gp")
    }

    fun isVideoType(resourceType: String?): Boolean {
        val key = resourceType.orEmpty().lowercase()
        return "video" in key || "youtube" in key
    }

    fun playbackUrl(
        youtubeUrl: String?,
        fileUrl: String?,
        externalUrl: String?,
        resourceType: String?,
    ): String? {
        val candidates = listOf(youtubeUrl, externalUrl, fileUrl)
            .mapNotNull { it?.trim()?.takeIf { value -> value.isNotBlank() } }
        candidates.firstOrNull { youtubeId(it) != null }?.let { return it }
        val file = fileUrl?.trim()?.takeIf { it.isNotBlank() }
        if (file != null && (isDirectVideoUrl(file) || isVideoType(resourceType))) {
            return file
        }
        return null
    }

    fun thumbnailUrl(
        thumbnailUrl: String?,
        youtubeUrl: String?,
        fileUrl: String?,
        externalUrl: String?,
    ): String? {
        thumbnailUrl?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        val id = youtubeId(youtubeUrl) ?: youtubeId(externalUrl) ?: youtubeId(fileUrl)
        if (id != null) return youtubeThumbnail(id)
        val file = fileUrl?.trim()?.takeIf { it.isNotBlank() }
        return file?.takeIf { !isDirectVideoUrl(it) }
    }
}
