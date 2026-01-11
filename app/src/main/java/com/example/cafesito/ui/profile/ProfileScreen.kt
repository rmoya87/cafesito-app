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
import com.example.cafesito.domain.Post
import com.example.cafesito.domain.User
import com.example.cafesito.domain.currentUser
import com.example.cafesito.ui.components.PostCard
import com.example.cafesito.ui.components.UserReviewCard // Importamos el componente compartido
import com.example.cafesito.ui.theme.CoffeeBrown
import java.util.Locale

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

    when (val state = uiState) {
        is ProfileUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is ProfileUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message) }
        is ProfileUiState.Success -> {
            var bio by remember { mutableStateOf(state.user.bio ?: "") }
            var email by remember { mutableStateOf(state.user.email) }
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                viewModel.onAvatarChange(uri)
            }

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
                                    if (state.isEditing) viewModel.onSaveProfile(state.user.avatarUrl, bio, email)
                                    else viewModel.toggleEditMode()
                                }) {
                                    Text(if (state.isEditing) "Guardar" else "Editar Perfil")
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.toggleFollow() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (state.isFollowing) Color.LightGray else CoffeeBrown
                                    )
                                ) {
                                    Text(if (state.isFollowing) "Siguiendo" else "Seguir", color = Color.White)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp)) {
                            Box {
                                AsyncImage(
                                    model = state.user.avatarUrl,
                                    contentDescription = "Foto de perfil",
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

                        Text(text = state.user.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (state.isEditing) {
                            OutlinedTextField(
                                value = bio,
                                onValueChange = { bio = it },
                                label = { Text("Biografía") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
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
                    TabRow(selectedTabIndex = selectedTab, containerColor = Color.White, contentColor = CoffeeBrown) {
                        tabs.forEachIndexed { index, title ->
                            Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                        }
                    }
                }

                when (selectedTab) {
                    0 -> items(state.posts) { post ->
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            PostCard(
                                post = post,
                                onUserClick = { onUserClick(post.user.id) },
                                onCommentClick = { /* Handled in VM */ }, 
                                modifier = Modifier.background(Color.White),
                                showHeader = false
                            )
                        }
                    }
                    1 -> items(state.favoriteCoffees) { coffeeDetails ->
                        val isMyFavorite = state.myFavoriteIds.contains(coffeeDetails.coffee.id)
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            CoffeeFavoriteItem(
                                coffeeDetails = coffeeDetails,
                                isFavorite = isMyFavorite,
                                onFavoriteClick = { viewModel.onToggleFavorite(coffeeDetails.coffee.id, isMyFavorite) },
                                onClick = { onCoffeeClick(coffeeDetails.coffee.id) }
                            )
                        }
                    }
                    2 -> items(state.userReviews) { info ->
                        // USO CORRECTO DEL COMPONENTE SIN CABECERA PARA EL PERFIL
                        UserReviewCard(
                            info = info, 
                            showHeader = false, // ELIMINA LA FRANJA DE "OPINIÓN PUBLICADA"
                            onClick = { onCoffeeClick(info.coffeeDetails.coffee.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CoffeeFavoriteItem(coffeeDetails: CoffeeWithDetails, isFavorite: Boolean, onFavoriteClick: () -> Unit, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = coffeeDetails.coffee.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = coffeeDetails.coffee.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = "${coffeeDetails.coffee.marca} • ${coffeeDetails.coffee.paisOrigen}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onFavoriteClick) {
            Icon(imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorito", tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant)
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
