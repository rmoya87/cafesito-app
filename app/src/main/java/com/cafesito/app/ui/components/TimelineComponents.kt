package com.cafesito.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.cafesito.app.ui.theme.LocalCaramelAccent
import com.cafesito.app.ui.theme.WaterBlue
import com.cafesito.app.ui.theme.WaterBlueBackground
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.cafesito.app.R
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.data.DiaryEntryEntity
import com.cafesito.app.data.PantryItemWithDetails
import com.cafesito.app.data.PostWithDetails
import com.cafesito.app.data.UserEntity
import com.cafesito.app.data.UserReviewInfo
import com.cafesito.app.ui.brewlab.BrewLabViewModel
import com.cafesito.app.ui.brewlab.BrewMethod
import com.cafesito.app.ui.brewlab.BrewPhaseInfo
import com.cafesito.shared.domain.brew.BREW_METHOD_AGUA
import com.cafesito.shared.domain.brew.BREW_METHOD_NAMES
import com.cafesito.shared.domain.brew.BREW_METHOD_OTROS
import com.cafesito.app.ui.diary.DiaryAnalytics
import com.cafesito.app.ui.diary.DiaryPeriod
import com.cafesito.app.ui.profile.ProfileUiState
import com.cafesito.app.ui.profile.ProfileViewModel
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.timeline.TimelineNotification
import com.cafesito.app.ui.utils.formatRelativeTime
import kotlin.text.Regex
import kotlin.text.RegexOption
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.net.Uri
import androidx.core.content.FileProvider
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

// --- TIMELINE COMPONENTS ---

