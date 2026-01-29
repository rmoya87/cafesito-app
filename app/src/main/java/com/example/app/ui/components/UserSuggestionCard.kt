package com.cafesito.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cafesito.shared.domain.SuggestedUserInfo
import com.cafesito.app.ui.theme.CoffeeBrown
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UserSuggestionCarousel(
    users: List<SuggestedUserInfo>,
    followingIds: Set<Int>,
    onUserClick: (Int) -> Unit,
    onFollowClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val recentlyFollowedIds = remember { mutableStateListOf<Int>() }
    val scope = rememberCoroutineScope()
    val cachedUsers = remember { mutableStateMapOf<Int, SuggestedUserInfo>() }
    
    LaunchedEffect(users) {
        users.forEach { cachedUsers[it.user.id] = it }
    }

    val displayUsers = (users + cachedUsers.values.filter { recentlyFollowedIds.contains(it.user.id) })
        .distinctBy { it.user.id }
        .filter { recentlyFollowedIds.contains(it.user.id) || users.any { u -> u.user.id == it.user.id } }

    if (displayUsers.isNotEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "Personas que podrías seguir",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayUsers, key = { it.user.id }) { info ->
                    val isHidden = remember { mutableStateOf(false) }
                    val isActuallyFollowing = followingIds.contains(info.user.id) || recentlyFollowedIds.contains(info.user.id)
                    
                    AnimatedVisibility(
                        visible = !isHidden.value,
                        exit = fadeOut(tween(500)) + shrinkHorizontally()
                    ) {
                        UserSuggestionCard(
                            info = info,
                            isFollowing = isActuallyFollowing,
                            onUserClick = onUserClick,
                            onFollowClick = { targetId ->
                                if (!isActuallyFollowing) {
                                    onFollowClick(targetId)
                                    recentlyFollowedIds.add(targetId)
                                    scope.launch {
                                        delay(2000) 
                                        isHidden.value = true
                                        delay(500)
                                        recentlyFollowedIds.removeAll { it == targetId }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserSuggestionCard(
    info: SuggestedUserInfo,
    isFollowing: Boolean,
    onUserClick: (Int) -> Unit,
    onFollowClick: (Int) -> Unit
) {
    val user = info.user
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0).copy(alpha = 0.4f)),
        onClick = { onUserClick(user.id) }
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(70.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${info.followersCount} Seguidores",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            if (isFollowing) {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoffeeBrown,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Siguiendo", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = { onFollowClick(user.id) },
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    border = BorderStroke(1.dp, CoffeeBrown),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = CoffeeBrown
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Seguir", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
