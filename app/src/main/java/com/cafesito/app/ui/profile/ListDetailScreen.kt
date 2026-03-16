package com.cafesito.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cafesito.app.data.UserEntity
import com.cafesito.app.ui.theme.Spacing
import com.cafesito.app.ui.components.GlassyTopBar
import com.cafesito.app.ui.components.SwipeableFavoriteItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    onBackClick: () -> Unit,
    onCoffeeClick: (String) -> Unit,
    onListDeleted: () -> Unit,
    onLeftList: () -> Unit,
    onOpenListOptions: () -> Unit,
    viewModel: ListDetailViewModel
) {
    val listCoffees by viewModel.listCoffees.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listName by viewModel.listName.collectAsState()
    val listPrivacy by viewModel.listPrivacy.collectAsState()
    val isOwnList by viewModel.isOwnList.collectAsState()
    val isMember by viewModel.isMember.collectAsState()
    val listMemberCount by viewModel.listMemberCount.collectAsState()
    val listMemberPreviews by viewModel.listMemberPreviews.collectAsState()
    val canEditList by viewModel.canEditList.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val isPublicOrInvitation = listPrivacy == "public" || listPrivacy == "invitation"
    val showMemberAvatars = isPublicOrInvitation && listMemberCount > 0
    val showJoinButton = !isOwnList && isPublicOrInvitation && !isMember

    Scaffold(
        topBar = {
            GlassyTopBar(
                title = listName.take(32).ifEmpty { "Lista" },
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (showMemberAvatars) {
                        IconButton(onClick = onOpenListOptions) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = listMemberCount.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(8.dp))
                                ListMemberAvatarsStack(
                                    members = listMemberPreviews.take(3),
                                    avatarSize = 28.dp,
                                    overlap = 10.dp
                                )
                            }
                        }
                    }
                    if (!showMemberAvatars) {
                        IconButton(onClick = onOpenListOptions) {
                            androidx.compose.material3.Icon(Icons.Default.MoreHoriz, contentDescription = "Opciones de lista")
                        }
                    }
                    if (showJoinButton) {
                        Button(
                            onClick = { viewModel.joinPublicList() },
                            modifier = Modifier.padding(end = Spacing.space2)
                        ) {
                            Text("Unirse")
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (listCoffees.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    "No hay cafés en esta lista",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(listCoffees, key = { it.coffee.id }) { coffee ->
                    Box(Modifier.padding(vertical = 4.dp)) {
                        SwipeableFavoriteItem(
                            coffeeDetails = coffee,
                            onRemoveFromFavorites = { viewModel.removeFromList(coffee.coffee.id) },
                            onClick = { onCoffeeClick(coffee.coffee.id) },
                            enableSwipeToRemove = canEditList
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListMemberAvatarsStack(
    members: List<UserEntity>,
    avatarSize: Dp,
    overlap: Dp
) {
    val density = LocalDensity.current
    val overlapPx = with(density) { overlap.roundToPx() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        members.forEachIndexed { index, user ->
            Box(
                modifier = Modifier
                    .then(
                        if (index == 0) Modifier
                        else Modifier.offset { IntOffset(-overlapPx * index, 0) }
                    )
                    .size(avatarSize)
                    .aspectRatio(1f)
                    .zIndex(index.toFloat())
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!user.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val initial = user.fullName.takeIf { it.isNotBlank() }?.firstOrNull()?.uppercaseChar()
                        ?: user.username.firstOrNull()?.uppercaseChar()
                        ?: '?'
                    Text(
                        text = initial.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