@Composable
fun PremiumTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(horizontal = Spacing.space4, vertical = 6.dp)
                .fillMaxWidth()
                .height(52.dp)
                .background(MaterialTheme.colorScheme.surface, Shapes.shapePremium)
                .premiumBorder(Shapes.shapePremium)
                .padding(Spacing.space1)
        ) {
            AnimatedTabIndicator(selectedTabIndex, tabs.size)
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTabIndex == index
                    val contentColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(Shapes.pill)
                            .clickable { onTabSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = title, style = MaterialTheme.typography.labelLarge, color = contentColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsBottomSheet(
    notifications: List<TimelineNotification>,
    unreadIds: Set<String>,
    followingIds: Set<Int>,
    onDismiss: () -> Unit,
    onFollowToggle: (Int) -> Unit,
    onNotificationClick: (TimelineNotification) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CafesitoModalSheetDefaults.containerColor(),
        shape = CafesitoModalSheetDefaults.shape,
        scrimColor = CafesitoModalSheetDefaults.scrimColor,
        dragHandle = { CafesitoModalSheetDefaults.dragHandle() }
    ) {
        Column(Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = Spacing.space8)) {
            Text(
                text = stringResource(id = R.string.notifications_title).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = Spacing.space6, end = Spacing.space6, top = Spacing.space3, bottom = Spacing.space2)
            )

            if (notifications.isEmpty()) {
                Box(Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(id = R.string.notifications_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val groupedNotifications = remember(notifications) {
                    groupNotificationsByRange(notifications)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = Spacing.space4, end = Spacing.space4, top = Spacing.space1, bottom = Spacing.space2),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space3)
                ) {
                    groupedNotifications.forEachIndexed { index, section ->
                        item(key = "notif-header-${section.key}") {
                            val sectionTitle = when (section.title) {
                                "notifications_today" -> stringResource(id = R.string.notifications_today)
                                "notifications_yesterday" -> stringResource(id = R.string.notifications_yesterday)
                                "notifications_last7" -> stringResource(id = R.string.notifications_last7)
                                "notifications_last30" -> stringResource(id = R.string.notifications_last30)
                                else -> section.title
                            }
                            Text(
                                text = sectionTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PureBlack,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (index == 0) 2.dp else 10.dp, bottom = 2.dp)
                            )
                        }

                        items(section.items, key = { it.id }) { notification ->
                            val isUnread = notification.id in unreadIds
                            when (notification) {
                                is TimelineNotification.Follow -> {
                                    NotificationRow(
                                        avatarUrl = notification.user.avatarUrl,
                                        title = "@${notification.user.username}",
                                        subtitle = stringResource(id = R.string.notifications_follow_started),
                                        isUnread = isUnread,
                                        trailingContent = {
                                            val isFollowing = followingIds.contains(notification.user.id)
                                            val buttonColors = if (isFollowing) {
                                                ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                            } else {
                                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            }
                                            val buttonModifier = Modifier.height(Spacing.space8)
                                            if (isFollowing) {
                                                Button(
                                                    onClick = { onFollowToggle(notification.user.id) },
                                                    modifier = buttonModifier,
                                                    shape = Shapes.cardSmall,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                ) {
                                                    Text(stringResource(id = R.string.notifications_following).uppercase(), fontWeight = FontWeight.Medium, fontSize = 11.sp)
                                                }
                                            } else {
                                                Button(
                                                    onClick = { onFollowToggle(notification.user.id) },
                                                    modifier = buttonModifier,
                                                    shape = Shapes.cardSmall,
                                                    colors = buttonColors
                                                ) {
                                                    Text(stringResource(id = R.string.notifications_follow).uppercase(), fontWeight = FontWeight.Medium, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary)
                                                }
                                            }
                                        },
                                        onClick = { onNotificationClick(notification) }
                                    )
                                }
                                is TimelineNotification.Mention -> {
                                    NotificationRow(
                                        avatarUrl = notification.user.avatarUrl,
                                        title = "@${notification.user.username}",
                                        subtitle = notification.commentText,
                                        isUnread = isUnread,
                                        trailingContent = null,
                                        onClick = { onNotificationClick(notification) }
                                    )
                                }
                                is TimelineNotification.Comment -> {
                                    NotificationRow(
                                        avatarUrl = notification.user.avatarUrl,
                                        title = "@${notification.user.username}",
                                        subtitle = notification.message,
                                        isUnread = isUnread,
                                        trailingContent = null,
                                        onClick = { onNotificationClick(notification) }
                                    )
                                }
                                is TimelineNotification.ListInvite -> {
                                    NotificationRow(
                                        avatarUrl = notification.user.avatarUrl,
                                        title = "@${notification.user.username}",
                                        subtitle = notification.message,
                                        isUnread = isUnread,
                                        trailingContent = null,
                                        onClick = { onNotificationClick(notification) }
                                    )
                                }
                                is TimelineNotification.FirstCoffee -> {
                                    NotificationRow(
                                        avatarUrl = notification.user.avatarUrl,
                                        title = "@${notification.user.username}",
                                        subtitle = stringResource(
                                            id = R.string.notifications_first_coffee_started,
                                            notification.user.fullName.ifBlank { notification.user.username }
                                        ),
                                        isUnread = isUnread,
                                        trailingContent = null,
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
}

@Composable
private fun NotificationRow(
    avatarUrl: String,
    title: String,
    subtitle: String,
    isUnread: Boolean,
    trailingContent: (@Composable () -> Unit)?,
    onClick: () -> Unit
) {
    val unreadColor = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = Shapes.shapeCardMedium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.space3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = stringResource(id = R.string.notifications_avatar),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(10.dp)
                            .background(unreadColor, CircleShape)
                    )
                }
            }
            Spacer(Modifier.width(Spacing.space3))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailingContent != null) {
                Spacer(Modifier.width(Spacing.space3))
                trailingContent()
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
        NotificationSection("today", "notifications_today", today),
        NotificationSection("yesterday", "notifications_yesterday", yesterday),
        NotificationSection("last7", "notifications_last7", last7),
        NotificationSection("last30", "notifications_last30", last30)
    ).filter { it.items.isNotEmpty() }
}

@Composable
fun MentionText(
    text: String,
    style: TextStyle,
    onMentionClick: (String) -> Unit,
    resolveMentionUser: (String) -> UserEntity? = { null },
    modifier: Modifier = Modifier
) {
    val mentionRegex = remember { Regex("@([A-Za-z0-9._-]{2,30})") }
    val visual = remember(text, onMentionClick, resolveMentionUser) {
        val inline = linkedMapOf<String, InlineTextContent>()
        val annotated = buildAnnotatedString {
            var lastIndex = 0
            var mentionIndex = 0
            mentionRegex.findAll(text).forEach { match ->
                if (match.range.first > lastIndex) {
                    append(text.substring(lastIndex, match.range.first))
                }
                val username = match.groupValues[1]
                val id = "mention-display-$mentionIndex-$username"
                inline[id] = InlineTextContent(
                    placeholder = Placeholder(
                        width = ((username.length + 6) * 7f).sp,
                        height = 22.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    val mentionUser = resolveMentionUser(username)
                    Surface(
                        onClick = { onMentionClick(username) },
                        shape = Shapes.card,
                        color = LocalCaramelAccent.current.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier
                                .height(22.dp)
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val fallbackChar = (mentionUser?.username ?: username).take(1).uppercase()
                            if (mentionUser?.avatarUrl.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(LocalCaramelAccent.current.copy(alpha = 0.14f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = fallbackChar,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LocalCaramelAccent.current,
                                        maxLines = 1
                                    )
                                }
                            } else {
                                SubcomposeAsyncImage(
                                    model = mentionUser.avatarUrl,
                                    contentDescription = stringResource(id = R.string.profile_avatar_cd, mentionUser.username.ifBlank { "user" }),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape),
                                    loading = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(LocalCaramelAccent.current.copy(alpha = 0.14f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = fallbackChar,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LocalCaramelAccent.current,
                                                maxLines = 1
                                            )
                                        }
                                    },
                                    error = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(LocalCaramelAccent.current.copy(alpha = 0.14f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = fallbackChar,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LocalCaramelAccent.current,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                )
                            }
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "@$username",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalCaramelAccent.current,
                                lineHeight = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                appendInlineContent(id, "@$username")
                lastIndex = match.range.last + 1
                mentionIndex += 1
            }
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
        ComposerMentionVisual(annotated = annotated, inlineContent = inline)
    }

    Text(
        text = visual.annotated,
        inlineContent = visual.inlineContent,
        style = style,
        modifier = modifier
    )
}

private data class ComposerMentionVisual(
    val annotated: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>
)

@Composable
private fun ComposerMentionPill(user: UserEntity) {
    Surface(
        shape = Shapes.card,
        color = LocalCaramelAccent.current.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .height(22.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fallbackContent: @Composable () -> Unit = {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(LocalCaramelAccent.current.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.username.take(1).uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalCaramelAccent.current,
                        maxLines = 1
                    )
                }
            }
            if (user.avatarUrl.isNullOrBlank()) {
                fallbackContent()
            } else {
                SubcomposeAsyncImage(
                    model = user.avatarUrl,
                    contentDescription = stringResource(id = R.string.profile_avatar_cd, user.username),
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape),
                    loading = { fallbackContent() },
                    error = { fallbackContent() }
                )
            }
            Spacer(Modifier.width(5.dp))
            Text(
                text = "@${user.username}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = LocalCaramelAccent.current,
                lineHeight = 12.sp,
                maxLines = 1
            )
        }
    }
}

private fun buildComposerMentionVisual(
    text: String,
    validUsers: List<UserEntity>
): ComposerMentionVisual {
    val usersByUsername = validUsers.associateBy { it.username.lowercase() }
    val mentionRegex = Regex("(^|\\s)@([A-Za-z0-9._-]{2,30})(?=\\s|$)")
    val inline = linkedMapOf<String, InlineTextContent>()
    val annotated = buildAnnotatedString {
        var lastIndex = 0
        var mentionIndex = 0
        mentionRegex.findAll(text).forEach { match ->
            val leading = match.groupValues.getOrElse(1) { "" }
            val username = match.groupValues.getOrElse(2) { "" }
            val mentionStart = match.range.first + leading.length
            val mentionEnd = match.range.last + 1
            if (mentionStart > lastIndex) {
                append(text.substring(lastIndex, mentionStart))
            }
            val user = usersByUsername[username.lowercase()]
            if (user != null) {
                val id = "mention-inline-$mentionIndex-${user.id}"
                inline[id] = InlineTextContent(
                    placeholder = Placeholder(
                        width = ((username.length + 6) * 7f).sp,
                        height = 22.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    ComposerMentionPill(user = user)
                }
                appendInlineContent(id, "@$username")
            } else {
                append(text.substring(mentionStart, mentionEnd))
            }
            lastIndex = mentionEnd
            mentionIndex += 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
    return ComposerMentionVisual(annotated = annotated, inlineContent = inline)
}

@Composable
fun MentionComposerField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    validUsers: List<UserEntity>,
    modifier: Modifier = Modifier,
    minHeight: Dp = 120.dp
) {
    val textColor = if (isSystemInDarkTheme()) PureWhite else PureBlack
    val visual = remember(value.text, validUsers) {
        buildComposerMentionVisual(text = value.text, validUsers = validUsers)
    }

    Box(modifier = modifier) {
        if (value.text.isNotEmpty()) {
            Text(
                text = visual.annotated,
                inlineContent = visual.inlineContent,
                style = MaterialTheme.typography.bodyLarge.copy(color = textColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.space4, vertical = Spacing.space4)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight),
            shape = Shapes.card,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = if (value.text.isEmpty()) textColor else Color.Transparent
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.Transparent,
                unfocusedTextColor = Color.Transparent,
                cursorColor = textColor
            )
        )
    }
}

@Composable
fun SuggestionChip(user: UserEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = Shapes.card,
        color = LocalCaramelAccent.current.copy(alpha = 0.08f)
    ) {
        Row(modifier = Modifier.padding(horizontal = Spacing.space3, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = user.avatarUrl, contentDescription = stringResource(id = R.string.profile_avatar_cd, user.fullName.ifBlank { user.username }), modifier = Modifier
                .size(20.dp)
                .clip(CircleShape))
            Spacer(Modifier.width(Spacing.space2))
            Text("@${user.username}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LocalCaramelAccent.current)
        }
    }
}

@Composable
fun StockSliderSection(
    label: String,
    value: Float,
    maxValue: Float,
    onValueChange: (Float) -> Unit
) {
    var editableValue by remember(value) { mutableStateOf(value.roundToInt().toString()) }
    val coffeeColor = LocalCaramelAccent.current
    val inactiveTrackColor = if (isSystemInDarkTheme()) SliderTrackInactiveDark else SliderTrackInactiveLight

    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BasicTextField(
            value = editableValue,
            onValueChange = { input ->
                if (input.isEmpty() || input.all { it.isDigit() }) {
                    editableValue = input
                    input.toFloatOrNull()?.let(onValueChange)
                }
            },
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
        Slider(
            value = value.coerceIn(0f, maxValue.coerceAtLeast(1f)),
            onValueChange = {
                onValueChange(it)
                editableValue = it.roundToInt().toString()
            },
            valueRange = 0f..maxValue.coerceAtLeast(1f),
            colors = SliderDefaults.colors(
                thumbColor = coffeeColor,
                activeTrackColor = coffeeColor,
                inactiveTrackColor = inactiveTrackColor
            )
        )
        Spacer(Modifier.height(Spacing.space2))
    }
}

@Composable
fun DetailPremiumBlock(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    DetailPremiumBlock(
        label = label,
        value = value,
        iconPainter = null,
        iconVector = icon,
        modifier = modifier
    )
}

@Composable
fun DetailPremiumBlock(label: String, value: String, icon: Painter, modifier: Modifier = Modifier) {
    DetailPremiumBlock(
        label = label,
        value = value,
        iconPainter = icon,
        iconVector = null,
        modifier = modifier
    )
}

@Composable
private fun DetailPremiumBlock(
    label: String,
    value: String,
    iconPainter: Painter?,
    iconVector: ImageVector?,
    modifier: Modifier = Modifier
) {
    PremiumCard(modifier = modifier) {
        Row(modifier = Modifier.padding(Spacing.space3), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier
                .size(Spacing.space8)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                when {
                    iconPainter != null -> Icon(
                        painter = iconPainter,
                        contentDescription = label,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(Spacing.space6)
                    )
                    iconVector != null -> Icon(
                        imageVector = iconVector,
                        contentDescription = label,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Spacing.space6)
                    )
                }
            }
            Spacer(Modifier.width(Spacing.space3))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                Text(text = value.uppercase(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableDiaryItem(
    entry: DiaryEntryEntity,
    coffeeImageUrl: String? = null,
    highlightNew: Boolean = false,
    selectedPeriod: DiaryPeriod = DiaryPeriod.HOY,
    enableDeleteSwipe: Boolean = true,
    onDelete: () -> Unit,
    onClick: (() -> Unit)? = null
) {
    if (!enableDeleteSwipe) {
        DiaryEntryItem(entry = entry, coffeeImageUrl = coffeeImageUrl, highlightNew = highlightNew, selectedPeriod = selectedPeriod, onClick = onClick)
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false })

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(Shapes.shapeCardMedium)
                    .background(ElectricRed),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.notifications_delete), tint = PureWhite, modifier = Modifier.padding(end = Spacing.space4))
            }
        }
    ) {
        DiaryEntryItem(entry = entry, coffeeImageUrl = coffeeImageUrl, highlightNew = highlightNew, selectedPeriod = selectedPeriod, onClick = onClick)
    }
}

@Composable
fun DiaryEntryItem(
    entry: DiaryEntryEntity,
    coffeeImageUrl: String? = null,
    highlightNew: Boolean = false,
    selectedPeriod: DiaryPeriod = DiaryPeriod.HOY,
    onClick: (() -> Unit)? = null
) {
    val datePattern = if (selectedPeriod == DiaryPeriod.HOY || selectedPeriod == DiaryPeriod.SEMANA || selectedPeriod == DiaryPeriod.MES) "HH:mm" else "dd/MM | HH:mm"
    val dateStr = SimpleDateFormat(datePattern, Locale.getDefault()).format(Date(entry.timestamp))
    val coffeeNameNorm = entry.coffeeName.trim().lowercase()
    val isBrewSinCafe = entry.type == "CUP" && coffeeNameNorm == "café"
    val isRegistroRapido = entry.type == "CUP" && !isBrewSinCafe && (
        entry.coffeeId.isNullOrBlank() ||
        coffeeNameNorm == "registro r\u00E1pido" ||
        coffeeNameNorm == "registro rapido" ||
        Regex("registro\\s*rapido", RegexOption.IGNORE_CASE).containsMatchIn(coffeeNameNorm)
    )
    val rawPreparationType = entry.preparationType.trim()
    val parsed = parsePreparationType(rawPreparationType)
    val elaborationMethod = parsed.method ?: extractDiaryElaborationMethod(rawPreparationType)
    val preparationValue = when {
        elaborationMethod != null -> elaborationMethod
        rawPreparationType.isNotBlank() -> rawPreparationType
        else -> "-"
    }
    val preparationDrawableName = if (elaborationMethod != null) null else diaryPreparationDrawableName(rawPreparationType)
    val elaborationDrawableName = elaborationMethod?.let { diaryElaborationDrawableName(it) }
    val sizeDrawableName = diarySizeDrawableName(entry.sizeLabel, entry.amountMl)
    val preparationDrawablePainter = preparationDrawableName?.let { diaryDrawablePainter(it) }
    val elaborationDrawablePainter = elaborationDrawableName?.let { diaryDrawablePainter(it) }
    val sizeDrawablePainter = diaryDrawablePainter(sizeDrawableName)
    val preparationIcon = diaryPreparationIcon(rawPreparationType)
    val elaborationIcon = elaborationMethod?.let { diaryElaborationIcon(it) }
    val cardColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surface,
        animationSpec = tween(320),
        label = "DiaryCardColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .let { baseModifier ->
                if (onClick != null) baseModifier.clickable(onClick = onClick) else baseModifier
            },
        color = cardColor,
        shape = Shapes.shapeCardMedium
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry.type == "CUP" && !coffeeImageUrl.isNullOrBlank() && !isRegistroRapido && !isBrewSinCafe) {
                    AsyncImage(
                        model = coffeeImageUrl,
                        contentDescription = entry.coffeeName.ifBlank { stringResource(id = R.string.detail_coffee_photo_cd, stringResource(id = R.string.diary_add_coffee)) },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(Shapes.cardSmall),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                if (entry.type == "WATER") WaterBlueBackground.copy(alpha = if (isSystemInDarkTheme()) 0.22f else 1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                Shapes.cardSmall
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (entry.type == "WATER") {
                            Image(
                                painter = painterResource(R.drawable.agua),
                                contentDescription = stringResource(id = R.string.diary_add_water),
                                modifier = Modifier.size(20.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(
                                when {
                                    isBrewSinCafe -> Icons.Default.Coffee
                                    isRegistroRapido -> Icons.Filled.LocalCafe
                                    else -> Icons.Default.Coffee
                                },
                                contentDescription = when {
                                    isBrewSinCafe -> stringResource(id = R.string.diary_add_coffee)
                                    isRegistroRapido -> stringResource(id = R.string.diary_add_quick_entry)
                                    else -> stringResource(id = R.string.diary_add_selection)
                                },
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(Spacing.space6)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(Spacing.space3))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = when {
                            entry.type == "WATER" -> stringResource(id = R.string.diary_add_water)
                            isBrewSinCafe -> stringResource(id = R.string.diary_add_coffee)
                            isRegistroRapido -> entry.preparationType.trim().ifBlank { stringResource(id = R.string.diary_add_quick_entry) }.toCoffeeNameFormat()
                            else -> entry.coffeeName.toCoffeeNameFormat()
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (entry.type != "WATER") {
                        Text(
                            text = if (isRegistroRapido) stringResource(id = R.string.diary_add_coffee).uppercase() else entry.coffeeBrand.ifBlank { stringResource(id = R.string.diary_add_coffee) }.toCoffeeBrandFormat(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "${entry.amountMl} ml",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(dateStr, fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = "HH:mm", modifier = Modifier.size(14.dp)) }
                )
            }

            if (entry.type != "WATER") {
                val metaRowState = rememberLazyListState()
                val metaCarouselEnabled by remember {
                    derivedStateOf {
                        val info = metaRowState.layoutInfo
                        val visible = info.visibleItemsInfo
                        if (info.totalItemsCount == 0 || visible.isEmpty()) return@derivedStateOf true
                        val firstVisible = visible.first().index == 0
                        val lastVisible = visible.last().index == info.totalItemsCount - 1
                        val lastItemEnd = visible.last().offset + visible.last().size
                        !(firstVisible && lastVisible && lastItemEnd <= info.viewportEndOffset)
                    }
                }
                val hasScrollLeft by remember {
                    derivedStateOf {
                        metaRowState.firstVisibleItemIndex > 0 || metaRowState.firstVisibleItemScrollOffset > 0
                    }
                }
                val hasScrollRight by remember {
                    derivedStateOf {
                        val info = metaRowState.layoutInfo
                        if (info.totalItemsCount == 0 || info.visibleItemsInfo.isEmpty()) false
                        else {
                            val last = info.visibleItemsInfo.last()
                            val lastEnd = last.offset + last.size
                            lastEnd > info.viewportEndOffset - 1 || (metaRowState.firstVisibleItemIndex + info.visibleItemsInfo.size < info.totalItemsCount)
                        }
                    }
                }
                val surfaceColor = MaterialTheme.colorScheme.surface
                val metaRowModifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (hasScrollLeft || hasScrollRight) Modifier.drawWithContent {
                            drawContent()
                            val w = size.width
                            val edge = Spacing.space4.toPx().coerceAtMost(w / 4f)
                            if (hasScrollLeft) {
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(surfaceColor, surfaceColor.copy(alpha = 0f))
                                    ),
                                    size = androidx.compose.ui.geometry.Size(edge, size.height)
                                )
                            }
                            if (hasScrollRight) {
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        startX = w - edge,
                                        endX = w,
                                        colors = listOf(surfaceColor.copy(alpha = 0f), surfaceColor)
                                    ),
                                    topLeft = androidx.compose.ui.geometry.Offset(w - edge, 0f),
                                    size = androidx.compose.ui.geometry.Size(edge, size.height)
                                )
                            }
                        } else Modifier
                    )
                LazyRow(
                    state = metaRowState,
                    userScrollEnabled = metaCarouselEnabled,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space6),
                    modifier = metaRowModifier
                ) {
                    item {
                        MetricPill(
                            icon = painterResource(R.drawable.grano_cafe),
                            label = stringResource(id = R.string.diary_stat_caffeine).uppercase(),
                            value = "${entry.caffeineAmount} mg"
                        )
                    }
                    item {
                        MetricPill(
                            icon = painterResource(R.drawable.portafiltro),
                            label = stringResource(id = R.string.diary_stat_dose).uppercase(),
                            value = "${entry.coffeeGrams} g"
                        )
                    }
                    item {
                        if (sizeDrawablePainter != null) {
                            MetricPill(
                                icon = sizeDrawablePainter,
                                label = stringResource(id = R.string.diary_stat_cup_size).uppercase(),
                                value = entry.sizeLabel ?: inferSizeLabel(entry.amountMl)
                            )
                        } else {
                            MetricPill(
                                icon = Icons.Default.LocalCafe,
                                label = stringResource(id = R.string.diary_stat_cup_size).uppercase(),
                                value = entry.sizeLabel ?: inferSizeLabel(entry.amountMl)
                            )
                        }
                    }
                    item {
                        val prepIcon = if (elaborationMethod != null) {
                            if (elaborationDrawablePainter != null) (elaborationDrawablePainter to null) else (null to elaborationIcon)
                        } else {
                            if (preparationDrawablePainter != null) (preparationDrawablePainter to null) else (null to preparationIcon)
                        }
                        if (prepIcon.first != null) {
                            MetricPill(
                                icon = prepIcon.first!!,
                                label = stringResource(id = R.string.diary_stat_method).uppercase(),
                                value = preparationValue
                            )
                        } else {
                            MetricPill(
                                icon = prepIcon.second!!,
                                label = stringResource(id = R.string.diary_stat_method).uppercase(),
                                value = preparationValue
                            )
                        }
                    }
                    if (parsed.tipo != null) {
                        item {
                            val tipoDrawable = diaryDrawablePainter(diaryPreparationDrawableName(parsed.tipo))
                            if (tipoDrawable != null) {
                                MetricPill(icon = tipoDrawable, label = stringResource(id = R.string.diary_add_type).uppercase(), value = parsed.tipo)
                            } else {
                                MetricPill(icon = diaryPreparationIcon(parsed.tipo), label = stringResource(id = R.string.diary_add_type).uppercase(), value = parsed.tipo)
                            }
                        }
                    }
                    if (parsed.taste != null) {
                        item {
                            val tasteIcon = when (parsed.taste.lowercase()) {
                                "dulce" -> Icons.Default.Favorite
                                "amargo" -> Icons.Default.LocalFireDepartment
                                "ácido", "acido" -> Icons.Default.Science
                                "equilibrado" -> Icons.Default.Verified
                                "salado" -> Icons.Default.Waves
                                "acuoso" -> Icons.Default.WaterDrop
                                "aspero", "áspero" -> Icons.Default.Grain
                                else -> Icons.Default.AutoAwesome
                            }
                            MetricPill(icon = tasteIcon, label = stringResource(id = R.string.brew_result).uppercase(), value = parsed.taste)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEntryEditBottomSheet(
    entry: DiaryEntryEntity,
    onDismiss: () -> Unit,
    onSave: (DiaryEntryEntity) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val fieldBackground = if (isDark) PureBlack else PureWhite
    val fieldTextColor = if (isDark) PureWhite else PureBlack
    val editFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = fieldBackground,
        unfocusedContainerColor = fieldBackground,
        focusedBorderColor = LocalCaramelAccent.current,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    )
    val editFieldTextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.Bold,
        color = fieldTextColor
    )
    var amountText by remember(entry.id) { mutableStateOf(entry.amountMl.toString()) }
    var caffeineText by remember(entry.id) { mutableStateOf(entry.caffeineAmount.toString()) }
    var doseText by remember(entry.id) { mutableStateOf(String.format(Locale.getDefault(), "%.1f", entry.coffeeGrams.toFloat())) }
    var timeText by remember(entry.id) {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.timestamp)))
    }
    val sizeOptions = remember {
        listOf(
            SizeOption("Espresso", "25–30 ml", 30, "taza_espresso"),
            SizeOption("Pequeño", "150–200 ml", 180, "taza_pequeno"),
            SizeOption("Mediano", "250–300 ml", 275, "taza_mediano"),
            SizeOption("Grande", "350–400 ml", 375, "taza_grande"),
            SizeOption("Tazón XL", "450–500 ml", 475, "taza_xl")
        )
    }
    var selectedSize by remember(entry.id) {
        mutableStateOf(entry.sizeLabel ?: inferSizeLabel(entry.amountMl))
    }

    val isSpanish = Locale.getDefault().language.startsWith("es")
    val tasteBitter = stringResource(id = R.string.brew_taste_bitter)
    val tasteAcidic = stringResource(id = R.string.brew_taste_acidic)
    val tasteBalanced = stringResource(id = R.string.brew_taste_balanced)
    val tasteSalty = stringResource(id = R.string.brew_taste_salty)
    val tasteWatery = stringResource(id = R.string.brew_taste_watery)
    val tasteRough = stringResource(id = R.string.brew_taste_rough)
    val tasteSweet = stringResource(id = R.string.brew_taste_sweet)
    val errorInvalidTime = if (isSpanish) "Usa un formato de tiempo válido: HH:mm" else "Use a valid time format: HH:mm"
    val errorInvalidAmount = if (isSpanish) "Cantidad inválida" else "Invalid amount"
    val errorInvalidDose = if (isSpanish) "Para espresso, la dosis debe estar entre 3.0 y 30.0 g" else "For espresso, dose must be between 3.0 and 30.0 g"
    val errorFillFields = if (isSpanish) "Completa todos los campos correctamente" else "Complete all fields correctly"

    val (initialMethod, initialTaste, initialTipo) = remember(entry.id) {
        val p = parsePreparationType(entry.preparationType)
        Triple(p.method, p.taste, p.tipo)
    }
    var selectedBrewMethod by remember(entry.id) { mutableStateOf(initialMethod) }
    var selectedBrewTaste by remember(entry.id) { mutableStateOf(initialTaste ?: tasteBalanced) }

    val preparationOptions = remember(entry.id) {
        val base = listOf(
            PrepOption("Espresso", "espresso"),
            PrepOption("Americano", "americano"),
            PrepOption("Capuchino", "capuchino"),
            PrepOption("Latte", "latte"),
            PrepOption("Macchiato", "macchiato"),
            PrepOption("Moca", "moca"),
            PrepOption("Vienés", "vienes"),
            PrepOption("Irlandés", "irlandes"),
            PrepOption("Frappuccino", "frappuccino"),
            PrepOption("Caramelo macchiato", "caramel_macchiato"),
            PrepOption("Corretto", "corretto"),
            PrepOption("Freddo", "freddo"),
            PrepOption("Latte macchiato", "latte_macchiato"),
            PrepOption("Leche con chocolate", "leche_con_chocolate"),
            PrepOption("Marroquí", "marroqui"),
            PrepOption("Romano", "romano"),
            PrepOption("Descafeinado", "descafeinado")
        )
        val prepForTipo = initialTipo?.takeIf { it.isNotBlank() } ?: entry.preparationType.trim().takeIf { it.isNotBlank() } ?: "Espresso"
        if (prepForTipo in base.map { it.label }) base
        else base + PrepOption(prepForTipo, null)
    }
    var selectedPreparation by remember(entry.id) {
        mutableStateOf(initialTipo?.takeIf { it.isNotBlank() } ?: entry.preparationType.trim().ifBlank { "Espresso" })
    }
    var errorText by remember(entry.id) { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun handleSaveEditEntry() {
        val updatedTimestamp = updateTimestampWithHourMinute(entry.timestamp, timeText)
        if (updatedTimestamp == null) {
            errorText = errorInvalidTime
            return
        }

        val updatedEntry = if (entry.type == "WATER") {
            val amount = amountText.toIntOrNull()
            if (amount == null || amount <= 0) {
                errorText = errorInvalidAmount
                return
            }
            entry.copy(amountMl = amount, timestamp = updatedTimestamp)
        } else {
            val caffeine = caffeineText.toIntOrNull()
            val grams = doseText.replace(',', '.').toFloatOrNull()
            val normalizedPrep = selectedPreparation.trim()
            val espressoRangeValid = !normalizedPrep.equals("Espresso", ignoreCase = true) || (grams != null && grams in 3f..30f)
            if (caffeine == null || caffeine < 0 || grams == null || grams <= 0f || normalizedPrep.isBlank() || !espressoRangeValid) {
                errorText = if (!espressoRangeValid) errorInvalidDose else errorFillFields
                return
            }
            val preparationTypeToSave = if (selectedBrewMethod != null) {
                "Lab: $selectedBrewMethod (${selectedBrewTaste.trim().ifBlank { tasteBalanced }})|${normalizedPrep}"
            } else {
                normalizedPrep
            }
            entry.copy(
                caffeineAmount = caffeine,
                coffeeGrams = grams.roundToInt(),
                preparationType = preparationTypeToSave,
                sizeLabel = selectedSize,
                amountMl = sizeOptions.find { it.label == selectedSize }?.defaultMl ?: entry.amountMl,
                timestamp = updatedTimestamp
            )
        }

        errorText = null
        onSave(updatedEntry)
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        dragHandle = { CafesitoModalSheetDefaults.dragHandle() },
        containerColor = CafesitoModalSheetDefaults.containerColor(),
        shape = CafesitoModalSheetDefaults.shape,
        scrimColor = CafesitoModalSheetDefaults.scrimColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = Spacing.space2)
                .padding(bottom = 28.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space6)) {
                Text(
                    text = stringResource(id = R.string.list_edit),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center)
                )
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { handleSaveEditEntry() }) {
                        Text(stringResource(id = R.string.common_save_upper), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(Spacing.space6))

            val brewMethodNamesForWidth = remember { BREW_METHOD_NAMES + listOf(BREW_METHOD_OTROS, BREW_METHOD_AGUA) }
            val editModalChipLabels = remember(brewMethodNamesForWidth, preparationOptions, sizeOptions) {
                brewMethodNamesForWidth + preparationOptions.map { it.label } + sizeOptions.map { it.label } +
                    listOf(tasteBitter, tasteAcidic, tasteBalanced, tasteSalty, tasteWatery, tasteRough, tasteSweet)
            }
            val editModalChipMinWidth = remember(editModalChipLabels) {
                val maxWordLen = editModalChipLabels.flatMap { it.split(" ") }.maxOfOrNull { it.length } ?: 8
                (maxWordLen * 9 + 48).coerceAtLeast(100).dp
            }

            if (entry.type == "CUP") {
                Text(
                    text = stringResource(id = R.string.diary_stat_method),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = Spacing.space6)
                )
                Spacer(Modifier.height(Spacing.space1))
                val brewMethodNames = remember { BREW_METHOD_NAMES + listOf(BREW_METHOD_OTROS, BREW_METHOD_AGUA) }
                val unselectedChipBg = if (isDark) PureBlack else PureWhite
                val unselectedChipContent = if (isDark) PureWhite else PureBlack
                val selectedChipContent = if (isDark) PureBlack else PureWhite
                val chipIconSize = 30.dp
                val chipMinWidth = editModalChipMinWidth
                val chipMinHeight = 56.dp
                FadingLazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    itemSpacing = Spacing.space2,
                    contentPadding = PaddingValues(start = Spacing.space6, end = 0.dp)
                ) {
                    items(brewMethodNames, key = { it }) { methodName ->
                        val isSelected = selectedBrewMethod == methodName
                        val methodDrawable = diaryElaborationDrawableName(methodName)
                        val iconTint = if (isSelected) selectedChipContent else unselectedChipContent
                        Surface(
                            onClick = { selectedBrewMethod = if (selectedBrewMethod == methodName) null else methodName },
                            shape = Shapes.card,
                            color = if (isSelected) LocalCaramelAccent.current else unselectedChipBg,
                            modifier = Modifier.width(chipMinWidth).heightIn(min = chipMinHeight)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = Spacing.space2, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.space2)
                            ) {
                                when {
                                    methodName == BREW_METHOD_AGUA -> Image(
                                        painter = painterResource(R.drawable.agua),
                                        contentDescription = methodName,
                                        modifier = Modifier.size(chipIconSize),
                                        contentScale = ContentScale.Fit
                                    )
                                    methodName == BREW_METHOD_OTROS -> Image(
                                        painter = painterResource(R.drawable.relampago),
                                        contentDescription = methodName,
                                        modifier = Modifier.size(chipIconSize),
                                        contentScale = ContentScale.Fit
                                    )
                                    else -> {
                                        val resId = methodDrawable?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0
                                        if (resId != 0) {
                                            Image(painter = painterResource(id = resId), contentDescription = methodName, modifier = Modifier.size(chipIconSize), contentScale = ContentScale.Fit)
                                        } else {
                                            Icon(Icons.Default.CoffeeMaker, contentDescription = methodName, tint = iconTint, modifier = Modifier.size(chipIconSize))
                                        }
                                    }
                                }
                                Text(
                                    text = methodName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) selectedChipContent else unselectedChipContent,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.space6))
                Text(
                    text = stringResource(id = R.string.diary_stat_format),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = Spacing.space6)
                )
                Spacer(Modifier.height(Spacing.space1))
                FadingLazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    itemSpacing = Spacing.space2,
                    contentPadding = PaddingValues(start = Spacing.space6, end = 0.dp)
                ) {
                    items(preparationOptions, key = { it.label }) { option ->
                        val isSelected = selectedPreparation == option.label
                        val iconTint = if (isSelected) selectedChipContent else unselectedChipContent
                        Surface(
                            onClick = { selectedPreparation = option.label },
                            shape = Shapes.card,
                            color = if (isSelected) LocalCaramelAccent.current else unselectedChipBg,
                            modifier = Modifier.width(chipMinWidth).heightIn(min = chipMinHeight)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = Spacing.space2, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.space2)
                            ) {
                                val resId = option.drawableName?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0
                                if (resId != 0) {
                                    Image(painter = painterResource(id = resId), contentDescription = option.label, modifier = Modifier.size(chipIconSize), contentScale = ContentScale.Fit)
                                } else {
                                    Icon(Icons.Default.CoffeeMaker, contentDescription = option.label, tint = iconTint, modifier = Modifier.size(chipIconSize))
                                }
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) selectedChipContent else unselectedChipContent,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.space6))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space6)) {
                    OutlinedTextField(
                        value = caffeineText,
                        onValueChange = { caffeineText = it.filter(Char::isDigit) },
                        label = { Text("${stringResource(id = R.string.diary_stat_caffeine)} (mg)") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.grano_cafe),
                                contentDescription = stringResource(id = R.string.diary_stat_caffeine),
                                modifier = Modifier.size(20.dp),
                                tint = Color.Unspecified
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = editFieldTextStyle,
                        colors = editFieldColors
                    )

                    OutlinedTextField(
                        value = doseText,
                        onValueChange = { value ->
                            doseText = value.filter { it.isDigit() || it == '.' || it == ',' }.let { filtered ->
                                val normalized = filtered.replace(',', '.')
                                if (normalized.count { it == '.' } <= 1) filtered else filtered.dropLast(1)
                            }
                        },
                        label = { Text("${stringResource(id = R.string.diary_stat_dose)} (g)") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.portafiltro),
                                contentDescription = stringResource(id = R.string.diary_stat_dose),
                                modifier = Modifier.size(20.dp),
                                tint = Color.Unspecified
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = editFieldTextStyle,
                        colors = editFieldColors
                    )
                }

                Spacer(Modifier.height(Spacing.space6))
                Text(
                    text = stringResource(id = R.string.diary_stat_cup_size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = Spacing.space6)
                )
                Spacer(Modifier.height(Spacing.space1))
                FadingLazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    itemSpacing = 10.dp,
                    contentPadding = PaddingValues(start = Spacing.space6, end = 0.dp)
                ) {
                    items(sizeOptions, key = { it.label }) { option ->
                        val isSelected = selectedSize == option.label
                        val chipBg = if (isSelected) LocalCaramelAccent.current else unselectedChipBg
                        val chipContent = if (isSelected) selectedChipContent else unselectedChipContent
                        Surface(
                            onClick = { selectedSize = option.label },
                            shape = Shapes.cardSmall,
                            color = chipBg,
                            modifier = Modifier.width(editModalChipMinWidth)
                        ) {
                            Row(
                                Modifier.padding(horizontal = Spacing.space3, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val sizeResId = context.resources.getIdentifier(option.drawableName, "drawable", context.packageName)
                                if (sizeResId != 0) {
                                    Image(
                                        painter = painterResource(id = sizeResId),
                                        contentDescription = option.label,
                                        modifier = Modifier.size(Spacing.space6),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Icon(Icons.Default.LocalCafe, contentDescription = option.label, modifier = Modifier.size(20.dp), tint = chipContent)
                                }
                                Column {
                                    Text(option.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = chipContent)
                                    Text(option.rangeLabel, style = MaterialTheme.typography.bodySmall, color = chipContent.copy(alpha = 0.9f))
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(Spacing.space6))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space6)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter(Char::isDigit) },
                        label = { Text("${stringResource(id = R.string.diary_add_water)} (${stringResource(id = R.string.diary_ml)})") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = stringResource(id = R.string.diary_add_water),
                                tint = WaterBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = editFieldTextStyle,
                        colors = editFieldColors
                    )
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it.take(5) },
                        label = { Text("HH:mm") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = "HH:mm", modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = editFieldTextStyle,
                        colors = editFieldColors
                    )
                }
            }

            Spacer(Modifier.height(Spacing.space6))
            if (entry.type != "WATER") {
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it.take(5) },
                    label = { Text("HH:mm") },
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = "HH:mm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space6),
                    textStyle = editFieldTextStyle,
                    colors = editFieldColors
                )
            }

            if (entry.type == "CUP" && selectedBrewMethod != null) {
                Spacer(Modifier.height(Spacing.space6))
                Text(
                    text = stringResource(id = R.string.brew_result),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = Spacing.space6)
                )
                Spacer(Modifier.height(Spacing.space1))
                val tasteOptionsWithIcons = listOf(
                    tasteBitter to Icons.Default.LocalFireDepartment,
                    tasteAcidic to Icons.Default.Science,
                    tasteBalanced to Icons.Default.Verified,
                    tasteSalty to Icons.Default.Waves,
                    tasteWatery to Icons.Default.WaterDrop,
                    tasteRough to Icons.Default.Grain,
                    tasteSweet to Icons.Default.Favorite
                )
                val unselectedChipBgResult = if (isDark) PureBlack else PureWhite
                val unselectedChipContentResult = if (isDark) PureWhite else PureBlack
                val selectedChipContentResult = if (isDark) PureBlack else PureWhite
                FadingLazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    itemSpacing = Spacing.space2,
                    contentPadding = PaddingValues(start = Spacing.space6, end = 0.dp)
                ) {
                    items(tasteOptionsWithIcons, key = { it.first }) { (taste, iconVector) ->
                        val isSelected = selectedBrewTaste == taste
                        Surface(
                            onClick = { selectedBrewTaste = taste },
                            shape = Shapes.card,
                            color = if (isSelected) LocalCaramelAccent.current else unselectedChipBgResult,
                            modifier = Modifier.width(editModalChipMinWidth)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = Spacing.space3, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = taste,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) selectedChipContentResult else unselectedChipContentResult
                                )
                                Spacer(Modifier.height(Spacing.space1))
                                Text(
                                    text = taste,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) selectedChipContentResult else unselectedChipContentResult,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = Spacing.space6)
                )
            }

        }
    }
}

