package com.cafesito.app.ui.profile

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafesito.app.data.ProfileActivityItem
import com.cafesito.app.data.UserReviewInfo
import com.cafesito.app.security.runWithBiometricReauth
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onUserClick: (Int) -> Unit,
    onCoffeeClick: (String) -> Unit,
    onFollowersClick: (Int) -> Unit,
    onFollowingClick: (Int) -> Unit,
    onHistorialClick: () -> Unit,
    onFavoritosListClick: (() -> Unit)? = null,
    onOpenListClick: ((String, String) -> Unit)? = null,
    onOpenUserListClick: ((Int, String) -> Unit)? = null,
    onSearchUsersClick: (() -> Unit)? = null,
    onExploreCafes: (() -> Unit)? = null,
    profileBackStackEntry: NavBackStackEntry? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val uiState by viewModel.uiState.collectAsState()
    val profileActivityItems by viewModel.profileActivityItems.collectAsState()
    val profileActivityLoading by viewModel.profileActivityLoading.collectAsState()
    val tabs = listOf("ACTIVIDAD", "ADN", "LISTAS")
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        val tab = profileBackStackEntry?.savedStateHandle?.get<Int>("profile_return_tab")
        if (tab != null) {
            selectedTabIndex = tab
            profileBackStackEntry.savedStateHandle.remove<Int>("profile_return_tab")
        }
    }
    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteAccountConfirm by rememberSaveable { mutableStateOf(false) }
    var deletingAccount by rememberSaveable { mutableStateOf(false) }
    var showSensoryDetail by remember { mutableStateOf(false) }
    var showCreateListSheet by rememberSaveable { mutableStateOf(false) }

    var itemToDelete by remember { mutableStateOf<Any?>(null) }

    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.refreshData() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GlassyTopBar(
                title = "PERFIL",
                onBackClick = if ((uiState as? ProfileUiState.Success)?.isCurrentUser == false) onBackClick else null,
                navigationContent = if ((uiState as? ProfileUiState.Success)?.isCurrentUser == true && onSearchUsersClick != null) {
                    {
                        IconButton(onClick = { onSearchUsersClick?.invoke() }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar usuarios", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                } else null,
                scrollBehavior = scrollBehavior,
                actions = {
                    val state = uiState as? ProfileUiState.Success
                    if (state?.isCurrentUser == true) {
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "Opciones", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.refreshData()
                coroutineScope.launch {
                    delay(400)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { ProfileHeaderShimmer() }
                    items(3) { ProfileActivityCardShimmer() }
                }
            }
            is ProfileUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ErrorStateMessage(message = state.message, onRetry = { viewModel.retry() })
            }
            ProfileUiState.LoggedOut -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            }
            is ProfileUiState.Success -> {
                var username by remember { mutableStateOf(state.user.username) }
                var fullName by remember { mutableStateOf(state.user.fullName) }
                var bio by remember { mutableStateOf(state.user.bio ?: "") }
                var email by remember { mutableStateOf(state.user.email) }

                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { viewModel.onAvatarChange(it) }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ModernAvatar(imageUrl = state.user.avatarUrl, size = 110.dp)

                            if (state.isEditing) {
                                TextButton(onClick = { launcher.launch("image/*") }) {
                                    Text("CAMBIAR FOTO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            if (state.isEditing) {
                                EditProfileFields(
                                    username = username, fullName = fullName, bio = bio,
                                    onUsernameChange = { username = it }, onFullNameChange = { fullName = it },
                                    onBioChange = { bio = it }, onSave = { viewModel.onSaveProfile(username, fullName, bio, email) },
                                    usernameError = state.usernameError
                                )
                            } else {
                                Text(text = state.user.fullName, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(6.dp))
                                Text(text = "@${state.user.username}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                if (!state.isCurrentUser) {
                                    Spacer(Modifier.height(12.dp))
                                    FollowButton(isFollowing = state.isFollowing, onClick = { viewModel.toggleFollow() })
                                }
                                if (bio.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(text = bio, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            ProfileStatsRow(
                                activityCount = profileActivityItems.size,
                                followers = state.followers,
                                following = state.following,
                                onFollowersClick = { onFollowersClick(state.user.id) },
                                onFollowingClick = { onFollowingClick(state.user.id) }
                            )
                        }
                    }

                    stickyHeader {
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            PremiumTabRow(
                                selectedTabIndex = selectedTabIndex,
                                tabs = tabs,
                                onTabSelected = { selectedTabIndex = it }
                            )
                        }
                    }

                    when (selectedTabIndex) {
                        0 -> {
                            if (profileActivityLoading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(32.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            Text(
                                                text = "Recargando…",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else if (profileActivityItems.isEmpty()) {
                                item {
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(40.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Coffee,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = if (state.isCurrentUser) "Tu actividad está vacía" else "Sin actividad reciente",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = if (state.isCurrentUser) "Sigue a otras personas para ver aquí sus reseñas, favoritos y cafés probados." else "Este usuario aún no ha publicado reseñas ni añadido cafés.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        if (state.isCurrentUser && onExploreCafes != null) {
                                            Spacer(Modifier.height(20.dp))
                                            Button(onClick = { onExploreCafes() }) {
                                                Text("Explorar cafés")
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(profileActivityItems, key = { item ->
                                    when (item) {
                                        is ProfileActivityItem.Review -> "review-${item.reviewInfo.review.id}"
                                        is ProfileActivityItem.FirstTimeCoffee -> "first-${item.userId}-${item.coffeeId}"
                                        is ProfileActivityItem.AddedToList -> "list-${item.userId}-${item.listId}-${item.coffeeId}"
                                    }
                                }) { item ->
                                    Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                        ProfileActivityCard(
                                            item = item,
                                            activeUserId = state.activeUser?.id,
                                            onUserClick = onUserClick,
                                            onCoffeeClick = onCoffeeClick,
                                            onListClick = onOpenUserListClick,
                                            isCurrentUser = state.isCurrentUser
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            item {
                                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)) {
                                    Box(Modifier.clickable { showSensoryDetail = true }) {
                                        PremiumCard(
                                            containerColor = if (isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.surface
                                        ) {
                                            Column(Modifier.padding(24.dp)) {
                                                SensoryRadarChart(data = state.sensoryProfile, modifier = Modifier.fillMaxWidth().height(220.dp))
                                                Spacer(Modifier.height(16.dp))
                                                Text("Tus gustos basados en los cafés que consumes, tienes en listas o favoritos y has reseñado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            item {
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    if (state.isCurrentUser) {
                                        ListRowCreateList(onClick = { showCreateListSheet = true })
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    ListRowFavoritos(onClick = { onFavoritosListClick?.invoke() ?: Unit })
                                    if (state.isCurrentUser) {
                                        state.userLists.forEach { list ->
                                            Spacer(Modifier.height(8.dp))
                                            ListRowCustomList(
                                                name = list.name,
                                                onClick = { onOpenListClick?.invoke(list.id, list.name) ?: Unit }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Modals
                if (showSensoryDetail) {
                    SensoryDetailBottomSheet(profile = state.sensoryProfile, onDismiss = { showSensoryDetail = false })
                }

                if (showCreateListSheet) {
                    CreateListBottomSheet(
                        onDismiss = { showCreateListSheet = false },
                        onCreate = { name, isPublic ->
                            viewModel.createList(name, isPublic)
                            showCreateListSheet = false
                        }
                    )
                }

                if (showSettingsSheet) {
                    SettingsBottomSheet(
                        onDismiss = { showSettingsSheet = false },
                        onEditClick = { viewModel.toggleEditMode() },
                        onHistorialClick = onHistorialClick,
                        onDeleteAccountClick = { showDeleteAccountConfirm = true },
                        onLogoutClick = {
                            runWithBiometricReauth(
                                context = context,
                                title = "Confirma tu identidad",
                                subtitle = "Para cerrar sesión en Cafesito",
                                onAuthenticated = { viewModel.logout() },
                                onFallback = { viewModel.logout() }
                            )
                        }
                    )
                }

                if (showDeleteAccountConfirm) {
                    DeleteConfirmationDialog(
                        onDismissRequest = {
                            if (!deletingAccount) showDeleteAccountConfirm = false
                        },
                        title = "Eliminar mi cuenta y mis datos",
                        text = "Tu cuenta quedará inactiva durante 30 días y luego se eliminará con todos tus datos. Si vuelves a iniciar sesión antes, se cancelará el proceso.",
                        onConfirm = {
                            if (deletingAccount) return@DeleteConfirmationDialog
                            deletingAccount = true
                            viewModel.requestAccountDeletion()
                            showDeleteAccountConfirm = false
                            deletingAccount = false
                        }
                    )
                }

                itemToDelete?.let { item ->
                    if (item is UserReviewInfo) {
                        DeleteConfirmationDialog(
                            onDismissRequest = { itemToDelete = null },
                            title = "Eliminar reseña",
                            text = "¿Estás seguro de eliminar esta reseña?",
                            onConfirm = {
                                runWithBiometricReauth(
                                    context = context,
                                    title = "Acción sensible",
                                    subtitle = "Confirma para eliminar",
                                    onAuthenticated = {
                                        viewModel.deleteReview(item.coffeeDetails.coffee.id)
                                        itemToDelete = null
                                    },
                                    onFallback = {
                                        viewModel.deleteReview(item.coffeeDetails.coffee.id)
                                        itemToDelete = null
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
        }
    }
}
