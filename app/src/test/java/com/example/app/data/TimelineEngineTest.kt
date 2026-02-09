package com.cafesito.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineEngineTest {
    @Test
    fun `new user falls back to global feed`() {
        val posts = listOf(post("p1", userId = 2, timestamp = 100))
        val result = TimelineEngine.getTimeline(
            posts = posts,
            reviews = emptyList(),
            followingIds = emptySet(),
            viewerId = 1,
            cursor = null,
            limit = 10,
            mode = TimelineFeedMode.FOLLOWING
        )

        assertTrue(result.items.isNotEmpty())
        assertEquals(TimelineFeedMode.GLOBAL, result.meta.feedSource)
    }

    @Test
    fun `following feed includes own posts and followed users`() {
        val posts = listOf(
            post("p1", userId = 1, timestamp = 300),
            post("p2", userId = 2, timestamp = 200),
            post("p3", userId = 3, timestamp = 100)
        )

        val result = TimelineEngine.getTimeline(
            posts = posts,
            reviews = emptyList(),
            followingIds = setOf(2),
            viewerId = 1,
            cursor = null,
            limit = 10,
            mode = TimelineFeedMode.FOLLOWING
        )

        val authorIds = result.items.map { it.authorId }.toSet()
        assertTrue(authorIds.contains(1))
        assertTrue(authorIds.contains(2))
        assertTrue(authorIds.none { it == 3 })
    }

    @Test
    fun `following feed fills with global when below threshold`() {
        val posts = listOf(
            post("p1", userId = 2, timestamp = 300),
            post("p2", userId = 3, timestamp = 200),
            post("p3", userId = 4, timestamp = 100),
            post("p4", userId = 5, timestamp = 90),
            post("p5", userId = 6, timestamp = 80)
        )

        val result = TimelineEngine.getTimeline(
            posts = posts,
            reviews = emptyList(),
            followingIds = setOf(2),
            viewerId = 1,
            cursor = null,
            limit = 5,
            mode = TimelineFeedMode.FOLLOWING
        )

        assertEquals(5, result.items.size)
        assertTrue(result.meta.fallbacksUsed.contains(TimelineFeedMode.GLOBAL))
    }

    @Test
    fun `blocked users can exhaust feed`() {
        val posts = listOf(post("p1", userId = 2, timestamp = 100))
        val result = TimelineEngine.getTimeline(
            posts = posts,
            reviews = emptyList(),
            followingIds = emptySet(),
            viewerId = 1,
            cursor = null,
            limit = 10,
            mode = TimelineFeedMode.GLOBAL,
            blockedUserIds = setOf(2)
        )

        assertTrue(result.items.isEmpty())
        assertEquals(TimelineReasonCode.BLOCK_MUTE_EXHAUSTED, result.meta.reasonCode)
    }

    private fun post(id: String, userId: Int, timestamp: Long): PostWithDetails {
        return PostWithDetails(
            post = PostEntity(
                id = id,
                userId = userId,
                imageUrl = "",
                comment = "",
                timestamp = timestamp
            ),
            author = null,
            likes = emptyList(),
            comments = emptyList()
        )
    }
}