private data class PrepOption(
    val label: String,
    val drawableName: String?
)

private data class SizeOption(
    val label: String,
    val rangeLabel: String,
    val defaultMl: Int,
    val drawableName: String
)

/** Resultado de parsear preparation_type: (método, sabor/resultado, tipo de bebida). Formato: "Lab: Método (Sabor)|Tipo". */
private data class ParsedPreparation(val method: String?, val taste: String?, val tipo: String?)

private fun parsePreparationType(raw: String): ParsedPreparation {
    val s = raw.trim()
    if (s.isBlank()) return ParsedPreparation(null, null, null)
    val pipeIdx = s.indexOf('|')
    val tipoPart = if (pipeIdx >= 0) s.substring(pipeIdx + 1).trim().takeIf { it.isNotBlank() } else null
    val leftPart = if (pipeIdx >= 0) s.substring(0, pipeIdx).trim() else s
    val labMatch = Regex("""(?:^lab:\s*|^elaboracion:\s*)([^()]+?)(?:\s*\(([^)]*)\))?\s*$""", RegexOption.IGNORE_CASE).find(leftPart)
    return if (labMatch != null) {
        val method = labMatch.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        val taste = labMatch.groupValues.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
        ParsedPreparation(method, taste, tipoPart)
    } else ParsedPreparation(null, null, tipoPart)
}

