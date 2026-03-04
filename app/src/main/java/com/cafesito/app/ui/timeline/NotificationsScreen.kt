package com.cafesito.app.ui.timeline

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.cafesito.app.ui.theme.ElectricRed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cafesito.app.ui.components.GlassyTopBar
import com.cafesito.app.ui.theme.ElectricRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    notifications: List<TimelineNotification>,
    unreadIds: Set<String>,
    followingIds: Set<Int>,
    onBackClick: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onFollowToggle: (Int) -> Unit,
    onReplyToNotification: (TimelineNotification) -> Unit,
    onSavePostFromNotification: (TimelineNotification) -> Unit,
    onDeleteNotification: (TimelineNotification) -> Unit,
    onNotificationClick: (TimelineNotification) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val groupedNotifications = remember(notifications) {
        groupNotificationsByRange(notifications)
    }

    // Marcamos todo como visto automáticamente al entrar en la pantalla
    LaunchedEffect(Unit) {
        onMarkAllAsRead()
    }

    Scaffold(
        topBar = {
            GlassyTopBar(
                title = "Notificaciones",
                onBackClick = onBackClick,
                actions = {} 
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (notifications.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes notificaciones", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedNotifications.forEachIndexed { index, section ->
                        item(key = "header-${section.key}") {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (index == 0) 2.dp else 10.dp, bottom = 2.dp)
                            )
                        }

                        items(section.items, key = { it.id }) { notification ->
                            val isUnread = notification.id in unreadIds

                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        onDeleteNotification(notification)
                                        true
                                    } else false
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    val color by animateColorAsState(
                                        when (dismissState.targetValue) {
                                            SwipeToDismissBoxValue.EndToStart -> ElectricRed
                                            else -> Color.Transparent
                                        }, label = "bgColor"
                                    )
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(color),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = Color.White,
                                            modifier = Modifier.padding(end = 24.dp)
                                        )
                                    }
                                }
                            ) {
                                NotificationItemRow(
                                    notification = notification,
                                    isUnread = isUnread,
                                    isFollowing = when (notification) {
                                        is TimelineNotification.Follow -> followingIds.contains(notification.user.id)
                                        else -> false
                                    },
                                    onFollowToggle = onFollowToggle,
                                    onReplyToNotification = onReplyToNotification,
                                    onSavePostFromNotification = onSavePostFromNotification,
                                    onClick = { onNotificationClick(notification) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class NotificationSection(
    val key: String,
    val title: String,
    val items: List<TimelineNotification>
)

private fun groupNotificationsByRange(notifications: List<TimelineNotification>): List<NotificationSection> {
    if (notifications.isEmpty()) return emptyList()
    val calendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val todayStart = calendar.timeInMillis
    val oneDayMs = 24L * 60L * 60L * 1000L
    val yesterdayStart = todayStart - oneDayMs
    val last7Start = todayStart - oneDayMs * 7
    val last30Start = todayStart - oneDayMs * 30

    val today = mutableListOf<TimelineNotification>()
    val yesterday = mutableListOf<TimelineNotification>()
    val last7 = mutableListOf<TimelineNotification>()
    val last30 = mutableListOf<TimelineNotification>()

    notifications.forEach { notification ->
        when {
            notification.timestamp >= todayStart -> today += notification
            notification.timestamp >= yesterdayStart -> yesterday += notification
            notification.timestamp >= last7Start -> last7 += notification
            notification.timestamp >= last30Start -> last30 += notification
            else -> last30 += notification
        }
    }

    return listOf(
        NotificationSection("today", "Hoy", today),
        NotificationSection("yesterday", "Ayer", yesterday),
        NotificationSection("last7", "Últimos 7 días", last7),
        NotificationSection("last30", "Últimos 30 días", last30)
    ).filter { it.items.isNotEmpty() }
}

@Composable
private fun NotificationItemRow(
    notification: TimelineNotification,
    isUnread: Boolean,
    isFollowing: Boolean,
    onFollowToggle: (Int) -> Unit,
    onReplyToNotification: (TimelineNotification) -> Unit,
    onSavePostFromNotification: (TimelineNotification) -> Unit,
    onClick: () -> Unit
) {
    val unreadColor = ElectricRed
    val (avatarUrl, title, subtitle) = when (notification) {
        is TimelineNotification.Follow -> Triple(
            notification.user.avatarUrl,
            "@${notification.user.username}",
            "ha comenzado a seguirte"
        )
        is TimelineNotification.Mention -> Triple(
            notification.user.avatarUrl,
            "@${notification.user.username}",
            notification.commentText
        )
        is TimelineNotification.Comment -> Triple(
            notification.user.avatarUrl,
            "@${notification.user.username}",
            notification.message
        )
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                )
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(12.dp)
                            .background(unreadColor, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Spacer(Modifier.width(8.dp))

            when (notification) {
                is TimelineNotification.Follow -> {
                    if (isFollowing) {
                        OutlinedButton(
                            onClick = { onFollowToggle(notification.user.id) },
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("SIGUIENDO", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    } else {
                        Button(
                            onClick = { onFollowToggle(notification.user.id) },
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("SEGUIR", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }

                is TimelineNotification.Comment,
                is TimelineNotification.Mention -> {
                    Button(
                        onClick = { onReplyToNotification(notification) },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("RESPONDER", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
