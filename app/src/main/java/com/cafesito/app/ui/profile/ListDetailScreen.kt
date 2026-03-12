package com.cafesito.app.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafesito.app.ui.components.CreateListBottomSheet
import com.cafesito.app.ui.components.GlassyTopBar
import com.cafesito.app.ui.components.ListDeleteConfirmBottomSheet
import com.cafesito.app.ui.components.ListOptionsBottomSheet
import com.cafesito.app.ui.components.SwipeableFavoriteItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    onBackClick: () -> Unit,
    onCoffeeClick: (String) -> Unit,
    onListDeleted: () -> Unit,
    viewModel: ListDetailViewModel
) {
    val listCoffees by viewModel.listCoffees.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listName by viewModel.listName.collectAsState()
    val listIsPublic by viewModel.listIsPublic.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showOptionsMenu) {
        ListOptionsBottomSheet(
            onDismiss = { showOptionsMenu = false },
            onEditList = { showEditSheet = true },
            onDeleteList = { showDeleteConfirm = true }
        )
    }
    if (showEditSheet) {
        CreateListBottomSheet(
            onDismiss = { showEditSheet = false },
            onCreate = { _, _ -> },
            listIdForEdit = viewModel.listId,
            initialName = listName,
            initialIsPublic = listIsPublic,
            onUpdate = { name, isPublic ->
                viewModel.updateList(name, isPublic)
                showEditSheet = false
            }
        )
    }
    if (showDeleteConfirm) {
        ListDeleteConfirmBottomSheet(
            listName = listName,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                viewModel.deleteList()
                showDeleteConfirm = false
                onListDeleted()
            }
        )
    }

    Scaffold(
        topBar = {
            GlassyTopBar(
                title = listName.take(32).ifEmpty { "Lista" },
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { showOptionsMenu = true }) {
                        androidx.compose.material3.Icon(Icons.Default.MoreHoriz, contentDescription = "Opciones")
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
                            onClick = { onCoffeeClick(coffee.coffee.id) }
                        )
                    }
                }
            }
        }
    }
}
