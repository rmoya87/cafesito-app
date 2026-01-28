package com.cafesito.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafesito.app.ui.components.GlassyTopBar
import com.cafesito.app.ui.theme.EspressoDeep
import com.cafesito.app.ui.theme.SoftOffWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingScreen(
    userId: Int,
    onBackClick: () -> Unit,
    onUserClick: (Int) -> Unit,
    viewModel: FollowViewModel = hiltViewModel()
) {
    val uiState by viewModel.followingState(userId).collectAsState(initial = emptyList())
    val myFollowingIds by viewModel.myFollowingIds.collectAsState(initial = emptySet())
    val activeUser by viewModel.activeUser.collectAsState(initial = null)
    
    var currentQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

    val filteredFollowing = remember(uiState, currentQuery) {
        uiState.filter {
            it.user.username.contains(currentQuery, ignoreCase = true) || 
            it.user.fullName.contains(currentQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = SoftOffWhite,
        topBar = {
            GlassyTopBar(
                title = if (searchActive) "" else "Seguidos",
                onBackClick = {
                    if (searchActive) {
                        searchActive = false
                        currentQuery = ""
                    } else {
                        onBackClick()
                    }
                },
                actions = {
                    if (searchActive) {
                        TextField(
                            value = currentQuery,
                            onValueChange = { currentQuery = it },
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
                        IconButton(onClick = { currentQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = EspressoDeep)
                        }
                    } else {
                        IconButton(onClick = { searchActive = true }) {
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
            if (filteredFollowing.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentQuery.isEmpty()) "No sigue a nadie todavía" else "No se encontraron resultados",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(filteredFollowing, key = { it.user.id }) { info ->
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
