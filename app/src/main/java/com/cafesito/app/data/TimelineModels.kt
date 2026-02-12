package com.cafesito.app.data

enum class TimelineFeedMode {
    FOLLOWING,
    GLOBAL
}

enum class TimelineReasonCode {
    NO_POSTS_GLOBAL,
    RLS_DENY_OR_PERMISSION,
    FILTERS_TOO_STRICT,
    QUERY_ERROR_TIMEOUT,
    PAGINATION_CURSOR_BUG,
    BLOCK_MUTE_EXHAUSTED
}

data class TimelineMeta(
    val feedSource: TimelineFeedMode,
    val fallbacksUsed: List<TimelineFeedMode> = emptyList(),
    val reasonCode: TimelineReasonCode? = null,
    val totalCandidatesChecked: Int = 0
)

sealed class TimelineFeedItem {
    abstract val timestamp: Long
    abstract val key: String
    abstract val authorId: Int

    data class Post(val details: PostWithDetails) : TimelineFeedItem() {
        override val timestamp: Long = details.post.timestamp
        override val key: String = "post_${details.post.id}"
        override val authorId: Int = details.post.userId
    }

    data class Review(val details: ReviewWithAuthor) : TimelineFeedItem() {
        override val timestamp: Long = details.review.timestamp
        override val key: String = "review_${details.review.id}"
        override val authorId: Int = details.review.userId
    }
}

data class TimelinePage(
    val items: List<TimelineFeedItem>,
    val nextCursor: Long?,
    val meta: TimelineMeta
)
