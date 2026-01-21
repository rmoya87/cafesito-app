package com.example.cafesito.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.data.PostWithDetails
import com.example.cafesito.data.UserReviewInfo
import com.example.cafesito.ui.components.PostCard
import com.example.cafesito.ui.components.UserReviewCard
import com.example.cafesito.ui.theme.CoffeeBrown
import com.example.cafesito.ui.timeline.CommentsSheet
import com.example.cafesito.ui.timeline.EditPostDialog
import com.example.cafesito.ui.timeline.EditReviewDialog

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onUserClick: (Int) -> Unit,
    onCoffeeClick: (String) -> Unit,
    onFollowersClick: (Int) -> Unit,
    onFollowingClick: (Int) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    val tabs = listOf("Posts", "Favoritos", "Opiniones")
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    var showCommentSheetId by remember { mutableStateOf<String?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // ESTADOS DE EDICIÓN Y BORRADO
    var postToEdit by remember { mutableStateOf<PostWithDetails?>(null) }
    var reviewToEdit by remember { mutableStateOf<UserReviewInfo?>(null) }
    var itemToDelete by remember { mutableStateOf<Any?>(null) }

    // Forzar refresco al entrar en la pantalla
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    // Lanzar SnackBar si hay un error
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is ProfileUiState.Success && state.errorMessage != null) {
            snackbarHostState.showSnackbar(state.errorMessage)
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text("Editar mi perfil") },
                    leadingContent = { Icon(Icons.Default.Edit, null) },
                    modifier = Modifier.clickable { 
                        showSettingsSheet = false
                        viewModel.toggleEditMode() 
                    }
                )
                ListItem(
                    headlineContent = { Text("Cerrar sesión", color = Color.Red) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Red) },
                    modifier = Modifier.clickable { 
                        showSettingsSheet = false
                        viewModel.logout() 
                    }
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8F8F8)
    ) { padding ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is ProfileUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.message) }
            is ProfileUiState.Success -> {
                var username by remember { mutableStateOf(state.user.username) }
                var fullName by remember { mutableStateOf(state.user.fullName) }
                var bio by remember { mutableStateOf(state.user.bio ?: "") }
                var email by remember { mutableStateOf(state.user.email) }
                
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { viewModel.onAvatarChange(it) }

                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!state.isCurrentUser) {
                                        IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.Black) }
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = state.user.username, 
                                        style = MaterialTheme.typography.titleLarge, 
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Spacer(Modifier.weight(1f))
                                    if (state.isCurrentUser) {
                                        if (state.isEditing) {
                                            TextButton(onClick = { viewModel.onSaveProfile(username, fullName, bio, email) }) {
                                                Text("Guardar", color = CoffeeBrown, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            IconButton(onClick = { showSettingsSheet = true }) {
                                                Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = Color.Black)
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.toggleFollow() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (state.isFollowing) Color(0xFFE0E0E0) else CoffeeBrown,
                                                contentColor = if (state.isFollowing) Color.DarkGray else Color.White
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text(
                                                text = if (state.isFollowing) "Siguiendo" else "Seguir",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp)) {
                                    Box {
                                        AsyncImage(
                                            model = state.user.avatarUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.LightGray),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (state.isEditing) {
                                            FilledIconButton(
                                                onClick = { launcher.launch("image/*") },
                                                modifier = Modifier.size(28.dp).align(Alignment.BottomEnd),
                                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CoffeeBrown)
                                            ) { Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White) }
                                        }
                                    }
                                    Spacer(Modifier.width(24.dp))
                                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        UserInfoStat("Posts", state.posts.size.toString())
                                        UserInfoStat("Seguidores", state.followers.toString(), onClick = { onFollowersClick(state.user.id) })
                                        UserInfoStat("Siguiendo", state.following.toString(), onClick = { onFollowingClick(state.user.id) })
                                    }
                                }

                                if (state.isEditing) {
                                    OutlinedTextField(
                                        value = username,
                                        onValueChange = { username = it },
                                        label = { Text("Nombre de usuario") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        isError = state.usernameError != null,
                                        supportingText = { if (state.usernameError != null) Text(state.usernameError!!) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                    )
                                    OutlinedTextField(
                                        value = fullName,
                                        onValueChange = { fullName = it },
                                        label = { Text("Nombre completo") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, capitalization = KeyboardCapitalization.Words)
                                    )
                                    OutlinedTextField(
                                        value = bio,
                                        onValueChange = { bio = it },
                                        label = { Text("Biografía") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3,
                                        singleLine = false,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default, capitalization = KeyboardCapitalization.Sentences)
                                    )
                                } else {
                                    Text(text = state.user.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                                    if (bio.isNotBlank()) {
                                        Text(bio, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }

                        stickyHeader {
                            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White, contentColor = CoffeeBrown) {
                                tabs.forEachIndexed { index, title ->
                                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                                }
                            }
                        }

                        when (selectedTab) {
                            0 -> items(state.posts) { details ->
                                PostCard(
                                    details = details,
                                    onUserClick = { onUserClick(details.author.id) },
                                    onCommentClick = { showCommentSheetId = details.post.id },
                                    onLikeClick = { viewModel.onToggleLike(details.post.id) },
                                    isLiked = state.activeUser?.let { me -> 
                                        details.likes.any { it.userId == me.id } 
                                    } ?: false, 
                                    showHeader = false,
                                    isOwnPost = state.isCurrentUser,
                                    onEditClick = { postToEdit = details },
                                    onDeleteClick = { itemToDelete = details }
                                )
                            }
                            1 -> items(state.favoriteCoffees) { coffeeDetails ->
                                CoffeeFavoriteItem(
                                    coffeeDetails = coffeeDetails,
                                    isFavorite = true,
                                    onFavoriteClick = { viewModel.onToggleFavorite(coffeeDetails.coffee.id, false) },
                                    onClick = { onCoffeeClick(coffeeDetails.coffee.id) }
                                )
                            }
                            2 -> items(state.userReviews) { info ->
                                UserReviewCard(
                                    info = info, 
                                    showHeader = false, 
                                    isOwnReview = state.isCurrentUser,
                                    onEditClick = { reviewToEdit = info },
                                    onDeleteClick = { itemToDelete = info },
                                    onClick = { onCoffeeClick(info.coffeeDetails.coffee.id) }
                                )
                            }
                        }
                    }

                    // DIÁLOGOS DE GESTIÓN EN PERFIL
                    postToEdit?.let { details ->
                        EditPostDialog(
                            initialText = details.post.comment,
                            initialImage = details.post.imageUrl,
                            onDismiss = { postToEdit = null },
                            onConfirm = { newText, newImageUrl ->
                                viewModel.updatePost(details.post.id, newText, newImageUrl)
                                postToEdit = null
                            }
                        )
                    }

                    reviewToEdit?.let { info ->
                        EditReviewDialog(
                            initialRating = info.review.rating,
                            initialComment = info.review.comment,
                            initialImage = info.review.imageUrl,
                            onDismiss = { reviewToEdit = null },
                            onConfirm = { rating, comment, imageUrl ->
                                viewModel.updateReview(info.coffeeDetails.coffee.id, rating, comment, imageUrl)
                                reviewToEdit = null
                            }
                        )
                    }

                    itemToDelete?.let { item ->
                        AlertDialog(
                            onDismissRequest = { itemToDelete = null },
                            title = { Text("¿Eliminar publicación?") },
                            text = { Text("Esta acción borrará el contenido de forma permanente.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    if (item is PostWithDetails) viewModel.deletePost(item.post.id)
                                    else if (item is UserReviewInfo) viewModel.deleteReview(item.coffeeDetails.coffee.id)
                                    itemToDelete = null
                                }) { Text("Eliminar", color = Color.Red) }
                            },
                            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("Cancelar") } }
                        )
                    }

                    showCommentSheetId?.let { id ->
                        CommentsSheet(
                            postId = id,
                            onDismiss = { showCommentSheetId = null },
                            onAddComment = { text -> viewModel.onAddComment(id, text) },
                            onNavigateToProfile = { userId ->
                                showCommentSheetId = null
                                onUserClick(userId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CoffeeFavoriteItem(coffeeDetails: CoffeeWithDetails, isFavorite: Boolean, onFavoriteClick: () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = coffeeDetails.coffee.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = coffeeDetails.coffee.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(text = "${coffeeDetails.coffee.marca}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            IconButton(onClick = onFavoriteClick) { 
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                    contentDescription = "Favorito", 
                    tint = if (isFavorite) Color.Red else Color.Gray 
                ) 
            }
        }
    }
}

@Composable
private fun UserInfoStat(label: String, value: String, onClick: (() -> Unit)? = null) {
    val modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 12.sp)
    }
}
