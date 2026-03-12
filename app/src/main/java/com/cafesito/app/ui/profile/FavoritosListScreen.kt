package com.cafesito.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafesito.app.ui.components.GlassyTopBar
import com.cafesito.app.ui.components.SwipeableFavoriteItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritosListScreen(
    onBackClick: () -> Unit,
    onCoffeeClick: (String) -> Unit,
    viewModel: ProfileViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            GlassyTopBar(
                title = "FAVORITOS",
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (val state = uiState) {
            is ProfileUiState.Success -> {
                val sorted = state.favoriteCoffees.sortedBy { it.coffee.nombre }
                if (sorted.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay cafés favoritos",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(sorted, key = { it.coffee.id }) { coffee ->
                            Box(Modifier.padding(vertical = 4.dp)) {
                                SwipeableFavoriteItem(
                                    coffeeDetails = coffee,
                                    onRemoveFromFavorites = { viewModel.onToggleFavorite(coffee.coffee.id, false) },
                                    onClick = { onCoffeeClick(coffee.coffee.id) }
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
