package com.example.cafesito.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(post: Post, onDismiss: () -> Unit, onAddComment: (String) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        windowInsets = WindowInsets.ime,
    ) {
        val maxHeight = screenHeight * 0.7f
        // Box allows layering, with the input field at the bottom.
        Box(modifier = Modifier.heightIn(max = maxHeight)) {
            // The list of comments
            LazyColumn(
                // Add padding to the bottom so the last comment is not hidden by the input field.
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = "Comentarios",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                if (post.comments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxHeight(0.5f)
                                .fillParentMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sé el primero en comentar.")
                        }
                    }
                } else {
                    items(post.comments) { comment ->
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            CommentItem(comment)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            // The input field, aligned to the bottom of the Box.
            AddCommentInput(
                onAddComment = onAddComment,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.surface) // To hide content scrolling behind it
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun CommentItem(comment: Comment) {
    Row(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = comment.user.avatarUrl,
            contentDescription = "Avatar de ${comment.user.name}",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column {
            Text(comment.user.name, fontWeight = FontWeight.Bold)
            Text(comment.text)
        }
    }
}

@Composable
private fun AddCommentInput(onAddComment: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Añade un comentario...") },
            shape = CircleShape,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.size(8.dp))
        IconButton(
            onClick = {
                onAddComment(text)
                text = ""
            },
            enabled = text.isNotBlank()
        ) {
            Icon(Icons.Default.Send, contentDescription = "Enviar comentario")
        }
    }
}
