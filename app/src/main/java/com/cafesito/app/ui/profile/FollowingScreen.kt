package com.cafesito.app.ui.profile

import androidx.compose.animation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.cafesito.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingScreen(
    userId: Int,
    onBackClick: () -> Unit,
    onUserClick: (Int) -> Unit,
    viewModel: FollowViewModel = hiltViewModel()
) {
    val uiState by viewModel.followingState(userId).collectAsState()
    val myFollowingIds by viewModel.myFollowingIds.collectAsState(initial = emptySet())
    val activeUser by viewModel.activeUser.collectAsState(initial = null)
    
    var currentQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val filteredFollowing = remember(uiState, currentQuery) {
        uiState.filter {
            it.user.username.contains(currentQuery, ignoreCase = true) || 
            it.user.fullName.contains(currentQuery, ignoreCase = true)
        }
    }

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
                        value = currentQuery,
                        onValueChange = { currentQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        interactionSource = interactionSource,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { innerTextField ->
                            OutlinedTextFieldDefaults.DecorationBox(
                                value = currentQuery,
                                innerTextField = innerTextField,
                                enabled = true,
                                singleLine = true,
                                visualTransformation = VisualTransformation.None,
                                interactionSource = interactionSource,
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                placeholder = { Text("Buscar en seguidos...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                                        shape = Shapes.pillFull,
                                    )
                                }
                            )
                        }
                    )

                    AnimatedVisibility(
                        visible = currentQuery.isNotBlank() || isFocused,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        TextButton(
                            onClick = {
                                currentQuery = ""
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
                if (filteredFollowing.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (currentQuery.isEmpty()) "No sigue a nadie todavía" else "No se encontraron resultados",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
