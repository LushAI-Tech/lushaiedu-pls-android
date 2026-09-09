package com.lushaiedupls.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourcePlaybackTest {

    @Test
    fun youtubeId_parsesWatchEmbedShortsAndShortLinks() {
        assertEquals("abcdefghijk", ResourcePlayback.youtubeId("https://www.youtube.com/watch?v=abcdefghijk"))
        assertEquals("abcdefghijk", ResourcePlayback.youtubeId("https://youtu.be/abcdefghijk"))
        assertEquals("abcdefghijk", ResourcePlayback.youtubeId("https://www.youtube.com/embed/abcdefghijk"))
        assertEquals("abcdefghijk", ResourcePlayback.youtubeId("https://www.youtube.com/shorts/abcdefghijk"))
        assertNull(ResourcePlayback.youtubeId("https://example.com/video.mp4"))
    }

    @Test
    fun playbackUrl_prefersYoutubeThenDirectFile() {
        assertEquals(
            "https://youtu.be/abcdefghijk",
            ResourcePlayback.playbackUrl(
                youtubeUrl = "https://youtu.be/abcdefghijk",
                fileUrl = "https://cdn.example.com/clip.mp4",
                externalUrl = null,
                resourceType = "video",
            ),
        )
        assertEquals(
            "https://cdn.example.com/clip.mp4",
            ResourcePlayback.playbackUrl(
                youtubeUrl = null,
                fileUrl = "https://cdn.example.com/clip.mp4",
                externalUrl = null,
                resourceType = "VIDEO",
            ),
        )
        assertNull(
            ResourcePlayback.playbackUrl(
                youtubeUrl = null,
                fileUrl = "https://cdn.example.com/notes.pdf",
                externalUrl = null,
                resourceType = "pdf",
            ),
        )
    }

    @Test
    fun thumbnailUrl_usesYoutubePosterWhenMissing() {
        assertEquals(
            "https://img.youtube.com/vi/abcdefghijk/hqdefault.jpg",
            ResourcePlayback.thumbnailUrl(
                thumbnailUrl = null,
                youtubeUrl = "https://www.youtube.com/watch?v=abcdefghijk",
                fileUrl = null,
                externalUrl = null,
            ),
        )
        assertFalse(ResourcePlayback.isDirectVideoUrl("https://cdn.example.com/notes.pdf"))
        assertTrue(ResourcePlayback.isDirectVideoUrl("https://cdn.example.com/clip.mp4?token=1"))
    }
}
