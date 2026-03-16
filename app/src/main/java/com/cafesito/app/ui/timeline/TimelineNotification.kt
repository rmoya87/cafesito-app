package com.cafesito.app.ui.timeline

import com.cafesito.app.data.UserEntity

sealed class TimelineNotification(
    open val notificationId: Int,
    open val id: String,
    open val timestamp: Long,
    open val isRead: Boolean
) {
    abstract val type: String

    data class Follow(
        override val notificationId: Int,
        override val id: String,
        override val timestamp: Long,
        override val isRead: Boolean,
        val user: UserEntity
    ) : TimelineNotification(notificationId, id, timestamp, isRead) {
        override val type: String = "follow"
    }

    data class Mention(
        override val notificationId: Int,
        override val id: String,
        override val timestamp: Long,
        override val isRead: Boolean,
        val user: UserEntity,
        val postId: String,
        val commentId: Int,
        val commentText: String
    ) : TimelineNotification(notificationId, id, timestamp, isRead) {
        override val type: String = "mention"
    }

    data class Comment(
        override val notificationId: Int,
        override val id: String,
        override val timestamp: Long,
        override val isRead: Boolean,
        val user: UserEntity,
        val postId: String,
        val commentId: Int,
        val message: String
    ) : TimelineNotification(notificationId, id, timestamp, isRead) {
        override val type: String = "comment"
    }

    /** Invitación a una lista compartida. relatedId en BD = invitation_id (uuid). */
    data class ListInvite(
        override val notificationId: Int,
        override val id: String,
        override val timestamp: Long,
        override val isRead: Boolean,
        val user: UserEntity,
        val invitationId: String,
        val message: String
    ) : TimelineNotification(notificationId, id, timestamp, isRead) {
        override val type: String = "list_invite"
    }
}
