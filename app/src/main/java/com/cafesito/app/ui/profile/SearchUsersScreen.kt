package com.cafesito.app.ui.profile

import androidx.compose.animation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafesito.shared.domain.SuggestedUserInfo
import com.cafesito.shared.domain.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUsersScreen(
    onBackClick: () -> Unit,
    onUserClick: (Int) -> Unit,
    viewModel: FollowViewModel = hiltViewModel()
) {
    val allUsersWithCounts by viewModel.allUsersWithCounts.collectAsState(initial = emptyList())
    val suggestedUsers by viewModel.suggestedUsers.collectAsState(initial = emptyList())
    val myFollowingIds by viewModel.myFollowingIds.collectAsState(initial = emptySet())
    val activeUser by viewModel.activeUser.collectAsState(initial = null)
    
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val filteredUsers = if (searchQuery.isBlank()) {
        suggestedUsers
    } else {
        allUsersWithCounts.filter {
            it.user.username.contains(searchQuery, ignoreCase = true) ||
                it.user.fullName.contains(searchQuery, ignoreCase = true)
        }
    }.filter { it.user.id != activeUser?.id }

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { viewModel.loadInitialIfNeeded() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                    
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        interactionSource = interactionSource,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { innerTextField ->
                            OutlinedTextFieldDefaults.DecorationBox(
                                value = searchQuery,
                                innerTextField = innerTextField,
                                enabled = true,
                                singleLine = true,
                                visualTransformation = VisualTransformation.None,
                                interactionSource = interactionSource,
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar usuarios", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                placeholder = { Text("Buscar usuarios...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                contentPadding = PaddingValues(0.dp),
                                container = {
                                    OutlinedTextFieldDefaults.Container(
                                        enabled = true,
                                        isError = false,
                                        interactionSource = interactionSource,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(999.dp),
                                    )
                                }
                            )
                        }
                    )

                    AnimatedVisibility(
                        visible = searchQuery.isNotBlank() || isFocused,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                focusManager.clearFocus()
                            },
                            contentPadding = PaddingValues(start = 12.dp, end = 4.dp)
                        ) {
                            Text("Cancelar", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.loadInitialIfNeeded()
                scope.launch {
                    delay(400)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (filteredUsers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isEmpty()) "Busca amigos para seguir" else "No se encontraron usuarios",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredUsers, key = { it.user.id }) { info ->
                        FollowItemModern(
                            user = info.user,
                            isFollowing = myFollowingIds.contains(info.user.id),
                            isMe = false,
                            onFollowClick = { viewModel.toggleFollow(info.user.id) },
                            onClick = { onUserClick(info.user.id) },
                            followersCount = info.followersCount,
                            followingCount = info.followingCount
                        )
                    }
                }
            }
        }
    }
}