private fun extractDiaryElaborationMethod(preparationType: String): String? {
    return parsePreparationType(preparationType).method
        ?: (Regex("""(?:^lab:\s*|^elaboracion:\s*)([^()]+?)(?:\s*\(|$)""", RegexOption.IGNORE_CASE).find(preparationType.trim())
            ?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() })
}

private fun normalizeDiaryLookupText(value: String): String {
    val normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
    return normalized.replace("\\p{M}+".toRegex(), "").lowercase(Locale.getDefault())
}

private fun diaryPreparationDrawableName(preparationType: String): String {
    val normalized = normalizeDiaryLookupText(preparationType)
    return when {
        normalized.contains("americano") -> "americano"
        normalized.contains("capuch") -> "capuchino"
        normalized.contains("caramelo") && normalized.contains("macchi") -> "caramel_macchiato"
        normalized.contains("corretto") -> "corretto"
        normalized.contains("descafe") -> "descafeinado"
        normalized.contains("espresso") -> "espresso"
        normalized.contains("frapp") -> "frappuccino"
        normalized.contains("freddo") -> "freddo"
        normalized.contains("irland") -> "irlandes"
        normalized.contains("latte macchi") -> "latte_macchiato"
        normalized.contains("latte") -> "latte"
        normalized.contains("chocolate") -> "leche_con_chocolate"
        normalized.contains("macchiato") -> "macchiato"
        normalized.contains("marro") -> "marroqui"
        normalized.contains("moca") -> "moca"
        normalized.contains("romano") -> "romano"
        normalized.contains("vien") -> "vienes"
        else -> "maq_manual"
    }
}

