package com.example.cafesito.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cafesito.domain.SuggestedUserInfo
import com.example.cafesito.domain.User
import com.example.cafesito.ui.theme.CoffeeBrown

@Composable
fun UserSuggestionCarousel(
    users: List<SuggestedUserInfo>,
    followingIds: Set<Int>,
    onUserClick: (Int) -> Unit,
    onFollowClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
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
            items(users, key = { it.user.id }) { info ->
                UserSuggestionCard(
                    info = info,
                    isFollowing = followingIds.contains(info.user.id),
                    onUserClick = onUserClick,
                    onFollowClick = onFollowClick
                )
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
        elevation = CardDefaults.cardElevation(0.dp),
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
                    text = "${info.followersCount} Seguidores\n${info.followingCount} Seguidos",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }

            if (isFollowing) {
                Button(
                    onClick = { onFollowClick(user.id) },
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoffeeBrown,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Siguiendo", style = MaterialTheme.typography.labelMedium)
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
                    Text("Seguir", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
