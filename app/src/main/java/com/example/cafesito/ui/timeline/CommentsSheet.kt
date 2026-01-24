
package com.example.cafesito.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CommentWithAuthor
import com.example.cafesito.data.UserEntity
import com.example.cafesito.ui.theme.CoffeeBrown
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    postId: String,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    viewModel: CommentsViewModel = hiltViewModel()
) {
    var text by remember { mutableStateOf("") }
    val comments by viewModel.comments.collectAsState()
    val suggestions by viewModel.mentionSuggestions.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Estado para edición
    var editingCommentId by remember { mutableStateOf<Int?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = comments.size > 5)

    LaunchedEffect(postId) {
        viewModel.setPostId(postId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Comentarios", 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // Comentarios
            if (comments.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                    Text("No hay comentarios todavía", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp)
                ) {
                    items(comments) { item ->
                        CommentRow(
                            commentWithAuthor = item,
                            isOwnComment = item.author.id == activeUser?.id,
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

            // Sugerencias de menciones
            if (suggestions.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF8F8F8)).padding(8.dp),
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

            // INPUT FIJO
            Surface(
                color = Color.White
            ) {
                Column {
                    if (editingCommentId != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(CoffeeBrown.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Editando comentario", style = MaterialTheme.typography.labelSmall, color = CoffeeBrown)
                            IconButton(onClick = { 
                                editingCommentId = null
                                text = ""
                            }, modifier = Modifier.size(16.dp)) {
                                Icon(Icons.Default.Close, null, tint = CoffeeBrown)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth()
                            .navigationBarsPadding(),
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
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CoffeeBrown)
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
                            colors = IconButtonDefaults.iconButtonColors(contentColor = CoffeeBrown)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    commentWithAuthor: CommentWithAuthor,
    isOwnComment: Boolean,
    onNavigateToProfile: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onMentionClick: (String) -> Unit
) {
    val author = commentWithAuthor.author
    val comment = commentWithAuthor.comment
    var showMenu by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        AsyncImage(
            model = author.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable { onNavigateToProfile(author.id) }
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = author.username, 
                fontWeight = FontWeight.Bold, 
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { onNavigateToProfile(author.id) }
            )
            
            val annotatedContent = buildAnnotatedStringWithMentions(comment.text)
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
                        text = { Text("Borrar", color = Color.Red) },
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

private fun buildAnnotatedStringWithMentions(text: String): AnnotatedString {
    return buildAnnotatedString {
        val regex = Regex("@(\\w+)")
        var lastIndex = 0
        
        regex.findAll(text).forEach { matchResult ->
            append(text.substring(lastIndex, matchResult.range.first))
            val username = matchResult.groupValues[1]
            pushStringAnnotation(tag = "MENTION", annotation = username)
            withStyle(style = SpanStyle(color = CoffeeBrown, fontWeight = FontWeight.Bold)) {
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
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = user.avatarUrl, contentDescription = null, modifier = Modifier.size(20.dp).clip(CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(user.username, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