private fun diaryElaborationDrawableName(method: String): String? {
    val normalized = normalizeDiaryLookupText(method)
    return when {
        normalized == "otros" -> "relampago"
        normalized == "agua" -> "agua"
        normalized.contains("espresso") -> "maq_espresso"
        normalized.contains("v60") || normalized.contains("hario") -> "maq_hario_v60"
        normalized.contains("aero") -> "maq_aeropress"
        normalized.contains("moka") || normalized.contains("italiana") -> "maq_italiana"
        normalized.contains("chemex") -> "maq_chemex"
        normalized.contains("prensa") -> "maq_prensa_francesa"
        normalized.contains("goteo") -> "maq_goteo"
        normalized.contains("sifon") -> "maq_sifon"
        normalized.contains("turco") -> "maq_turco"
        else -> "maq_manual"
    }
}

private fun diarySizeDrawableName(sizeLabel: String?, amountMl: Int): String {
    val normalized = normalizeDiaryLookupText(sizeLabel?.takeIf { it.isNotBlank() } ?: inferSizeLabel(amountMl))
    return when {
        normalized.contains("espresso") -> "taza_espresso"
        normalized.contains("pequ") -> "taza_pequeno"
        normalized.contains("mediano") -> "taza_mediano"
        normalized.contains("grande") -> "taza_grande"
        normalized.contains("tazon") || normalized.contains("xl") -> "taza_xl"
        else -> "taza_mediano"
    }
}

