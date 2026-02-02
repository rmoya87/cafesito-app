package com.cafesito.app.ui.timeline

import com.cafesito.app.data.NotificationEntity
import com.cafesito.app.data.UserEntity

fun NotificationEntity.toTimelineNotification(users: List<UserEntity>): TimelineNotification? {
    val notificationId = id
    val notificationKey = "notification:$notificationId"
    return when (type.uppercase()) {
        "FOLLOW" -> {
            val followerId = relatedId?.toIntOrNull()
            val user = followerId?.let { id -> users.find { it.id == id } }
                ?: users.find { it.username == fromUsername }
                ?: return null
            TimelineNotification.Follow(
                notificationId = notificationId,
                id = notificationKey,
                timestamp = timestamp,
                isRead = isRead,
                user = user
            )
        }
        "MENTION" -> {
            val mentionTarget = parseMentionTarget(relatedId) ?: return null
            val user = users.find { it.username == fromUsername } ?: return null
            TimelineNotification.Mention(
                notificationId = notificationId,
                id = notificationKey,
                timestamp = timestamp,
                isRead = isRead,
                user = user,
                postId = mentionTarget.postId,
                commentId = mentionTarget.commentId,
                commentText = message
            )
        }
        else -> null
    }
}

private data class MentionTarget(val postId: String, val commentId: Int)

private fun parseMentionTarget(relatedId: String?): MentionTarget? {
    if (relatedId.isNullOrBlank()) return null
    val parts = relatedId.split(":")
    if (parts.size != 2) return null
    val commentId = parts[1].toIntOrNull() ?: return null
    return MentionTarget(postId = parts[0], commentId = commentId)
}
