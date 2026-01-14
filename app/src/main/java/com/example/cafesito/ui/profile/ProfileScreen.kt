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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.cafesito.ui.components.PostCard
import com.example.cafesito.ui.components.UserReviewCard
import com.example.cafesito.ui.theme.CoffeeBrown
import com.example.cafesito.ui.timeline.CommentsSheet

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
    
    // Estado para manejar la hoja de comentarios
    var showCommentSheetId by remember { mutableStateOf<String?>(null) }

    when (val state = uiState) {
        is ProfileUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is ProfileUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message) }
        is ProfileUiState.Success -> {
            var username by remember { mutableStateOf(state.user.username) }
            var fullName by remember { mutableStateOf(state.user.fullName) }
            var bio by remember { mutableStateOf(state.user.bio ?: "") }
            var email by remember { mutableStateOf(state.user.email) }
            
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { viewModel.onAvatarChange(it) }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize().background(Color(0xFFF8F8F8))
                ) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                                Spacer(Modifier.width(8.dp))
                                Text(text = state.user.username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                if (state.isCurrentUser) {
                                    TextButton(onClick = { 
                                        if (state.isEditing) viewModel.onSaveProfile(username, fullName, bio, email)
                                        else viewModel.toggleEditMode()
                                    }) {
                                        Text(if (state.isEditing) "Guardar" else "Editar Perfil")
                                    }
                                } else {
                                    // Botón de seguir para perfiles de otros usuarios
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
                                    supportingText = { if (state.usernameError != null) Text(state.usernameError!!) }
                                )
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text("Nombre completo") },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                )
                                OutlinedTextField(
                                    value = bio,
                                    onValueChange = { bio = it },
                                    label = { Text("Biografía") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(text = state.user.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(bio.ifBlank { "Sin descripción." }, style = MaterialTheme.typography.bodyMedium)
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
                                isLiked = false, // TODO: Cargar estado real de likes
                                showHeader = false
                            )
                        }
                        1 -> items(state.favoriteCoffees) { coffeeDetails ->
                            CoffeeFavoriteItem(
                                coffeeDetails = coffeeDetails,
                                isFavorite = true,
                                onFavoriteClick = { viewModel.onToggleFavorite(coffeeDetails.coffee.id, true) },
                                onClick = { onCoffeeClick(coffeeDetails.coffee.id) }
                            )
                        }
                        2 -> items(state.userReviews) { info ->
                            UserReviewCard(info = info, showHeader = false, onClick = { onCoffeeClick(info.coffeeDetails.coffee.id) })
                        }
                    }
                }

                // Mostrar la hoja de comentarios si hay un ID seleccionado
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

@Composable
fun CoffeeFavoriteItem(coffeeDetails: CoffeeWithDetails, isFavorite: Boolean, onFavoriteClick: () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = coffeeDetails.coffee.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = coffeeDetails.coffee.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "${coffeeDetails.coffee.marca}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            IconButton(onClick = onFavoriteClick) { Icon(Icons.Default.Favorite, "Favorito", tint = Color.Red) }
        }
    }
}

@Composable
private fun UserInfoStat(label: String, value: String, onClick: (() -> Unit)? = null) {
    val modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 12.sp)
    }
}