@Composable
private fun diaryDrawablePainter(drawableName: String?): Painter? {
    if (drawableName.isNullOrBlank()) return null
    val context = LocalContext.current
    val drawableRes = remember(drawableName) {
        context.resources.getIdentifier(drawableName, "drawable", context.packageName)
    }
    return if (drawableRes != 0) painterResource(id = drawableRes) else null
}

private fun diaryPreparationIcon(preparationType: String): ImageVector {
    val normalized = preparationType.trim().lowercase(Locale.getDefault())
    return when {
        normalized.contains("espresso") ||
            normalized.contains("americano") ||
            normalized.contains("capuch") ||
            normalized.contains("latte") ||
            normalized.contains("macchi") -> Icons.Default.CoffeeMaker
        else -> Icons.Default.LocalCafe
    }
}

private fun diaryElaborationIcon(method: String): ImageVector {
    val normalized = method.trim().lowercase(Locale.getDefault())
    return when {
        normalized == "otros" || normalized == "agua" -> Icons.Default.CoffeeMaker
        normalized.contains("espresso") || normalized.contains("moka") -> Icons.Default.CoffeeMaker
        normalized.contains("prensa") -> Icons.Default.LocalCafe
        else -> Icons.Default.Coffee
    }
}

private fun inferSizeLabel(amountMl: Int): String = when (amountMl) {
    in 20..60 -> "Espresso"
    in 150..220 -> "Pequeño"
    in 230..320 -> "Mediano"
    in 330..420 -> "Grande"
    in 430..520 -> "Tazón XL"
    else -> "Mediano"
}

