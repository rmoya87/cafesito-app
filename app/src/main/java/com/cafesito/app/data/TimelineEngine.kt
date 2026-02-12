package com.cafesito.app.data

import kotlin.math.min

object TimelineEngine {
    private const val MIN_FOLLOWING_THRESHOLD = 5

    fun getTimeline(
        posts: List<PostWithDetails>,
        reviews: List<ReviewWithAuthor>,
        followingIds: Set<Int>,
        viewerId: Int,
        cursor: Long?,
        limit: Int,
        mode: TimelineFeedMode,
        blockedUserIds: Set<Int> = emptySet()
    ): TimelinePage {
        val allItems = buildList {
            addAll(posts.map { TimelineFeedItem.Post(it) })
            addAll(reviews.map { TimelineFeedItem.Review(it) })
        }
        val filteredByCursor = allItems
            .filter { cursor == null || it.timestamp < cursor }
            .filterNot { blockedUserIds.contains(it.authorId) }
            .sortedByDescending { it.timestamp }

        val visibleFollowingIds = if (followingIds.isEmpty()) {
            emptySet()
        } else {
            followingIds + viewerId
        }

        val followingItems = if (visibleFollowingIds.isEmpty()) {
            emptyList()
        } else {
            filteredByCursor.filter { visibleFollowingIds.contains(it.authorId) }
        }
        val globalItems = filteredByCursor

        val fallbacks = mutableListOf<TimelineFeedMode>()
        var feedSource = mode

        val baseItems = when (mode) {
            TimelineFeedMode.FOLLOWING -> {
                if (visibleFollowingIds.isEmpty()) {
                    feedSource = TimelineFeedMode.GLOBAL
                    fallbacks.add(TimelineFeedMode.GLOBAL)
                    globalItems
                } else {
                    followingItems
                }
            }
            TimelineFeedMode.GLOBAL -> globalItems
        }

        val combinedItems = if (mode == TimelineFeedMode.FOLLOWING && visibleFollowingIds.isNotEmpty()) {
            if (followingItems.size < MIN_FOLLOWING_THRESHOLD) {
                val needed = maxOf(limit - followingItems.size, 0)
                val filler = globalItems
                    .filterNot { item -> followingItems.any { it.key == item.key } }
                    .take(needed)
                if (filler.isNotEmpty()) {
                    fallbacks.add(TimelineFeedMode.GLOBAL)
                }
                (followingItems + filler).sortedByDescending { it.timestamp }
            } else {
                baseItems
            }
        } else {
            baseItems
        }

        val limitedItems = combinedItems.take(limit)
        val nextCursor = if (limitedItems.size == limit) {
            limitedItems.lastOrNull()?.timestamp
        } else {
            null
        }

        val reasonCode = if (limitedItems.isEmpty()) {
            when {
                blockedUserIds.isNotEmpty() && filteredByCursor.isEmpty() -> TimelineReasonCode.BLOCK_MUTE_EXHAUSTED
                globalItems.isEmpty() -> TimelineReasonCode.NO_POSTS_GLOBAL
                mode == TimelineFeedMode.FOLLOWING -> TimelineReasonCode.FILTERS_TOO_STRICT
                else -> TimelineReasonCode.NO_POSTS_GLOBAL
            }
        } else {
            null
        }

        val candidateCount = when (feedSource) {
            TimelineFeedMode.FOLLOWING -> followingItems.size
            TimelineFeedMode.GLOBAL -> globalItems.size
        }

        return TimelinePage(
            items = limitedItems,
            nextCursor = nextCursor,
            meta = TimelineMeta(
                feedSource = feedSource,
                fallbacksUsed = fallbacks.distinct(),
                reasonCode = reasonCode,
                totalCandidatesChecked = min(candidateCount, filteredByCursor.size)
            )
        )
    }
}
