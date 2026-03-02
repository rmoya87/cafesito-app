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
                ?: users.find { it.username.equals(fromUsername, ignoreCase = true) }
                ?: buildFallbackUser(
                    notificationId = notificationId,
                    fromUsername = fromUsername,
                    preferredId = followerId
                )
            TimelineNotification.Follow(
                notificationId = notificationId,
                id = notificationKey,
                timestamp = timestamp,
                isRead = isRead,
                user = user
            )
        }
        "MENTION" -> {
            val mentionTarget = parseNotificationTarget(relatedId)
                ?: relatedId?.takeIf { it.isNotBlank() }?.let {
                    NotificationTarget(postId = it, commentId = -1)
                }
                ?: return null
            val user = users.find { it.username.equals(fromUsername, ignoreCase = true) }
                ?: buildFallbackUser(
                    notificationId = notificationId,
                    fromUsername = fromUsername
                )
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
            val commentTarget = parseNotificationTarget(relatedId)
                ?: relatedId?.takeIf { it.isNotBlank() }?.let {
                    NotificationTarget(postId = it, commentId = -1)
                }
                ?: return null
            val user = users.find { it.username.equals(fromUsername, ignoreCase = true) }
                ?: buildFallbackUser(
                    notificationId = notificationId,
                    fromUsername = fromUsername
                )
            TimelineNotification.Comment(
                notificationId = notificationId,
                id = notificationKey,
                timestamp = timestamp,
                isRead = isRead,
                user = user,
                postId = commentTarget.postId,
                commentId = commentTarget.commentId,
                message = normalizeNotificationMessage(message)
            )
        }
        else -> null
    }
}

private fun buildFallbackUser(
    notificationId: Int,
    fromUsername: String,
    preferredId: Int? = null
): UserEntity {
    val username = fromUsername.trim().ifBlank { "usuario" }
    return UserEntity(
        id = preferredId ?: -(notificationId + 1),
        username = username,
        fullName = username,
        avatarUrl = "",
        email = "",
        bio = null
    )
}

private data class NotificationTarget(val postId: String, val commentId: Int)

private fun normalizeNotificationMessage(message: String): String {
    return if (message.trim().equals("ha respondido en una publicación donde participas", ignoreCase = true)) {
        "respondió a una publicación"
    } else {
        message
    }
}

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
