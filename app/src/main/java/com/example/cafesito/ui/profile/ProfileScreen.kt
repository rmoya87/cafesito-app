package com.example.cafesito.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.domain.Post
import com.example.cafesito.domain.User
import com.example.cafesito.ui.components.CoffeeCard
import com.example.cafesito.ui.components.PostCard

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onUserClick: (Int) -> Unit,
    onCoffeeClick: (Int) -> Unit,
    onFollowersClick: (Int) -> Unit,
    onFollowingClick: (Int) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is ProfileUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ProfileUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message)
            }
        }
        is ProfileUiState.Success -> {
            ProfileContent(
                state = state,
                onBackClick = onBackClick,
                onUserClick = onUserClick,
                onCoffeeClick = onCoffeeClick,
                onFollowersClick = { onFollowersClick(state.user.id) },
                onFollowingClick = { onFollowingClick(state.user.id) },
                viewModel = viewModel
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileContent(
    state: ProfileUiState.Success,
    onBackClick: () -> Unit,
    onUserClick: (Int) -> Unit,
    onCoffeeClick: (Int) -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    viewModel: ProfileViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Publicaciones", "Favoritos")

    var bio by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }

    LaunchedEffect(state.user) {
        bio = state.user.bio ?: ""
        email = state.user.email
        avatarUrl = state.user.avatarUrl
    }

    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> viewModel.onAvatarChange(uri) }
    )

    if (state.showImageSourceDialog) {
        ImageSourceDialog(
            onDismiss = { viewModel.onDismissImagePicker() },
            onGalleryClick = {
                singlePhotoPickerLauncher.launch(
                    PickVisualMedia.Request(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onCameraClick = { /* TODO: Implement camera functionality */ }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(state.user.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (state.isCurrentUser) {
                        if (state.isEditing) {
                            Row {
                                IconButton(onClick = { viewModel.onSaveProfile(avatarUrl, bio, email) }) {
                                    Icon(Icons.Default.Save, contentDescription = "Guardar perfil", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.toggleEditMode() }) {
                                    Icon(Icons.Default.Cancel, contentDescription = "Cancelar edición")
                                }
                            }
                        } else {
                            IconButton(onClick = { viewModel.toggleEditMode() }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar perfil")
                            }
                        }
                    }
                }
            }

            // User Info
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.clickable(enabled = state.isEditing) { viewModel.onShowImagePicker() }) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar de ${state.user.fullName}",
                                modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                            if (state.isEditing) {
                                Icon(Icons.Default.Edit, "Editar foto", modifier = Modifier.align(Alignment.BottomEnd).background(MaterialTheme.colorScheme.surface, CircleShape).padding(4.dp))
                            }
                        }
                        
                        Spacer(Modifier.width(24.dp))
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceAround) {
                            UserInfoStat("Publicaciones", state.posts.size.toString())
                            UserInfoStat("Seguidores", state.followers.toString(), onClick = onFollowersClick)
                            UserInfoStat("Seguidos", state.following.toString(), onClick = onFollowingClick)
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    Text(state.user.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(4.dp))

                    if (state.isEditing) {
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Descripción") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            isError = state.emailError != null,
                            supportingText = { if (state.emailError != null) Text(state.emailError) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(bio.ifBlank { "Sin descripción." }, style = MaterialTheme.typography.bodyMedium)
                        if (state.isCurrentUser) {
                            Spacer(Modifier.height(8.dp))
                            Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))

                    if (!state.isCurrentUser) {
                        Button(onClick = { /* TODO: Follow logic */ }, modifier = Modifier.fillMaxWidth()) {
                            Text("Seguir")
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            stickyHeader {
                TabRow(selectedTabIndex = selectedTab, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> items(state.posts) { post ->
                    PostCard(
                        post = post,
                        onUserClick = { onUserClick(post.user.id) },
                        onCommentClick = { /* TODO */ },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                1 -> item {
                    if (state.favoriteCoffees.isEmpty()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(32.dp).fillMaxWidth()) {
                            Text("Todavía no hay cafés favoritos.")
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.heightIn(max = 1000.dp) // Adjust height as needed
                        ) {
                            items(state.favoriteCoffees) { coffee ->
                                CoffeeCard(coffee, onClick = { onCoffeeClick(coffee.coffee.id) })
                            }
                        }
                    }
                }
            }
        }

        if (!state.isEditing) {
            IconButton(onClick = onBackClick, modifier = Modifier.padding(8.dp).align(Alignment.TopStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
            }
        }
    }
}

@Composable
private fun UserInfoStat(label: String, value: String, onClick: (() -> Unit)? = null) {
    val modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun ImageSourceDialog(onDismiss: () -> Unit, onGalleryClick: () -> Unit, onCameraClick: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar foto de perfil") },
        text = { Text("¿Desde dónde quieres seleccionar la nueva foto?") },
        confirmButton = {
            TextButton(onClick = onGalleryClick) {
                Text("Galería")
            }
        },
        dismissButton = {
            TextButton(onClick = onCameraClick) {
                Text("Cámara")
            }
        }
    )
}
