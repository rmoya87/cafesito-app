package com.cafesito.app.ui.profile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cafesito.app.data.ListMemberRow
import com.cafesito.app.data.UserEntity
import com.cafesito.app.ui.components.CreateListBottomSheet
import com.cafesito.app.ui.components.GlassyTopBar
import com.cafesito.app.ui.components.ListDeleteConfirmBottomSheet
import android.os.Bundle
import androidx.core.os.bundleOf
import com.cafesito.app.ui.theme.Spacing
import com.cafesito.app.ui.utils.containsSearchQuery

private val PRIVACY_OPTIONS = listOf(
    "public" to ("Pública" to "Cualquier persona puede suscribirse."),
    "invitation" to ("Por invitación" to "Solo quienes invites podrán ver la lista."),
    "private" to ("Privada" to "Solo tú.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListOptionsScreen(
    onBackClick: () -> Unit,
    onListDeleted: () -> Unit,
    onLeftList: () -> Unit,
    viewModel: ListOptionsViewModel,
    onTrackEvent: (String, Bundle) -> Unit = { _, _ -> }
) {
    val list by viewModel.list.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listPrivacy by viewModel.listPrivacy.collectAsState()
    val listMembersCanEdit by viewModel.listMembersCanEdit.collectAsState()
    val listMembersCanInvite by viewModel.listMembersCanInvite.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()
    val members by viewModel.members.collectAsState()
    val memberUsers by viewModel.memberUsers.collectAsState()
    val invitations by viewModel.invitations.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val invitingId by viewModel.invitingId.collectAsState()
    val mutualFollowIds by viewModel.mutualFollowIds.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    if (showEditSheet) {
        CreateListBottomSheet(
            onDismiss = { onTrackEvent("modal_close", bundleOf("modal_id" to "list_edit")); showEditSheet = false },
            onCreate = { _, _, _ -> },
            listIdForEdit = viewModel.listId,
            initialName = list?.name ?: "",
            initialIsPublic = listPrivacy == "public",
            initialPrivacy = listPrivacy,
            initialMembersCanEdit = listMembersCanEdit,
            onUpdate = { name, privacy, membersCanEdit ->
                viewModel.updateList(name, privacy, membersCanEdit, listMembersCanInvite)
                onTrackEvent("modal_close", bundleOf("modal_id" to "list_edit"))
                showEditSheet = false
            }
        )
    }
    if (showDeleteConfirm) {
        ListDeleteConfirmBottomSheet(
            listName = list?.name ?: "Lista",
            onDismiss = { onTrackEvent("modal_close", bundleOf("modal_id" to "delete_confirm_list")); showDeleteConfirm = false },
            onConfirm = {
                viewModel.deleteList()
                onTrackEvent("modal_close", bundleOf("modal_id" to "delete_confirm_list"))
                showDeleteConfirm = false
                onListDeleted()
            }
        )
    }
    if (showLeaveConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { onTrackEvent("modal_close", bundleOf("modal_id" to "leave_list_confirm")); showLeaveConfirm = false },
            title = { Text("Abandonar lista") },
            text = { Text("¿Salir de esta lista? Dejarás de verla en tus listas.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.leaveList()
                    onTrackEvent("modal_close", bundleOf("modal_id" to "leave_list_confirm"))
                    showLeaveConfirm = false
                    onLeftList()
                }) { Text("Abandonar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { onTrackEvent("modal_close", bundleOf("modal_id" to "leave_list_confirm")); showLeaveConfirm = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        topBar = {
            GlassyTopBar(
                title = "Opciones",
                onBackClick = onBackClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isOwner && list != null) {
                    item {
                        Text(
                            "Privacidad",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                PRIVACY_OPTIONS.forEach { option ->
                                    val privacyValue = option.first
                                    val label = option.second.first
                                    val desc = option.second.second
                                    val selected = listPrivacy == privacyValue
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.RadioButton(
                                            selected = selected,
                                            onClick = { viewModel.updatePrivacy(privacyValue, listMembersCanEdit, listMembersCanInvite) }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(label, style = MaterialTheme.typography.bodyLarge)
                                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                if (listPrivacy == "invitation") {
                                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Permitir que los miembros inviten", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        Switch(
                                            checked = listMembersCanInvite,
                                            onCheckedChange = { viewModel.updatePrivacy(listPrivacy, listMembersCanEdit, it) },
                                            colors = SwitchDefaults.colors(
                                                uncheckedTrackColor = if (isSystemInDarkTheme()) Color(0xFFB0B0B0) else Color(0xFF757575),
                                                uncheckedThumbColor = MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    }
                                }
                                if (listPrivacy == "public" || listPrivacy == "invitation") {
                                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Permitir editar lista", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        Switch(
                                            checked = listMembersCanEdit,
                                            onCheckedChange = { viewModel.updatePrivacy(listPrivacy, it, listMembersCanInvite) },
                                            colors = SwitchDefaults.colors(
                                                uncheckedTrackColor = if (isSystemInDarkTheme()) Color(0xFFB0B0B0) else Color(0xFF757575),
                                                uncheckedThumbColor = MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            "Miembros",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Buscar usuarios...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(percent = 50)
                                )
                                val ownerId = list?.userId ?: 0L
                                val memberIdsSet = members.map { it.userId.toInt() }.toSet()
                                val pendingIds = invitations.filter { it.status == "pending" }.map { it.inviteeId.toInt() }.toSet()
                                val searchResultUsers = allUsers
                                    .filter { it.id != (list?.userId?.toInt() ?: 0) }
                                    .filter { u ->
                                        searchQuery.isBlank() ||
                                            u.username.containsSearchQuery(searchQuery) ||
                                            u.fullName.containsSearchQuery(searchQuery)
                                    }
                                if (searchQuery.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    searchResultUsers.take(5).forEach { user ->
                                        val alreadyMember = user.id in memberIdsSet
                                        val pendingInvite = user.id in pendingIds
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
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
                                                    Text(
                                                        user.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text("@${user.username}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                            when {
                                                alreadyMember -> Text(
                                                    "Ya está en la lista",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                pendingInvite -> Text(
                                                    "Invitación enviada",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                else -> TextButton(
                                                    onClick = { viewModel.inviteUser(user.id) },
                                                    enabled = invitingId != user.id
                                                ) { Text("Invitar") }
                                            }
                                        }
                                    }
                                    if (searchResultUsers.isEmpty()) {
                                        Text(
                                            "No hay usuarios que coincidan.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        tint = if (isSystemInDarkTheme()) Color.White else Color.Black
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    TextButton(
                                        onClick = { viewModel.shareList() },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = if (isSystemInDarkTheme()) Color.White else Color.Black
                                        )
                                    ) {
                                        Text("Compartir lista")
                                    }
                                }
                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                val displayOrder = listOf(ownerId) + members.map { it.userId }.distinct().filter { it != ownerId }
                                displayOrder.forEach { uid ->
                                    val user = memberUsers.find { it.id == uid.toInt() }
                                    val member = members.find { it.userId == uid }
                                    val isOwnerRow = uid == ownerId
                                    val displayName = user?.fullName?.takeIf { it.isNotBlank() } ?: user?.username?.let { "@$it" } ?: uid.toString()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!user?.avatarUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = user?.avatarUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text(
                                                    displayName.replace(Regex("^@"), "").trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                displayName,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                if (isOwnerRow) "Admin." else "Miembro",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (!isOwnerRow) {
                                            val isDark = isSystemInDarkTheme()
                                            Button(
                                                onClick = { viewModel.removeMember(uid.toInt()) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFC62828),
                                                    contentColor = if (isDark) Color.Black else Color.White
                                                )
                                            ) {
                                                Text("Eliminar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val listForMember = list
                if (!isOwner && listForMember != null && listPrivacy == "public") {
                    item {
                        Text(
                            "Miembros",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Buscar usuarios...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(percent = 50)
                                )
                                val ownerId = listForMember.userId
                                val memberIdsSet = members.map { it.userId.toInt() }.toSet()
                                val pendingIds = invitations.filter { it.status == "pending" }.map { it.inviteeId.toInt() }.toSet()
                                val searchResultUsers = allUsers
                                    .filter { it.id != ownerId.toInt() }
                                    .filter { u ->
                                        searchQuery.isBlank() ||
                                            u.username.containsSearchQuery(searchQuery) ||
                                            u.fullName.containsSearchQuery(searchQuery)
                                    }
                                if (searchQuery.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    searchResultUsers.take(5).forEach { user ->
                                        val alreadyMember = user.id in memberIdsSet
                                        val pendingInvite = user.id in pendingIds
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
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
                                                    Text(
                                                        user.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text("@${user.username}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                            when {
                                                alreadyMember -> Text(
                                                    "Ya está en la lista",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                pendingInvite -> Text(
                                                    "Invitación enviada",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                else -> TextButton(
                                                    onClick = { viewModel.inviteUser(user.id) },
                                                    enabled = invitingId != user.id
                                                ) { Text("Invitar") }
                                            }
                                        }
                                    }
                                    if (searchResultUsers.isEmpty()) {
                                        Text(
                                            "No hay usuarios que coincidan.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        tint = if (isSystemInDarkTheme()) Color.White else Color.Black
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    TextButton(
                                        onClick = { viewModel.shareList() },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = if (isSystemInDarkTheme()) Color.White else Color.Black
                                        )
                                    ) {
                                        Text("Compartir lista")
                                    }
                                }
                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                val displayOrderPublic = (listOf(ownerId) + members.map { it.userId }.distinct().filter { it != ownerId })
                                    .filter { mutualFollowIds.contains(it.toInt()) || it.toInt() == currentUserId }
                                displayOrderPublic.forEach { uid ->
                                    val user = memberUsers.find { it.id == uid.toInt() }
                                    val isOwnerRow = uid == ownerId
                                    val displayName = user?.fullName?.takeIf { it.isNotBlank() } ?: user?.username?.let { "@$it" } ?: uid.toString()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!user?.avatarUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = user?.avatarUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text(
                                                    displayName.replace(Regex("^@"), "").trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(displayName, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                if (isOwnerRow) "Admin." else "Miembro",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val listForInvitation = list
                if (!isOwner && listForInvitation != null && listPrivacy == "invitation" && listForInvitation.membersCanInvite == true) {
                    item {
                        Text(
                            "Miembros",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Buscar usuarios...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(percent = 50)
                                )
                                val ownerIdInv = listForInvitation.userId
                                val memberIdsSetInv = members.map { it.userId.toInt() }.toSet()
                                val pendingIdsInv = invitations.filter { it.status == "pending" }.map { it.inviteeId.toInt() }.toSet()
                                val searchResultUsersInv = allUsers
                                    .filter { it.id != ownerIdInv.toInt() }
                                    .filter { u ->
                                        searchQuery.isBlank() ||
                                            u.username.containsSearchQuery(searchQuery) ||
                                            u.fullName.containsSearchQuery(searchQuery)
                                    }
                                if (searchQuery.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    searchResultUsersInv.take(5).forEach { user ->
                                        val alreadyMember = user.id in memberIdsSetInv
                                        val pendingInvite = user.id in pendingIdsInv
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
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
                                                    Text(
                                                        user.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text("@${user.username}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                            when {
                                                alreadyMember -> Text(
                                                    "Ya está en la lista",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                pendingInvite -> Text(
                                                    "Invitación enviada",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                else -> TextButton(
                                                    onClick = { viewModel.inviteUser(user.id) },
                                                    enabled = invitingId != user.id
                                                ) { Text("Invitar") }
                                            }
                                        }
                                    }
                                    if (searchResultUsersInv.isEmpty()) {
                                        Text(
                                            "No hay usuarios que coincidan.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        tint = if (isSystemInDarkTheme()) Color.White else Color.Black
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    TextButton(
                                        onClick = { viewModel.shareList() },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = if (isSystemInDarkTheme()) Color.White else Color.Black
                                        )
                                    ) {
                                        Text("Compartir lista")
                                    }
                                }
                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                val displayOrderInv = listOf(ownerIdInv) + members.map { it.userId }.distinct().filter { it != ownerIdInv }

                                displayOrderInv.forEach { uid ->
                                    val userInv = memberUsers.find { it.id == uid.toInt() }
                                    val isOwnerRow = uid == ownerIdInv
                                    val displayNameInv = userInv?.fullName?.takeIf { it.isNotBlank() } ?: userInv?.username?.let { "@$it" } ?: uid.toString()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!userInv?.avatarUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = userInv?.avatarUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text(
                                                    displayNameInv.replace(Regex("^@"), "").trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(displayNameInv, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                if (isOwnerRow) "Admin." else "Miembro",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "General",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column {
                            if (isOwner) {
                                OptionRowInCard(icon = Icons.Default.Edit, label = "Editar lista", onClick = { onTrackEvent("modal_open", bundleOf("modal_id" to "list_edit")); showEditSheet = true })
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                                OptionRowInCard(icon = Icons.Default.Delete, label = "Eliminar lista", onClick = { onTrackEvent("modal_open", bundleOf("modal_id" to "delete_confirm_list")); showDeleteConfirm = true })
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.space4, 14.dp)
                                        .clickable { onTrackEvent("modal_open", bundleOf("modal_id" to "leave_list_confirm")); showLeaveConfirm = true },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.width(Spacing.space3))
                                    Text("Salir de la lista", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRowInCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.space4, 14.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(Spacing.space3))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
