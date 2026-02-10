package com.cafesito.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cafesito.app.R
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.data.CommentWithAuthor
import com.cafesito.app.data.DiaryEntryEntity
import com.cafesito.app.data.PantryItemWithDetails
import com.cafesito.app.data.PostWithDetails
import com.cafesito.app.data.UserEntity
import com.cafesito.app.data.UserReviewInfo
import com.cafesito.app.ui.brewlab.BrewLabViewModel
import com.cafesito.app.ui.brewlab.BrewMethod
import com.cafesito.app.ui.brewlab.BrewPhaseInfo
import com.cafesito.app.ui.diary.DiaryAnalytics
import com.cafesito.app.ui.diary.DiaryPeriod
import com.cafesito.app.ui.profile.ProfileUiState
import com.cafesito.app.ui.profile.ProfileViewModel
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.timeline.CommentsViewModel
import com.cafesito.app.ui.timeline.TimelineNotification
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

// --- TIMELINE COMPONENTS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    postId: String,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    highlightedCommentId: Int? = null,
    viewModel: CommentsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var text by remember { mutableStateOf("") }
    val comments by viewModel.comments.collectAsState()
    val suggestions by viewModel.mentionSuggestions.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var editingCommentId by remember { mutableStateOf<Int?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(postId) {
        viewModel.setPostId(postId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding() 
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Comentarios",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (comments.isEmpty()) {
                Box(Modifier
                    .fillMaxWidth()
                    .padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                    Text("No hay comentarios todavía", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp)
                ) {
                    items(comments) { item ->
                        CommentRow(
                            commentWithAuthor = item,
                            isOwnComment = item.author?.id == activeUser?.id,
                            isHighlighted = highlightedCommentId == item.comment.id,
                            onNavigateToProfile = onNavigateToProfile,
                            onDeleteClick = { viewModel.deleteComment(item.comment.id) },
                            onEditClick = {
                                editingCommentId = item.comment.id
                                text = item.comment.text
                            },
                            onMentionClick = { username ->
                                scope.launch {
                                    viewModel.getUserIdByUsername(username)?.let { id ->
                                        onNavigateToProfile(id)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (suggestions.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestions) { user ->
                        SuggestionChip(user) {
                            val parts = text.split(" ").toMutableList()
                            parts[parts.size - 1] = "@${user.username} "
                            text = parts.joinToString(" ")
                            viewModel.onTextChanged(text)
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp
            ) {
                Column {
                    if (editingCommentId != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Editando comentario", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = {
                                editingCommentId = null
                                text = ""
                            }, modifier = Modifier.size(16.dp)) {
                                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = {
                                text = it
                                viewModel.onTextChanged(it)
                            },
                            placeholder = { Text("Añade un comentario...") },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (editingCommentId != null) {
                                    viewModel.updateComment(editingCommentId!!, text)
                                    editingCommentId = null
                                } else {
                                    onAddComment(text)
                                }
                                text = ""
                            },
                            enabled = text.isNotBlank(),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(comments, highlightedCommentId) {
        if (highlightedCommentId == null) return@LaunchedEffect
        val index = comments.indexOfFirst { it.comment.id == highlightedCommentId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }
}

@Composable
fun PremiumTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .height(64.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(32.dp))
                .premiumBorder(RoundedCornerShape(32.dp))
                .padding(4.dp)
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
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onTabSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = title, style = MaterialTheme.typography.labelLarge, color = contentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)) {
            Text(
                text = "NOTIFICACIONES",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp)
            )

            if (notifications.isEmpty()) {
                Box(Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text("No tienes notificaciones", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications, key = { it.id }) { notification ->
                        val isUnread = notification.id in unreadIds
                        when (notification) {
                            is TimelineNotification.Follow -> {
                                NotificationRow(
                                    avatarUrl = notification.user.avatarUrl,
                                    title = "${notification.user.fullName} empezó a seguirte.",
                                    subtitle = "@${notification.user.username}",
                                    isUnread = isUnread,
                                    trailingContent = {
                                        val isFollowing = followingIds.contains(notification.user.id)
                                        val buttonColors = if (isFollowing) {
                                            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        }
                                        val buttonModifier = Modifier.height(32.dp)
                                        if (isFollowing) {
                                            OutlinedButton(
                                                onClick = { onFollowToggle(notification.user.id) },
                                                modifier = buttonModifier,
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                                colors = buttonColors
                                            ) {
                                                Text("SIGUIENDO", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        } else {
                                            Button(
                                                onClick = { onFollowToggle(notification.user.id) },
                                                modifier = buttonModifier,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = buttonColors
                                            ) {
                                                Text("SEGUIR", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary)
                                            }
                                        }
                                    },
                                    onClick = { onNotificationClick(notification) }
                                )
                            }
                            is TimelineNotification.Mention -> {
                                val preview = notification.commentText.replace("\n", " ").take(80)
                                NotificationRow(
                                    avatarUrl = notification.user.avatarUrl,
                                    title = "${notification.user.fullName} te mencionó en un comentario.",
                                    subtitle = preview,
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
                    modifier = Modifier
                        .size(44.dp)
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
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailingContent != null) {
                Spacer(Modifier.width(12.dp))
                trailingContent()
            }
        }
    }
}

@Composable
private fun CommentRow(
    commentWithAuthor: CommentWithAuthor,
    isOwnComment: Boolean,
    isHighlighted: Boolean,
    onNavigateToProfile: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onMentionClick: (String) -> Unit
) {
    val author = commentWithAuthor.author
    val comment = commentWithAuthor.comment
    var showMenu by remember { mutableStateOf(false) }

    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                if (isHighlighted) highlightColor else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
    ) {
        AsyncImage(
            model = author?.avatarUrl ?: "",
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable { author?.id?.let { onNavigateToProfile(it) } }
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = author?.username ?: "Usuario",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { author?.id?.let { onNavigateToProfile(it) } }
            )

            val annotatedContent = buildAnnotatedStringWithMentions(comment.text)
            @Suppress("DEPRECATION")
            ClickableText(
                text = annotatedContent,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                onClick = { offset ->
                    annotatedContent.getStringAnnotations("MENTION", offset, offset)
                        .firstOrNull()?.let { annotation ->
                            onMentionClick(annotation.item)
                        }
                }
            )
        }

        if (isOwnComment) {
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        onClick = {
                            showMenu = false
                            onEditClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Borrar", color = ElectricRed) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun buildAnnotatedStringWithMentions(text: String): AnnotatedString {
    return buildAnnotatedString {
        val regex = Regex("@(\\w+)")
        var lastIndex = 0

        regex.findAll(text).forEach { matchResult ->
            append(text.substring(lastIndex, matchResult.range.first))
            val username = matchResult.groupValues[1]
            pushStringAnnotation(tag = "MENTION", annotation = username)
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                append(matchResult.value)
            }
            pop()
            lastIndex = matchResult.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

@Composable
private fun SuggestionChip(user: UserEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = user.avatarUrl, contentDescription = null, modifier = Modifier
                .size(20.dp)
                .clip(CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(user.username, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StockSliderSection(label: String, value: Float, maxValue: Float, onValueChange: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${value.roundToInt()} g", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        Slider(
            value = value, 
            onValueChange = onValueChange, 
            valueRange = 0f..maxValue.coerceAtLeast(1f), 
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
fun DetailPremiumBlock(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    PremiumCard(modifier = modifier) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                Text(text = value.uppercase(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableDiaryItem(entry: DiaryEntryEntity, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false })
    
    SwipeToDismissBox(
        state = dismissState, 
        enableDismissFromStartToEnd = false, 
        backgroundContent = { 
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ElectricRed),
                contentAlignment = Alignment.CenterEnd
            ) { 
                Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.padding(end = 16.dp)) 
            } 
        }
    ) {
        DiaryEntryItem(entry)
    }
}

@Composable
fun DiaryEntryItem(entry: DiaryEntryEntity) {
    val dateStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
    Surface(
        modifier = Modifier.fillMaxWidth(), 
        color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), 
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (entry.type == "WATER") Color(0xFFE3F2FD).copy(alpha = if (isSystemInDarkTheme()) 0.2f else 1f) else MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.15f
                        ),
                        CircleShape
                    ), 
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (entry.type == "WATER") Icons.Default.WaterDrop else Icons.Default.Coffee, 
                    null, 
                    tint = if (entry.type == "WATER") Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurface, 
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.coffeeName, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                Text("$dateStr • ${if (entry.type == "WATER") "${entry.amountMl}ml" else "${entry.caffeineAmount}mg - ${entry.preparationType}"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)) {
            Text(
                text = "Recomendaciones OMS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            InfoRow("Máximo Diario", "400 mg", "Aprox. 4 espressos. Límite seguro para adultos sanos.")
            HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline)
            InfoRow("Embarazo", "200 mg", "Se recomienda reducir el consumo a la mitad.")
            HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline)
            InfoRow("Hidratación", "2.5 L", "El agua es vital. El café deshidrata ligeramente.")
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, desc: String) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Editar Stock",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(32.dp))
            StockSliderSection("Bolsa total", total, 1000f) { total = it; if (rem > it) rem = it }
            Spacer(Modifier.height(24.dp))
            StockSliderSection("Restante", rem, total) { rem = it }
            Spacer(Modifier.height(40.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("CANCELAR", fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { onSave(total.roundToInt(), rem.roundToInt()) }, 
                    Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) { 
                    Text("GUARDAR", fontWeight = FontWeight.Bold) 
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
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
            ModalMenuOption(
                title = "Editar",
                icon = Icons.Default.Edit,
                color = MaterialTheme.colorScheme.primary,
                onClick = onEditClick
            )
            ModalMenuOption(
                title = "Borrar",
                icon = Icons.Default.Delete,
                color = MaterialTheme.colorScheme.primary,
                onClick = onDeleteClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewOptionsBottomSheet(
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
            ModalMenuOption(
                title = "Editar",
                icon = Icons.Default.Edit,
                color = MaterialTheme.colorScheme.primary,
                onClick = onEditClick
            )
            ModalMenuOption(
                title = "Borrar",
                icon = Icons.Default.Delete,
                color = MaterialTheme.colorScheme.primary,
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
    onLogoutClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        containerColor = MaterialTheme.colorScheme.surfaceContainer, 
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(Modifier.padding(bottom = 48.dp, start = 24.dp, end = 24.dp)) {
            Text(
                "GENERAL", 
                style = MaterialTheme.typography.labelMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
            )
            
            ModalMenuOption(
                title = "Editar Perfil",
                icon = Icons.Default.Edit,
                color = MaterialTheme.colorScheme.primary,
                onClick = { onDismiss(); onEditClick() }
            )
            ModalMenuOption(
                title = "Cerrar Sesión",
                icon = Icons.AutoMirrored.Filled.Logout,
                color = MaterialTheme.colorScheme.primary,
                onClick = { onDismiss(); onLogoutClick() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostBottomSheet(
    initialText: String,
    initialImage: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var imageUrl by remember { mutableStateOf(initialImage) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { imageUrl = it.toString() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState, 
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
        ) {
            Text(
                text = "Editar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clickable { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descripción") },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("CANCELAR", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onConfirm(text, imageUrl) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("GUARDAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReviewBottomSheet(
    initialRating: Float,
    initialComment: String,
    initialImage: String?,
    onDismiss: () -> Unit,
    onConfirm: (Float, String, String?) -> Unit
) {
    var rating by remember { mutableFloatStateOf(initialRating) }
    var comment by remember { mutableStateOf(initialComment) }
    var imageUrl by remember { mutableStateOf(initialImage) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { imageUrl = it.toString() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
        ) {
            Text(
                text = "Editar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clickable { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                } else {
                    Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(5) { i ->
                    Icon(
                        imageVector = if (rating > i) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clickable { rating = (i + 1).toFloat() }
                            .size(32.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Tu opinión") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("CANCELAR", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onConfirm(rating, comment, imageUrl) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("ACTUALIZAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    text: String
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 40.dp),
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
            Spacer(Modifier.height(8.dp))
            Text(
                text = text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("CANCELAR", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        onConfirm()
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("ELIMINAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
