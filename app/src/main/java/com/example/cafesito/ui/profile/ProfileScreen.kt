package com.example.cafesito.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.data.PostWithDetails
import com.example.cafesito.data.UserEntity
import com.example.cafesito.data.UserReviewInfo
import com.example.cafesito.ui.components.*
import com.example.cafesito.ui.theme.*
import com.example.cafesito.ui.timeline.CommentsSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onUserClick: (Int) -> Unit,
    onCoffeeClick: (String) -> Unit,
    onFollowersClick: (Int) -> Unit,
    onFollowingClick: (Int) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("POSTS", "ADN", "FAVORITOS", "RESEÑAS")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    val postsListState = rememberLazyListState()
    val adnListState = rememberLazyListState()
    val favoritesListState = rememberLazyListState()
    val reviewsListState = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCommentSheetId by remember { mutableStateOf<String?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSensoryDetail by remember { mutableStateOf(false) }

    var showPostOptions by remember { mutableStateOf<PostWithDetails?>(null) }
    var showReviewOptions by remember { mutableStateOf<UserReviewInfo?>(null) }

    var postToEdit by remember { mutableStateOf<PostWithDetails?>(null) }
    var reviewToEdit by remember { mutableStateOf<UserReviewInfo?>(null) }
    var itemToDelete by remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshData() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SoftOffWhite,
        topBar = {
            GlassyTopBar(
                title = "PERFIL",
                onBackClick = if ((uiState as? ProfileUiState.Success)?.isCurrentUser == false) onBackClick else null,
                scrollBehavior = scrollBehavior,
                actions = {
                    val state = uiState as? ProfileUiState.Success
                    if (state?.isCurrentUser == true) {
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Ajustes", tint = EspressoDeep)
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    item { ProfileHeaderShimmer() }
                    items(3) { PostCardShimmer() }
                }
            }
            is ProfileUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.message) }
            is ProfileUiState.Success -> {
                var username by remember { mutableStateOf(state.user.username) }
                var fullName by remember { mutableStateOf(state.user.fullName) }
                var bio by remember { mutableStateOf(state.user.bio ?: "") }
                var email by remember { mutableStateOf(state.user.email) }

                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { viewModel.onAvatarChange(it) }
                
                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    viewModel.healthConnectRepository.healthConnectManager.healthConnectClient?.permissionController?.createRequestPermissionResultContract() ?: ActivityResultContracts.RequestMultiplePermissions()
                ) { granted ->
                    if (granted.containsAll(viewModel.healthConnectRepository.permissions)) {
                        viewModel.onToggleHealthConnect(true)
                    } else {
                        viewModel.onToggleHealthConnect(false)
                    }
                }

                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ModernAvatar(imageUrl = state.user.avatarUrl, size = 110.dp)

                                if (state.isEditing) {
                                    TextButton(onClick = { launcher.launch("image/*") }) {
                                        Text("CAMBIAR FOTO", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                if (state.isEditing) {
                                    EditProfileFields(
                                        username = username,
                                        fullName = fullName,
                                        bio = bio,
                                        onUsernameChange = { username = it },
                                        onFullNameChange = { fullName = it },
                                        onBioChange = { bio = it },
                                        onSave = { viewModel.onSaveProfile(username, fullName, bio, email) },
                                        usernameError = state.usernameError
                                    )
                                } else {
                                    Text(
                                        text = state.user.fullName,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = EspressoDeep
                                    )
                                    Text(
                                        text = "@${state.user.username}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = CaramelAccent,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (bio.isNotBlank()) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = bio,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(24.dp))

                                ProfileStatsRow(
                                    posts = state.posts.size,
                                    followers = state.followers,
                                    following = state.following,
                                    onFollowersClick = { onFollowersClick(state.user.id) },
                                    onFollowingClick = { onFollowingClick(state.user.id) }
                                )

                                if (!state.isCurrentUser) {
                                    Spacer(Modifier.height(24.dp))
                                    FollowButton(
                                        isFollowing = state.isFollowing,
                                        onClick = { viewModel.toggleFollow() }
                                    )
                                }
                            }
                        }

                        stickyHeader {
                            PremiumTabRow(
                                selectedTabIndex = pagerState.currentPage,
                                tabs = tabs,
                                onTabSelected = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(it) }
                                }
                            )
                        }

                        item {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillParentMaxHeight(),
                                verticalAlignment = Alignment.Top,
                            ) {
                                when (it) {
                                    0 -> ProfilePosts(
                                        posts = state.posts,
                                        isCurrentUser = state.isCurrentUser,
                                        activeUser = state.activeUser,
                                        viewModel = viewModel,
                                        onUserClick = onUserClick,
                                        listState = postsListState,
                                        onCommentClick = { c -> showCommentSheetId = c },
                                        onEditClick = { post -> showPostOptions = post },
                                        onDeleteClick = { post -> itemToDelete = post }
                                    )
                                    1 -> ProfileAdn(state, listState = adnListState) { showSensoryDetail = true }
                                    2 -> ProfileFavorites(state.favoriteCoffees, viewModel, onCoffeeClick, listState = favoritesListState)
                                    3 -> ProfileReviews(
                                        userReviews = state.userReviews,
                                        isCurrentUser = state.isCurrentUser,
                                        onCoffeeClick = onCoffeeClick,
                                        listState = reviewsListState,
                                        onEditClick = { review -> showReviewOptions = review },
                                        onDeleteClick = { review -> itemToDelete = review }
                                    )
                                }
                            }
                        }
                    }
                }

                // Modals & Dialogs
                if (showSensoryDetail) {
                    SensoryDetailBottomSheet(
                        profile = state.sensoryProfile,
                        onDismiss = { showSensoryDetail = false }
                    )
                }

                if (showSettingsSheet) {
                    SettingsBottomSheet(
                        onDismiss = { showSettingsSheet = false },
                        onEditClick = { viewModel.toggleEditMode() },
                        onLogoutClick = { viewModel.logout() },
                        healthConnectEnabled = state.healthConnectEnabled,
                        onHealthConnectToggle = { enabled ->
                            if (enabled) {
                                coroutineScope.launch {
                                    if (viewModel.healthConnectRepository.hasPermissions()) {
                                        viewModel.onToggleHealthConnect(true)
                                    } else {
                                        requestPermissionLauncher.launch(viewModel.healthConnectRepository.permissions)
                                    }
                                }
                            } else {
                                viewModel.onToggleHealthConnect(false)
                            }
                        }
                    )
                }

                showPostOptions?.let { post ->
                    PostOptionsBottomSheet(
                        onDismiss = { showPostOptions = null },
                        onEditClick = { 
                            showPostOptions = null
                            postToEdit = post 
                        },
                        onDeleteClick = { 
                            showPostOptions = null
                            itemToDelete = post 
                        }
                    )
                }

                showReviewOptions?.let { review ->
                    ReviewOptionsBottomSheet(
                        onDismiss = { showReviewOptions = null },
                        onEditClick = { 
                            showReviewOptions = null
                            reviewToEdit = review 
                        },
                        onDeleteClick = { 
                            showReviewOptions = null
                            itemToDelete = review 
                        }
                    )
                }

                itemToDelete?.let { item ->
                    AlertDialog(
                        onDismissRequest = { itemToDelete = null },
                        title = { Text("Eliminar permanentemente", fontWeight = FontWeight.Bold) },
                        text = { Text("¿Estás seguro de que deseas borrar este contenido? No se podrá recuperar.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (item is PostWithDetails) viewModel.deletePost(item.post.id)
                                    else if (item is UserReviewInfo) viewModel.deleteReview(item.coffeeDetails.coffee.id)
                                    itemToDelete = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                            ) { Text("ELIMINAR") }
                        },
                        dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("CANCELAR", color = Color.Gray) } },
                        shape = RoundedCornerShape(28.dp),
                        containerColor = Color.White
                    )
                }

                postToEdit?.let { details ->
                    EditPostBottomSheet(
                        initialText = details.post.comment,
                        initialImage = details.post.imageUrl,
                        onDismiss = { postToEdit = null },
                        onConfirm = { newText, newImageUrl ->
                            viewModel.updatePost(details.post.id, newText, newImageUrl)
                            postToEdit = null
                        }
                    )
                }

                reviewToEdit?.let { info ->
                    EditReviewBottomSheet(
                        initialRating = info.review.rating,
                        initialComment = info.review.comment,
                        initialImage = info.review.imageUrl,
                        onDismiss = { reviewToEdit = null },
                        onConfirm = { rating, comment, imageUrl ->
                            viewModel.updateReview(info.coffeeDetails.coffee.id, rating, comment, imageUrl)
                            reviewToEdit = null
                        }
                    )
                }

                showCommentSheetId?.let { id ->
                    CommentsSheet(
                        postId = id,
                        onDismiss = { showCommentSheetId = null },
                        onAddComment = { text -> viewModel.onAddComment(id, text) },
                        onNavigateToProfile = { userId ->
                            showCommentSheetId = null
                            onUserClick(userId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfilePosts(
    posts: List<PostWithDetails>,
    isCurrentUser: Boolean,
    activeUser: UserEntity?,
    viewModel: ProfileViewModel,
    onUserClick: (Int) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    onCommentClick: (String) -> Unit,
    onEditClick: (PostWithDetails) -> Unit,
    onDeleteClick: (PostWithDetails) -> Unit
) {
    LazyColumn(state = listState, contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)) {
        items(posts) { item ->
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                PostCard(
                    details = item,
                    onUserClick = { onUserClick(item.author.id) },
                    onCommentClick = { onCommentClick(item.post.id) },
                    onLikeClick = { viewModel.onToggleLike(item.post.id) },
                    isLiked = activeUser?.let { me -> item.likes.any { it.userId == me.id } } ?: false,
                    showHeader = false,
                    isOwnPost = isCurrentUser,
                    onEditClick = { onEditClick(item) },
                    onDeleteClick = { onDeleteClick(item) }
                )
            }
        }
    }
}

@Composable
fun ProfileAdn(
    state: ProfileUiState.Success,
    listState: LazyListState = rememberLazyListState(),
    onShowSensoryDetail: () -> Unit
) {
    LazyColumn(state = listState, contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)) {
        item {
            PremiumCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable(onClick = onShowSensoryDetail)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    SensoryRadarChart(
                        data = state.sensoryProfile,
                        lineColor = CaramelAccent,
                        fillColor = CaramelAccent.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pulsa para descubrir tus preferencias",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
        item {
            Text(
                text = "Tu ADN se elabora analizando tus cafés favoritos y tus reseñas.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun ProfileFavorites(
    favoriteCoffees: List<CoffeeWithDetails>,
    viewModel: ProfileViewModel,
    onCoffeeClick: (String) -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    LazyColumn(state = listState, contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)) {
        items(favoriteCoffees) { item ->
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                CoffeeFavoritePremiumItem(
                    coffeeDetails = item,
                    onFavoriteClick = { viewModel.onToggleFavorite(item.coffee.id, false) },
                    onClick = { onCoffeeClick(item.coffee.id) }
                )
            }
        }
    }
}

@Composable
fun ProfileReviews(
    userReviews: List<UserReviewInfo>,
    isCurrentUser: Boolean,
    onCoffeeClick: (String) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    onEditClick: (UserReviewInfo) -> Unit,
    onDeleteClick: (UserReviewInfo) -> Unit
) {
    LazyColumn(state = listState, contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)) {
        items(userReviews) { item ->
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                UserReviewCard(
                    info = item,
                    showHeader = false,
                    isOwnReview = isCurrentUser,
                    onEditClick = { onEditClick(item) },
                    onDeleteClick = { onDeleteClick(item) },
                    onClick = { onCoffeeClick(item.coffeeDetails.coffee.id) }
                )
            }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensoryDetailBottomSheet(profile: Map<String, Float>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.padding(24.dp).padding(bottom = 48.dp)) {
            Text(
                text = "ANÁLISIS DE PREFERENCIAS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = EspressoDeep,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            val sorted = profile.toList().sortedByDescending { it.second }
            val highest = sorted.getOrNull(0)
            val second = sorted.getOrNull(1)

            if (highest != null) {
                val primaryName = highest.first.lowercase()
                val secondaryName = second?.first?.lowercase()

                Text(
                    text = if (secondaryName != null && second!!.second > 3f) {
                        "Tu paladar es una combinación experta de $primaryName y $secondaryName."
                    } else {
                        "Tu paladar destaca principalmente por preferir notas de $primaryName."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = EspressoDeep,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                
                val description = when(highest.first) {
                    "Acidez" -> "Buscas brillo en cada taza. Te apasionan los perfiles cítricos y vibrantes de cafés de altura (1500m+)."
                    "Dulzura" -> "Eres amante de lo meloso. Prefieres cafés con procesos naturales que resaltan notas de chocolate, caramelo y frutas maduras."
                    "Cuerpo" -> "Para ti, la textura lo es todo. Disfrutas de esa sensación aterciopelada y densa, típica de tuestes medios y perfiles clásicos."
                    "Aroma" -> "Tu ritual empieza con el olfato. Te atraen las fragancias complejas, florales y especiadas que inundan la habitación."
                    "Sabor" -> "Buscas la máxima intensidad y complejidad gustativa. Valoras la persistencia de las notas tras cada sorbo."
                    else -> "Disfrutas de una complejidad excepcional y un balance perfectamente equilibrado en tu ADN cafetero."
                }
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, lineHeight = 22.sp)

                val (idealType, idealOrigin) = when(highest.first) {
                    "Acidez" -> "Lavados de alta montaña" to "Etiopía o Colombia (Nariño)"
                    "Dulzura" -> "Procesos Natural o Honey" to "Brasil o El Salvador"
                    "Cuerpo" -> "Tuestes medios / Naturales" to "Sumatra o Guatemala"
                    "Aroma" -> "Variedades florales / Geishas" to "Panamá o Ruanda"
                    "Sabor" -> "Micro-lotes de especialidad" to "Costa Rica o Kenia"
                    else -> "Blends equilibrados" to "Cualquier origen de especialidad"
                }

                Spacer(Modifier.height(24.dp))
                Surface(
                    color = CaramelAccent.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CaramelAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CaramelAccent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("RECOMENDACIÓN IDEAL", style = MaterialTheme.typography.labelMedium, color = CaramelAccent, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(text = "Deberías probar: $idealType", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = EspressoDeep)
                        Text(text = "Orígenes sugeridos: $idealOrigin", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EspressoDeep),
                shape = RoundedCornerShape(28.dp)
            ) { Text("CONTINUAR EXPLORANDO", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun ProfileStatsRow(
    posts: Int,
    followers: Int,
    following: Int,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("Posts", posts.toString())
        StatItem("Seguidores", followers.toString(), onFollowersClick)
        StatItem("Siguiendo", following.toString(), onFollowingClick)
    }
}

@Composable
fun StatItem(label: String, value: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).let { 
            if (onClick != null) it.clickable(onClick = onClick) else it 
        }.padding(8.dp)
    ) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = EspressoDeep)
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelLarge, color = CaramelAccent, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
fun CoffeeFavoritePremiumItem(
    coffeeDetails: CoffeeWithDetails,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    PremiumCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = coffeeDetails.coffee.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(20.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = coffeeDetails.coffee.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = coffeeDetails.coffee.marca, style = MaterialTheme.typography.bodySmall, color = CaramelAccent)
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = ElectricRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun FollowButton(isFollowing: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.7f).height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFollowing) SoftOffWhite else EspressoDeep,
            contentColor = if (isFollowing) EspressoDeep else Color.White
        ),
        shape = RoundedCornerShape(24.dp),
        border = if (isFollowing) BorderStroke(1.dp, BorderLight) else null
    ) {
        Text(if (isFollowing) "SIGUIENDO" else "SEGUIR", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun EditProfileFields(
    username: String,
    fullName: String,
    bio: String,
    onUsernameChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onSave: () -> Unit,
    usernameError: String?
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        OutlinedTextField(
            value = username, onValueChange = onUsernameChange, label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            isError = usernameError != null, supportingText = { if (usernameError != null) Text(usernameError) }
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = fullName, onValueChange = onFullNameChange, label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = bio, onValueChange = onBioChange, label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), minLines = 3
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSave, modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen), shape = RoundedCornerShape(28.dp)
        ) { Text("GUARDAR CAMBIOS", fontWeight = FontWeight.Bold) }
    }
}