private fun updateTimestampWithHourMinute(currentTimestamp: Long, hourMinute: String): Long? {
    val parts = hourMinute.trim().split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null

    val calendar = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, minute)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

@Composable
private fun MetricPill(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    MetricPillContent(icon = icon, label = label, value = value, modifier = modifier)
}

@Composable
private fun MetricPill(
    icon: Painter,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = Shapes.cardSmall,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = Spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .width(26.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = icon,
                    contentDescription = label,
                    modifier = Modifier.size(Spacing.space6),
                    contentScale = ContentScale.Fit
                )
            }
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MetricPillContent(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = Shapes.cardSmall,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = Spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .width(26.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(Spacing.space6), tint = MaterialTheme.colorScheme.primary)
            }
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CafesitoModalSheetDefaults.containerColor(),
        shape = CafesitoModalSheetDefaults.shape,
        scrimColor = CafesitoModalSheetDefaults.scrimColor,
        dragHandle = { CafesitoModalSheetDefaults.dragHandle() }
    ) {
        Column(Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = Spacing.space6, end = Spacing.space6, bottom = Spacing.space8)) {
            Text(
                text = stringResource(id = R.string.oms_recommendations_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.space6))
            InfoRow(
                stringResource(id = R.string.oms_max_daily_label),
                stringResource(id = R.string.oms_max_daily_value),
                stringResource(id = R.string.oms_max_daily_desc)
            )
            HorizontalDivider(Modifier.padding(vertical = Spacing.space4), color = MaterialTheme.colorScheme.outline)
            InfoRow(
                stringResource(id = R.string.oms_pregnancy_label),
                stringResource(id = R.string.oms_pregnancy_value),
                stringResource(id = R.string.oms_pregnancy_desc)
            )
            HorizontalDivider(Modifier.padding(vertical = Spacing.space4), color = MaterialTheme.colorScheme.outline)
            InfoRow(
                stringResource(id = R.string.oms_hydration_label),
                stringResource(id = R.string.oms_hydration_value),
                stringResource(id = R.string.oms_hydration_desc)
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, desc: String) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        }
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockEditBottomSheet(item: PantryItemWithDetails, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var total by remember { mutableStateOf(item.pantryItem.totalGrams.toFloat()) }
    var rem by remember { mutableStateOf(item.pantryItem.gramsRemaining.toFloat()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CafesitoModalSheetDefaults.containerColor(),
        shape = CafesitoModalSheetDefaults.shape,
        scrimColor = CafesitoModalSheetDefaults.scrimColor,
        dragHandle = { CafesitoModalSheetDefaults.dragHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = Spacing.space6, end = Spacing.space6, bottom = Spacing.space6)
                .padding(bottom = Spacing.space8)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(id = R.string.timeline_edit_stock),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.space8))
            StockSliderSection(
                stringResource(id = R.string.detail_total_coffee_amount),
                total,
                1000f,
                onValueChange = { total = it }
            )
            Spacer(Modifier.height(Spacing.space6))
            StockSliderSection(
                stringResource(id = R.string.detail_remaining_coffee_amount),
                rem,
                total.coerceAtLeast(1f),
                onValueChange = { rem = it }
            )
            Spacer(Modifier.height(40.dp))

            val saveBackground = LocalCaramelAccent.current
            val saveTextColor = if (isSystemInDarkTheme()) PureBlack else PureWhite
            val cancelBorderAndText = MaterialTheme.colorScheme.onSurface

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.space3)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = Shapes.card,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = cancelBorderAndText
                    )
                ) {
                    Text(stringResource(id = R.string.common_cancel_upper), fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = { onSave(total.roundToInt(), rem.roundToInt()) },
                    Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = saveBackground,
                        contentColor = saveTextColor
                    ),
                    shape = Shapes.card
                ) {
                    Text(stringResource(id = R.string.common_save_upper), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ModalMenuOption(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = Shapes.shapeCardMedium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(Spacing.space4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(Spacing.space6)
            )
            Spacer(Modifier.width(Spacing.space4))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Abrir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ModalMenuOption(
    title: String,
    iconPainter: Painter,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = Shapes.shapeCardMedium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(Spacing.space4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(Spacing.space6)
            )
            Spacer(Modifier.width(Spacing.space4))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Abrir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Fila de opción dentro de una card (opciones despensa: Organiza / General). */
@Composable
fun PantryOptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.space4, 14.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(Spacing.space3))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsBottomSheet(
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CafesitoModalSheetDefaults.containerColor(),
        shape = CafesitoModalSheetDefaults.shape,
        scrimColor = CafesitoModalSheetDefaults.scrimColor,
        dragHandle = { CafesitoModalSheetDefaults.dragHandle() }
    ) {
        Column(Modifier.padding(top = 8.dp, start = Spacing.space6, end = Spacing.space6, bottom = 40.dp)) {
            ModalMenuOption(
                title = stringResource(id = R.string.list_edit),
                icon = Icons.Default.Edit,
                color = MaterialTheme.colorScheme.onSurface,
                onClick = onEditClick
            )
            ModalMenuOption(
                title = stringResource(id = R.string.notifications_delete),
                icon = Icons.Default.Delete,
                color = MaterialTheme.colorScheme.onSurface,
                onClick = onDeleteClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onHistorialClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val prefs = remember(context) { context.getSharedPreferences("cafesito_prefs", android.content.Context.MODE_PRIVATE) }
    var dynamicColorEnabled by remember {
        mutableStateOf(prefs.getBoolean(DynamicColorMode.KEY, DynamicColorMode.DEFAULT))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CafesitoModalSheetDefaults.containerColor(),
        shape = CafesitoModalSheetDefaults.shape,
        scrimColor = CafesitoModalSheetDefaults.scrimColor,
        dragHandle = { CafesitoModalSheetDefaults.dragHandle() }
    ) {
        Column(
            Modifier
                .padding(top = 8.dp, start = Spacing.space6, end = Spacing.space6, bottom = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                stringResource(id = R.string.settings_general),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.space2, start = Spacing.space2)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    SettingsOptionRow(
                        icon = Icons.Default.History,
                        title = stringResource(id = R.string.settings_consumed_coffees),
                        onClick = { onDismiss(); onHistorialClick() }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.space4, 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = stringResource(id = R.string.settings_material_you),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(Spacing.space3))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(id = R.string.settings_material_you), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = stringResource(id = R.string.settings_material_you_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = dynamicColorEnabled,
                            onCheckedChange = { enabled ->
                                dynamicColorEnabled = enabled
                                prefs.edit().putBoolean(DynamicColorMode.KEY, enabled).apply()
                            },
                            enabled = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.space4))
            Text(
                stringResource(id = R.string.settings_account),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.space2, start = Spacing.space2, top = Spacing.space2)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    SettingsOptionRow(
                        icon = Icons.Default.Edit,
                        title = stringResource(id = R.string.settings_edit_profile),
                        onClick = { onDismiss(); onEditClick() }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SettingsOptionRow(
                        icon = Icons.Default.Language,
                        title = stringResource(id = R.string.language_title),
                        onClick = { onDismiss(); onLanguageClick() }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SettingsOptionRow(
                        icon = Icons.Default.PersonRemove,
                        title = stringResource(id = R.string.settings_delete_account_data),
                        onClick = { onDismiss(); onDeleteAccountClick() }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SettingsOptionRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = stringResource(id = R.string.settings_sign_out),
                        onClick = { onDismiss(); onLogoutClick() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsOptionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.space4, 14.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(Spacing.space3))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    text: String,
    confirmButtonText: String = stringResource(id = R.string.notifications_delete).uppercase()
) {
    val isDark = isSystemInDarkTheme()
    val cancelColor = if (isDark) PureWhite else PureBlack
    val deleteContainer = ElectricRed
    val deleteContent = if (isDark) PureBlack else PureWhite
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = CafesitoModalSheetDefaults.containerColor(),
        shape = CafesitoModalSheetDefaults.shape,
        scrimColor = CafesitoModalSheetDefaults.scrimColor,
        dragHandle = { CafesitoModalSheetDefaults.dragHandle() }
    ) {
        Column(
            modifier = Modifier.padding(start = Spacing.space6, end = Spacing.space6, top = 8.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.space2))
            Text(
                text = text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.space6))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.space3)
            ) {
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = Shapes.pillFull,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = cancelColor
                    )
                ) {
                    Text(stringResource(id = R.string.common_cancel_upper), fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = {
                        onConfirm()
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = deleteContainer,
                        contentColor = deleteContent
                    ),
                    shape = Shapes.pillFull
                ) {
                    Text(confirmButtonText, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** LazyRow horizontal sin degradados laterales; alineado a la izquierda con el titular, sin padding a la derecha. */
@Composable
fun FadingLazyRow(
    modifier: Modifier = Modifier,
    itemSpacing: Dp = Spacing.space2,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        content = content
    )
}
