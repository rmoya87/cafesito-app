package com.cafesito.app.ui.timeline

import com.cafesito.app.data.UserEntity

sealed class TimelineNotification(
    open val notificationId: Int,
    open val id: String,
    open val timestamp: Long,
    open val isRead: Boolean
) {
    data class Follow(
        override val notificationId: Int,
        override val id: String,
        override val timestamp: Long,
        override val isRead: Boolean,
        val user: UserEntity
    ) : TimelineNotification(notificationId, id, timestamp, isRead)

    data class Mention(
        override val notificationId: Int,
        override val id: String,
        override val timestamp: Long,
        override val isRead: Boolean,
        val user: UserEntity,
        val postId: String,
        val commentId: Int,
        val commentText: String
    ) : TimelineNotification(notificationId, id, timestamp, isRead)
}
