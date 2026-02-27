package com.cafesito.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
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
import kotlin.text.Regex
import kotlin.text.RegexOption
import kotlinx.coroutines.launch
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
    var textValue by remember { mutableStateOf(TextFieldValue("")) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePickerSheet by remember { mutableStateOf(false) }
    var showEmojiPanel by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val comments by viewModel.comments.collectAsState()
    val suggestions by viewModel.mentionSuggestions.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var editingCommentId by remember { mutableStateOf<Int?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) selectedImageUri = pendingCameraUri
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) selectedImageUri = uri
    }

    LaunchedEffect(postId) { viewModel.setPostId(postId) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        // scrimColor removed as it caused compilation error
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Text(
                text = "Comentarios",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 16.dp)
            )

            if (comments.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                    Text("No hay comentarios todavía", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 12.dp, top = 8.dp)
                ) {
                    items(comments, key = { it.comment.id }) { item ->
                        CommentRow(
                            commentWithAuthor = item,
                            isOwnComment = item.author?.id == activeUser?.id,
                            isHighlighted = highlightedCommentId == item.comment.id,
                            onNavigateToProfile = onNavigateToProfile,
                            onDeleteClick = { viewModel.deleteComment(item.comment.id) },
                            onEditClick = {
                                editingCommentId = item.comment.id
                                textValue = TextFieldValue(item.comment.text, selection = TextRange(item.comment.text.length))
                            },
                            onMentionClick = { username ->
                                scope.launch {
                                    viewModel.getUserIdByUsername(username)?.let(onNavigateToProfile)
                                }
                            }
                        )
                    }
                }
            }

            if (editingCommentId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Editando comentario", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = {
                        editingCommentId = null
                        textValue = TextFieldValue("")
                    }, modifier = Modifier.size(16.dp)) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            var composerHeight by remember { mutableStateOf(0) }
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Main Composer Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { composerHeight = it.height }
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    // Suggestions/Emojis moved inside at the top
                    AnimatedVisibility(
                        visible = (suggestions.isNotEmpty() && !showEmojiPanel && !textValue.text.trim().endsWith("@")) || showEmojiPanel,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            if (showEmojiPanel) {
                                FadingLazyRow(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    items(listOf("😀", "😍", "🤎", "☕", "🔥", "🙌", "👏", "😋", "🥳", "😎")) { emoji ->
                                        Surface(
                                            onClick = {
                                                val updated = textValue.text + emoji
                                                textValue = TextFieldValue(updated, selection = TextRange(updated.length))
                                                viewModel.onTextChanged(updated)
                                            },
                                            modifier = Modifier.size(40.dp),
                                            shape = CircleShape,
                                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.45f)),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(emoji, fontSize = 16.sp)
                                            }
                                        }
                                    }
                                }
                            } else if (suggestions.isNotEmpty() && !textValue.text.trim().endsWith("@")) {
                                FadingLazyRow(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    items(suggestions, key = { it.id }) { user ->
                                        SuggestionChip(user = user) {
                                            val updated = insertOrReplaceMentionToken(textValue.text, user.username)
                                            textValue = TextFieldValue(updated, selection = TextRange(updated.length))
                                            viewModel.onTextChanged(updated)
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                        }
                    }

                    OutlinedTextField(
                        value = textValue,
                        onValueChange = {
                            textValue = it
                            viewModel.onTextChanged(it.text)
                            if (it.text.endsWith("@")) showEmojiPanel = false
                        },
                        placeholder = { Text("Añade un comentario...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showImagePickerSheet = true },
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.45f)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Surface(
                            onClick = {
                                val updated = textValue.text + "@"
                                textValue = TextFieldValue(updated, selection = TextRange(updated.length))
                                viewModel.onTextChanged(updated)
                                showEmojiPanel = false
                                keyboardController?.show()
                            },
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.45f)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("@", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Surface(
                            onClick = { showEmojiPanel = !showEmojiPanel },
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.45f)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("😊", fontSize = 16.sp)
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        IconButton(
                            onClick = {
                                if (editingCommentId != null) {
                                    viewModel.updateComment(editingCommentId!!, textValue.text)
                                    editingCommentId = null
                                } else {
                                    onAddComment(textValue.text)
                                }
                                textValue = TextFieldValue("")
                                selectedImageUri = null
                            },
                            enabled = textValue.text.isNotBlank(),
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", modifier = Modifier.size(24.dp))
                        }
                    }

                    selectedImageUri?.let { uri ->
                        Box(modifier = Modifier.padding(12.dp).size(88.dp).clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(model = uri, contentDescription = "Miniatura", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clickable { selectedImageUri = null },
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.65f)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImagePickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImagePickerSheet = false }, 
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            // scrimColor removed
        ) {
            Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
                Text("AÑADIR FOTO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
                ModalMenuOption("Hacer Foto", Icons.Default.PhotoCamera, MaterialTheme.colorScheme.primary) {
                    val file = File(context.cacheDir, "captured_${UUID.randomUUID()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
                    showImagePickerSheet = false
                }
                ModalMenuOption("Elegir de Galería", Icons.Default.Collections, MaterialTheme.colorScheme.primary) {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    showImagePickerSheet = false
                }
            }
        }
    }

    LaunchedEffect(comments, highlightedCommentId) {
        if (highlightedCommentId == null) return@LaunchedEffect
        val index = comments.indexOfFirst { it.comment.id == highlightedCommentId }
        if (index >= 0) listState.animateScrollToItem(index)
    }
}

private fun insertOrReplaceMentionToken(currentText: String, username: String): String {
    val parts = currentText.trimEnd().split(" ").toMutableList()
    if (parts.isEmpty() || parts.firstOrNull().isNullOrBlank()) {
        return "@$username "
    }

    val lastIndex = parts.lastIndex
    parts[lastIndex] = if (parts[lastIndex].startsWith("@")) "@$username" else "${parts[lastIndex]} @$username"
    return parts.joinToString(" ") + " "
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
        // scrimColor removed
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
                                    title = "@${notification.user.username}",
                                    subtitle = "ha comenzado a seguirte",
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
                                NotificationRow(
                                    avatarUrl = notification.user.avatarUrl,
                                    title = "@${notification.user.username}",
                                    subtitle = "Te han mencionado.",
                                    isUnread = isUnread,
                                    trailingContent = null,
                                    onClick = { onNotificationClick(notification) }
                                )
                            }
                            is TimelineNotification.Comment -> {
                                NotificationRow(
                                    avatarUrl = notification.user.avatarUrl,
                                    title = "@${notification.user.username}",
                                    subtitle = "Te ha escrito en un post",
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
    var showOptionsSheet by remember { mutableStateOf(false) }

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

            MentionText(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                onMentionClick = onMentionClick
            )
        }

        if (isOwnComment) {
            IconButton(onClick = { showOptionsSheet = true }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
    }

    if (showOptionsSheet) {
        PostOptionsBottomSheet(
            onDismiss = { showOptionsSheet = false },
            onEditClick = {
                showOptionsSheet = false
                onEditClick()
            },
            onDeleteClick = {
                showOptionsSheet = false
                onDeleteClick()
            }
        )
    }

}

private fun buildAnnotatedStringWithMentions(text: String, mentionColor: Color): AnnotatedString {
    return buildAnnotatedString {
        val regex = Regex("@([A-Za-z0-9._-]{2,30})")
        var lastIndex = 0

        regex.findAll(text).forEach { matchResult ->
            append(text.substring(lastIndex, matchResult.range.first))
            val username = matchResult.groupValues[1]
            pushStringAnnotation(tag = "MENTION", annotation = username)
            withStyle(style = SpanStyle(color = mentionColor, fontWeight = FontWeight.Bold)) {
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
fun MentionText(
    text: String,
    style: TextStyle,
    onMentionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val mentionColor = MaterialTheme.colorScheme.primary
    val annotatedContent = remember(text, mentionColor) { buildAnnotatedStringWithMentions(text, mentionColor) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotatedContent,
        style = style,
        modifier = modifier.pointerInput(annotatedContent) {
            detectTapGestures { tapOffset ->
                val layoutResult = textLayoutResult ?: return@detectTapGestures
                val offset = layoutResult.getOffsetForPosition(tapOffset)
                annotatedContent
                    .getStringAnnotations(tag = "MENTION", start = offset, end = offset)
                    .firstOrNull()
                    ?.let { onMentionClick(it.item) }
            }
        },
        onTextLayout = { textLayoutResult = it }
    )
}

@Composable
fun SuggestionChip(user: UserEntity, onClick: () -> Unit) {
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
fun StockSliderSection(
    label: String,
    value: Float,
    maxValue: Float,
    onValueChange: (Float) -> Unit
) {
    var editableValue by remember(value) { mutableStateOf(value.roundToInt().toString()) }

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
                thumbColor = MaterialTheme.colorScheme.outline,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
        )
        Spacer(Modifier.height(8.dp))
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
                    .clip(RoundedCornerShape(20.dp))
                    .background(ElectricRed),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.padding(end = 16.dp))
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
    val isRegistroRapido = entry.type == "CUP" && (
        entry.coffeeId.isNullOrBlank() ||
        coffeeNameNorm == "registro r\u00E1pido" ||
        coffeeNameNorm == "registro rapido" ||
        Regex("registro\\s*rapido", RegexOption.IGNORE_CASE).containsMatchIn(coffeeNameNorm)
    )
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
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry.type == "CUP" && !coffeeImageUrl.isNullOrBlank() && !isRegistroRapido) {
                    AsyncImage(
                        model = coffeeImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                if (entry.type == "WATER") Color(0xFFE3F2FD).copy(alpha = if (isSystemInDarkTheme()) 0.22f else 1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                entry.type == "WATER" -> Icons.Default.WaterDrop
                                isRegistroRapido -> Icons.Filled.LocalCafe
                                else -> Icons.Default.Coffee
                            },
                            null,
                            tint = if (entry.type == "WATER") Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = when {
                            entry.type == "WATER" -> "Agua"
                            isRegistroRapido -> entry.preparationType.trim().ifBlank { "Registro r\u00E1pido" }.toCoffeeNameFormat()
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
                            text = if (isRegistroRapido) "CAFÉ" else entry.coffeeBrand.ifBlank { "Café" }.toCoffeeBrandFormat(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(dateStr, fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp)) }
                )
            }

            if (entry.type == "WATER") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricPill(
                        icon = Icons.Default.LocalDrink,
                        label = "Cantidad",
                        value = "${entry.amountMl} ml",
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        MetricPill(
                            icon = Icons.Default.Bolt,
                            label = "Cafeína",
                            value = "${entry.caffeineAmount} mg"
                        )
                    }
                    item {
                        MetricPill(
                            icon = Icons.Default.CoffeeMaker,
                            label = "Preparación",
                            value = entry.preparationType
                        )
                    }
                    item {
                        MetricPill(
                            icon = Icons.Default.Scale,
                            label = "Dosis",
                            value = "${entry.coffeeGrams} g"
                        )
                    }
                    item {
                        MetricPill(
                            icon = Icons.Default.LocalCafe,
                            label = "Tamaño",
                            value = entry.sizeLabel ?: inferSizeLabel(entry.amountMl)
                        )
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
        if (entry.preparationType in base.map { it.label }) base
        else base + PrepOption(entry.preparationType, null)
    }
    var selectedPreparation by remember(entry.id) { mutableStateOf(entry.preparationType) }
    var errorText by remember(entry.id) { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (entry.type == "WATER") "Editar registro de agua" else "Editar registro de café",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (entry.type == "CUP") {
                Text(
                    text = "Preparación",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(preparationOptions, key = { it.label }) { option ->
                        val isSelected = selectedPreparation == option.label
                        Surface(
                            onClick = { selectedPreparation = option.label },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) CaramelAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val resId = option.drawableName?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0
                                if (resId != 0) {
                                    Image(
                                        painter = painterResource(id = resId),
                                        contentDescription = option.label,
                                        modifier = Modifier.size(28.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Icon(Icons.Default.CoffeeMaker, null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = caffeineText,
                        onValueChange = { caffeineText = it.filter(Char::isDigit) },
                        label = { Text("Cafeína (mg)") },
                        leadingIcon = { Icon(Icons.Default.Bolt, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = doseText,
                        onValueChange = { value ->
                            doseText = value.filter { it.isDigit() || it == '.' }.let { filtered ->
                                if (filtered.count { it == '.' } <= 1) filtered else filtered.dropLast(1)
                            }
                        },
                        label = { Text("Dosis (g)") },
                        leadingIcon = { Icon(Icons.Default.Scale, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Tamaño",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(sizeOptions, key = { it.label }) { option ->
                        val isSelected = selectedSize == option.label
                        Surface(
                            onClick = { selectedSize = option.label },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) CaramelAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val sizeResId = context.resources.getIdentifier(option.drawableName, "drawable", context.packageName)
                                    if (sizeResId != 0) {
                                        Image(
                                            painter = painterResource(id = sizeResId),
                                            contentDescription = option.label,
                                            modifier = Modifier.size(22.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Icon(Icons.Default.LocalCafe, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Text(option.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                }
                                Text(option.rangeLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit) },
                    label = { Text("Cantidad (ml)") },
                    leadingIcon = { Icon(Icons.Default.LocalDrink, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = timeText,
                onValueChange = { timeText = it.take(5) },
                label = { Text("Tiempo (HH:mm)") },
                leadingIcon = { Icon(Icons.Default.Schedule, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    val updatedTimestamp = updateTimestampWithHourMinute(entry.timestamp, timeText)
                    if (updatedTimestamp == null) {
                        errorText = "Usa un formato de tiempo válido: HH:mm"
                        return@Button
                    }

                    val updatedEntry = if (entry.type == "WATER") {
                        val amount = amountText.toIntOrNull()
                        if (amount == null || amount <= 0) {
                            errorText = "Cantidad inválida"
                            return@Button
                        }
                        entry.copy(amountMl = amount, timestamp = updatedTimestamp)
                    } else {
                        val caffeine = caffeineText.toIntOrNull()
                        val grams = doseText.toFloatOrNull()
                        val normalizedPrep = selectedPreparation.trim()
                        val espressoRangeValid = !normalizedPrep.equals("Espresso", ignoreCase = true) || (grams != null && grams in 3f..30f)
                        if (caffeine == null || caffeine < 0 || grams == null || grams <= 0f || normalizedPrep.isBlank() || !espressoRangeValid) {
                            errorText = if (!espressoRangeValid) "Para espresso, la dosis debe estar entre 3.0 y 30.0 g" else "Completa todos los campos correctamente"
                            return@Button
                        }
                        entry.copy(
                            caffeineAmount = caffeine,
                            coffeeGrams = grams.roundToInt(),
                            preparationType = selectedPreparation.trim(),
                            sizeLabel = selectedSize,
                            amountMl = sizeOptions.find { it.label == selectedSize }?.defaultMl ?: entry.amountMl,
                            timestamp = updatedTimestamp
                        )
                    }

                    errorText = null
                    onSave(updatedEntry)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .requiredHeight(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Guardar cambios")
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
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
            StockSliderSection(
                "Cantidad de café total (g)",
                total,
                1000f,
                onValueChange = { total = it }
            )
            Spacer(Modifier.height(24.dp))
            StockSliderSection(
                "Cantidad de café restante (g)",
                rem,
                total.coerceAtLeast(1f),
                onValueChange = { rem = it }
            )
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
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        scrimColor = Color.Black.copy(alpha = 0.5f)
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
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        scrimColor = Color.Black.copy(alpha = 0.5f)
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
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        scrimColor = Color.Black.copy(alpha = 0.5f)
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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrimColor = Color.Black.copy(alpha = 0.5f)
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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrimColor = Color.Black.copy(alpha = 0.5f)
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

@Composable
fun FadingLazyRow(
    modifier: Modifier = Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
    val showLeftGradient by remember {
        derivedStateOf { scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 0 }
    }
    val showRightGradient by remember {
        derivedStateOf {
            val layoutInfo = scrollState.layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            if (totalItemsCount == 0) false
            else {
                val lastItem = layoutInfo.visibleItemsInfo.lastOrNull()
                lastItem == null || lastItem.index < totalItemsCount - 1 || lastItem.offset + lastItem.size > layoutInfo.viewportEndOffset
            }
        }
    }

    androidx.compose.foundation.lazy.LazyRow(
        state = scrollState,
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val leftColor = if (showLeftGradient) Color.Transparent else Color.Black
                val rightColor = if (showRightGradient) Color.Transparent else Color.Black
                
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        0.0f to leftColor,
                        0.05f to Color.Black,
                        0.95f to Color.Black,
                        1.0f to rightColor
                    ),
                    blendMode = BlendMode.DstIn
                )
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}
