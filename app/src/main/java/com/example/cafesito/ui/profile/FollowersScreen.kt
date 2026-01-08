package com.example.cafesito.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.domain.User
import com.example.cafesito.domain.allUsers
import com.example.cafesito.domain.currentUser
import com.example.cafesito.ui.theme.CoffeeBrown

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
    
    var searchQuery by remember { mutableStateOf("") }

    val filteredFollowers = uiState.filter {
        it.user.username.contains(searchQuery, ignoreCase = true) || 
        it.user.fullName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguidores") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Buscar seguidores...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CoffeeBrown,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (filteredFollowers.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text(if (searchQuery.isEmpty()) "Todavía no tiene seguidores." else "No se encontraron seguidores.")
                        }
                    }
                } else {
                    items(filteredFollowers) { info ->
                        FollowItem(
                            user = info.user,
                            followersCount = info.followersCount,
                            followingCount = info.followingCount,
                            isFollowing = myFollowingIds.contains(info.user.id),
                            onFollowClick = { viewModel.toggleFollow(info.user.id) },
                            onClick = { onUserClick(info.user.id) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FollowItem(
    user: User,
    followersCount: Int,
    followingCount: Int,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
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
            model = user.avatarUrl,
            contentDescription = null,
            modifier = Modifier.size(50.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$followersCount Seguidores • $followingCount Seguidos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        if (user.id != currentUser.id) {
            if (isFollowing) {
                Button(
                    onClick = onFollowClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoffeeBrown,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Siguiendo", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                OutlinedButton(
                    onClick = onFollowClick,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CoffeeBrown),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = CoffeeBrown
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Seguir", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
