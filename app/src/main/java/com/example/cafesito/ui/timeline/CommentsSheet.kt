package com.example.cafesito.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cafesito.data.CommentEntity
import com.example.cafesito.data.SocialRepository
import com.example.cafesito.domain.allUsers
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    postId: String,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit
) {
    // Nota: En una versión final, estos comentarios deberían venir de un ViewModel específico o del SocialRepository
    // Por ahora, usamos un estado local para la visualización inmediata del flujo real
    var comments by remember { mutableStateOf<List<CommentEntity>>(emptyList()) }
    
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
            Text(
                text = "Comentarios",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                if (comments.isEmpty()) {
                    item { Text("No hay comentarios aún. ¡Sé el primero!", color = Color.Gray, modifier = Modifier.padding(vertical = 20.dp)) }
                } else {
                    items(comments) { comment ->
                        CommentItem(comment)
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            AddCommentInput(onAddComment = onAddComment)
        }
    }
}

@Composable
private fun CommentItem(comment: CommentEntity) {
    val author = allUsers.find { it.id == comment.userId }
    Row(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = author?.avatarUrl,
            contentDescription = null,
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.LightGray)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = author?.fullName ?: "Usuario", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AddCommentInput(onAddComment: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Añade un comentario...") },
            modifier = Modifier.weight(1f),
            shape = CircleShape
        )
        IconButton(onClick = { onAddComment(text); text = "" }, enabled = text.isNotBlank()) {
            Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
