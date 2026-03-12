package com.cafesito.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cafesito.app.ui.profile.FollowItemModern
import com.cafesito.shared.domain.SuggestedUserInfo

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineEmptyState(
    suggestedUsers: List<SuggestedUserInfo>,
    topics: List<String>,
    onFollowClick: (Int) -> Unit,
    onUserClick: (Int) -> Unit,
    onAddPostClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp, top = 16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tu timeline está vacío",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Empieza siguiendo personas o publicando tu primer café.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAddPostClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = "Publica tu primer café", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (suggestedUsers.isNotEmpty()) {
            item {
                Text(
                    text = "Personas recomendadas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }
            items(suggestedUsers.take(6), key = { it.user.id }) { info ->
                FollowItemModern(
                    user = info.user,
                    isFollowing = false,
                    isMe = false,
                    onFollowClick = { onFollowClick(info.user.id) },
                    onClick = { onUserClick(info.user.id) },
                    followersCount = info.followersCount,
                    followingCount = info.followingCount
                )
            }
        }

        if (topics.isNotEmpty()) {
            item {
                Text(
                    text = "Temas para explorar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topics.take(8).forEach { topic ->
                            TagChip(label = "Tema", value = topic)
                        }
                    }
                }
            }
        }
    }
}
