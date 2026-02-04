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
    onDeleteNotification: (TimelineNotification) -> Unit,
    onNotificationClick: (TimelineNotification) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications, key = { it.id }) { notification ->
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
                                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF5350)
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
                            onClick = { onNotificationClick(notification) }
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun NotificationItemRow(
    notification: TimelineNotification,
    isUnread: Boolean,
    isFollowing: Boolean,
    onFollowToggle: (Int) -> Unit,
    onClick: () -> Unit
) {
    val unreadColor = ElectricRed
    val (avatarUrl, title, subtitle) = when (notification) {
        is TimelineNotification.Follow -> Triple(
            notification.user.avatarUrl,
            notification.user.fullName,
            "Empezó a seguirte."
        )
        is TimelineNotification.Mention -> Triple(
            notification.user.avatarUrl,
            "${notification.user.fullName} te mencionó en un comentario.",
            notification.commentText.replace("\n", " ").take(80)
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
            
            if (notification is TimelineNotification.Follow) {
                Spacer(Modifier.width(12.dp))
                
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
        }
    }
}
