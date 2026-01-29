package com.cafesito.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafesito.shared.domain.User
import com.cafesito.app.ui.components.GlassyTopBar
import com.cafesito.app.ui.components.ModernAvatar
import com.cafesito.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersScreen(
    userId: Int,
    onBackClick: () -> Unit,
    onUserClick: (Int) -> Unit,
    viewModel: FollowViewModel = hiltViewModel()
) {
    val uiState by viewModel.followersState(userId).collectAsState(initial = emptyList())
    val myFollowingIds by viewModel.myFollowingIds.collectAsState(initial = emptySet())
    val activeUser by viewModel.activeUser.collectAsState(initial = null)
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchMode by remember { mutableStateOf(false) }

    val filteredFollowers = uiState.filter {
        it.user.username.contains(searchQuery, ignoreCase = true) || 
        it.user.fullName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = SoftOffWhite,
        topBar = {
            GlassyTopBar(
                title = if (isSearchMode) "" else "Seguidores",
                onBackClick = {
                    if (isSearchMode) {
                        isSearchMode = false
                        searchQuery = ""
                    } else {
                        onBackClick()
                    }
                },
                actions = {
                    if (isSearchMode) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = EspressoDeep)
                        }
                    } else {
                        IconButton(onClick = { isSearchMode = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar", tint = EspressoDeep)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (filteredFollowers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isEmpty()) "Todavía no hay seguidores" else "No se encontraron resultados",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(filteredFollowers, key = { it.user.id }) { info ->
                    FollowItemModern(
                        user = info.user,
                        isFollowing = myFollowingIds.contains(info.user.id),
                        isMe = activeUser?.id == info.user.id,
                        onFollowClick = { viewModel.toggleFollow(info.user.id) },
                        onClick = { onUserClick(info.user.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun FollowItemModern(
    user: User,
    isFollowing: Boolean,
    isMe: Boolean,
    onFollowClick: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModernAvatar(imageUrl = user.avatarUrl, size = 48.dp)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EspressoDeep
                )
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            if (!isMe) {
                Button(
                    onClick = onFollowClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) Color(0xFFF5F5F5) else EspressoDeep,
                        contentColor = if (isFollowing) EspressoDeep else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = if (isFollowing) "Siguiendo" else "Seguir",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
