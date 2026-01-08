package com.example.cafesito.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import com.example.cafesito.ui.timeline.CommentsSheet
import com.example.cafesito.ui.detail.RatingBar
import com.example.cafesito.ui.theme.CoffeeBrown
import com.example.cafesito.ui.theme.LightGrayBackground
import java.text.SimpleDateFormat
import java.util.Locale

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
            Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ProfileUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
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
    val tabs = listOf("Publicaciones", "Favoritos", "Opiniones")

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
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onCameraClick = { /* TODO */ }
        )
    }

    if (state.activeCommentPost != null) {
        CommentsSheet(
            post = state.activeCommentPost,
            onDismiss = { viewModel.onDismissComments() },
            onAddComment = { text -> viewModel.onAddComment(state.activeCommentPost, text) }
        )
    }

    if (state.postToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text("¿Borrar publicación?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeletePost() }) { 
                    Text("Borrar", color = Color.Red) 
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) { Text("Cancelar") }
            }
        )
    }

    if (state.postToEdit != null) {
        var editedComment by remember { mutableStateOf(state.postToEdit.comment) }
        AlertDialog(
            onDismissRequest = { viewModel.dismissEditPost() },
            title = { Text("Editar publicación") },
            text = {
                OutlinedTextField(
                    value = editedComment,
                    onValueChange = { editedComment = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.savePostEdit(state.postToEdit, editedComment) }) { 
                    Text("Guardar") 
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissEditPost() }) { Text("Cancelar") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(LightGrayBackground)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!state.isCurrentUser && !state.isEditing) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                        }
                    } else if (state.isCurrentUser && !state.isEditing) {
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Text(
                        text = state.user.username,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    if (state.isCurrentUser) {
                        if (state.isEditing) {
                            Row {
                                IconButton(onClick = { viewModel.onSaveProfile(avatarUrl, bio, email) }) {
                                    Icon(Icons.Default.Save, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.toggleEditMode() }) {
                                    Icon(Icons.Default.Cancel, contentDescription = "Cancelar")
                                }
                            }
                        } else {
                            IconButton(onClick = { viewModel.toggleEditMode() }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.background(Color.White).padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.clickable(enabled = state.isEditing) { viewModel.onShowImagePicker() }) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                            if (state.isEditing) {
                                Icon(Icons.Default.Edit, "Editar foto", modifier = Modifier.align(Alignment.BottomEnd).background(MaterialTheme.colorScheme.surface, CircleShape).padding(4.dp))
                            }
                        }
                        
                        Spacer(Modifier.width(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                UserInfoStat("Seguidores", state.followers.toString(), onClick = onFollowersClick)
                                UserInfoStat("Seguidos", state.following.toString(), onClick = onFollowingClick)
                            }
                            
                            if (!state.isCurrentUser) {
                                Spacer(Modifier.height(8.dp))
                                if (state.isFollowing) {
                                    Button(
                                        onClick = { viewModel.toggleFollow() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CoffeeBrown,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text("Siguiendo", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.toggleFollow() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp),
                                        border = BorderStroke(1.dp, CoffeeBrown),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = CoffeeBrown,
                                            containerColor = Color.White
                                        )
                                    ) {
                                        Text("Seguir", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

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
                }
            }

            stickyHeader {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = CoffeeBrown
                ) {
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
                    Box(modifier = Modifier.padding(vertical = 4.dp)) {
                        PostCard(
                            post = post,
                            onUserClick = { onUserClick(post.user.id) },
                            onCommentClick = { viewModel.onCommentClick(post) },
                            modifier = Modifier.background(Color.White),
                            showHeader = false
                        )
                        if (state.isCurrentUser) {
                            var showMenu by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.Gray)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Editar") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.requestEditPost(post)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Borrar", color = Color.Red) },
                                        onClick = {
                                            showMenu = false
                                            viewModel.requestDeletePost(post)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (state.favoriteCoffees.isEmpty()) {
                        item {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                                Text("Todavía no hay cafés favoritos.")
                            }
                        }
                    } else {
                        items(state.favoriteCoffees) { coffeeDetails ->
                            val isMyFavorite = state.myFavoriteIds.contains(coffeeDetails.coffee.id)
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                CoffeeFavoriteItem(
                                    coffeeDetails = coffeeDetails,
                                    isFavorite = isMyFavorite,
                                    onFavoriteClick = { viewModel.onToggleFavorite(coffeeDetails.coffee.id, isMyFavorite) },
                                    onClick = { onCoffeeClick(coffeeDetails.coffee.id) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    if (state.userReviews.isEmpty()) {
                        item {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                                Text("Todavía no hay opiniones.")
                            }
                        }
                    } else {
                        items(state.userReviews) { info ->
                            UserReviewCard(info, onClick = { onCoffeeClick(info.coffeeDetails.coffee.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserReviewCard(info: com.example.cafesito.ui.profile.UserReviewInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            // Header con fondo marrón suave y sin negrita
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CoffeeBrown.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "El usuario ${info.review.user.username} ha opinado",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(info.review.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Línea divisora
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = info.coffeeDetails.coffee.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = info.coffeeDetails.coffee.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = info.coffeeDetails.coffee.brandRoaster,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Highlighted Score
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format("%.1f", info.review.rating),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        RatingBar(rating = info.review.rating, isInteractive = false, starSize = 12.dp)
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Full opinion text with larger typography
                Text(
                    text = info.review.comment,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
fun CoffeeFavoriteItem(
    coffeeDetails: CoffeeWithDetails,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = coffeeDetails.coffee.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = coffeeDetails.coffee.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${coffeeDetails.coffee.brandRoaster} • ${coffeeDetails.origin?.countryName ?: "N/A"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorito",
                tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
