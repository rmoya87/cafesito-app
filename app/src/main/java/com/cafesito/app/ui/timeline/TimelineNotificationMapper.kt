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
            val mentionTarget = parseNotificationTarget(relatedId) ?: return null
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
        "COMMENT" -> {
            val commentTarget = parseNotificationTarget(relatedId) ?: return null
            val user = users.find { it.username == fromUsername } ?: return null
            TimelineNotification.Comment(
                notificationId = notificationId,
                id = notificationKey,
                timestamp = timestamp,
                isRead = isRead,
                user = user,
                postId = commentTarget.postId,
                commentId = commentTarget.commentId,
                message = message
            )
        }
        else -> null
    }
}

private data class NotificationTarget(val postId: String, val commentId: Int)

private fun parseNotificationTarget(relatedId: String?): NotificationTarget? {
    if (relatedId.isNullOrBlank()) return null
    val delimiter = when {
        relatedId.contains(":") -> ":"
        relatedId.contains("|") -> "|"
        relatedId.contains(";") -> ";"
        else -> return null
    }
    val parts = relatedId.split(delimiter)
    if (parts.size != 2) return null
    val commentId = parts[1].toIntOrNull() ?: return null
    return NotificationTarget(postId = parts[0], commentId = commentId)
}
