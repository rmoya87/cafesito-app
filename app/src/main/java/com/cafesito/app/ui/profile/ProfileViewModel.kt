package com.cafesito.app.ui.profile

import kotlinx.coroutines.ExperimentalCoroutinesApi
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.*
import com.cafesito.app.ui.utils.ConnectivityObserver
import com.cafesito.shared.domain.Review
import com.cafesito.shared.domain.User
import com.cafesito.shared.domain.repository.ReviewRepository
import com.cafesito.shared.domain.validation.ValidateReviewInputUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.exceptions.HttpRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private data class ProfileInternalState(
    val isEditing: Boolean,
    val generalError: String?,
    val usernameError: String?,
    val tempAvatar: Uri?,
    val loggedOut: Boolean
)

private data class ProfilePrimaryData(
    val activeUser: UserEntity?,
    val targetUser: UserEntity?,
    val userPosts: List<PostWithDetails>,
    val allCoffees: List<CoffeeWithDetails>,
    val allUsers: List<UserEntity>
)

private data class ProfileSecondaryData(
    val allReviews: List<ReviewEntity>,
    val followingMap: Map<Int, Set<Int>>,
    val myFavorites: List<LocalFavorite>,
    val internalState: ProfileInternalState
)

private data class ProfileCombineState(
    val primary: ProfilePrimaryData,
    val secondary: ProfileSecondaryData,
    val userLists: List<UserListRow>,
    val coffeeIdsInUserLists: Set<String>,
    val profileUserDiaryCoffeeIds: Set<String>
)

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Error(val message: String) : ProfileUiState
    data class Success(
        val user: UserEntity,
        val posts: List<PostWithDetails>,
        val favoriteCoffees: List<CoffeeWithDetails>,
        val userReviews: List<UserReviewInfo>,
        val followers: Int,
        val following: Int,
        val isFollowing: Boolean,
        val isCurrentUser: Boolean,
        val isEditing: Boolean,
        val errorMessage: String?,
        val usernameError: String?,
        val myFavoriteIds: Set<String>,
        val activeUser: UserEntity?,
        val sensoryProfile: Map<String, Float>,
        val allUsers: List<UserEntity>,
        val userLists: List<UserListRow>,
        /** IDs de usuarios que el activo sigue (para feed de actividad en mi perfil). */
        val myFollowingIds: Set<Int>
    ) : ProfileUiState
    data object LoggedOut : ProfileUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val coffeeRepository: CoffeeRepository,
    private val socialRepository: SocialRepository,
    private val diaryRepository: DiaryRepository,
    private val reviewRepository: ReviewRepository,
    private val supabaseDataSource: SupabaseDataSource,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {
    private val validateReviewInput = ValidateReviewInputUseCase()

    private val requestedUserId: Int = savedStateHandle["userId"] ?: 0
    private val _isEditing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _usernameError = MutableStateFlow<String?>(null)
    private val _temporaryAvatarUri = MutableStateFlow<Uri?>(null)
    private val _loggedOut = MutableStateFlow(false)
    private val _userLists = MutableStateFlow<List<UserListRow>>(emptyList())
    private val _coffeeIdsInUserLists = MutableStateFlow<Set<String>>(emptySet())
    /** IDs de cafés consumidos por el usuario del perfil (diario, excl. agua) para cálculo ADN. */
    private val _profileUserDiaryCoffeeIds = MutableStateFlow<Set<String>>(emptySet())
    private val _profileActivityItems = MutableStateFlow<List<ProfileActivityItem>>(emptyList())
    val profileActivityItems: StateFlow<List<ProfileActivityItem>> = _profileActivityItems.asStateFlow()

    private val activeUserFlow = userRepository.getActiveUserFlow()
    
    private val targetUserFlow = if (requestedUserId == 0) {
        activeUserFlow
    } else {
        userRepository.getUserByIdFlow(requestedUserId)
    }.distinctUntilChanged()

    /** Perfiles sensoriales del usuario del perfil (para ADN, misma lógica que webapp). */
    private val profileUserSensoryProfilesFlow = targetUserFlow.flatMapLatest { user ->
        flow {
            if (user != null) {
                try {
                    emit(withContext(Dispatchers.IO) { supabaseDataSource.getSensoryProfilesByUserId(user.id) })
                } catch (_: Exception) {
                    emit(emptyList())
                }
            } else {
                emit(emptyList())
            }
        }.flowOn(Dispatchers.IO)
    }

    private val userPostsFlow = flowOf(emptyList<PostWithDetails>())

    private val internalStateFlow = combine(_isEditing, _error, _usernameError, _temporaryAvatarUri, _loggedOut) {
        isEditing, generalError, usernameError, tempAvatar, loggedOut ->
        ProfileInternalState(isEditing, generalError, usernameError, tempAvatar, loggedOut)
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        combine(
            combine(activeUserFlow, targetUserFlow, userPostsFlow, coffeeRepository.allCoffees, userRepository.getAllUsersFlow()) { activeUser, targetUser, userPosts, allCoffees, allUsers ->
                ProfilePrimaryData(activeUser, targetUser, userPosts, allCoffees, allUsers)
            },
            combine(coffeeRepository.allReviews, userRepository.followingMap, coffeeRepository.favorites, internalStateFlow) { allReviews, followingMap, myFavorites, internalState ->
                ProfileSecondaryData(allReviews, followingMap, myFavorites, internalState)
            },
            _userLists,
            _coffeeIdsInUserLists,
            _profileUserDiaryCoffeeIds
        ) { primary, secondary, userLists, coffeeIdsInUserLists, profileUserDiaryCoffeeIds ->
            ProfileCombineState(primary, secondary, userLists, coffeeIdsInUserLists, profileUserDiaryCoffeeIds)
        },
        profileUserSensoryProfilesFlow
    ) { state, profileUserSensoryProfiles ->
        val primary = state.primary
        val secondary = state.secondary
        val userLists = state.userLists
        val coffeeIdsInUserLists = state.coffeeIdsInUserLists
        val profileUserDiaryCoffeeIds = state.profileUserDiaryCoffeeIds
        val activeUser = primary.activeUser
        val targetUser = primary.targetUser
        val userPosts = primary.userPosts
        val allCoffees = primary.allCoffees
        val allUsers = primary.allUsers
        val allReviews = secondary.allReviews
        val followingMap = secondary.followingMap
        val myFavorites = secondary.myFavorites

        val isEditing = secondary.internalState.isEditing
        val generalError = secondary.internalState.generalError
        val usernameError = secondary.internalState.usernameError
        val tempAvatar = secondary.internalState.tempAvatar
        val loggedOut = secondary.internalState.loggedOut

        if (loggedOut) return@combine ProfileUiState.LoggedOut
        if (generalError != null) return@combine ProfileUiState.Error(generalError)
        if (targetUser == null) return@combine ProfileUiState.Loading

        val displayUser = if (isEditing && tempAvatar != null) {
            targetUser.copy(avatarUrl = tempAvatar.toString())
        } else targetUser

        val userReviewsForSensory = allReviews.filter { it.userId == targetUser.id }
        val reviewedCoffees = userReviewsForSensory.mapNotNull { review ->
            allCoffees.find { it.coffee.id == review.coffeeId }?.coffee
        }
        val userFavs = if (targetUser.id == (activeUser?.id ?: -1)) {
            val favIds = myFavorites.map { it.coffeeId }.toSet()
            allCoffees.filter { favIds.contains(it.coffee.id) }.map { it.coffee }
        } else emptyList()
        val listCoffees = if (targetUser.id == (activeUser?.id ?: -1)) {
            allCoffees.filter { it.coffee.id in coffeeIdsInUserLists }.map { it.coffee }
        } else emptyList()
        val diaryCoffees = allCoffees.filter { it.coffee.id in profileUserDiaryCoffeeIds }.map { it.coffee }
        val allRelevantCoffees = (reviewedCoffees + userFavs + listCoffees + diaryCoffees).distinctBy { it.id }

        // Misma lógica que webapp: muestras con al menos un valor > 0 + perfiles sensoriales del usuario
        data class SensorySample(val aroma: Float, val sabor: Float, val cuerpo: Float, val acidez: Float, val dulzura: Float)
        val samples = mutableListOf<SensorySample>()
        fun addSample(aroma: Float, sabor: Float, cuerpo: Float, acidez: Float, dulzura: Float) {
            if (aroma > 0f || sabor > 0f || cuerpo > 0f || acidez > 0f || dulzura > 0f) {
                samples.add(SensorySample(
                    aroma.coerceIn(0f, 10f),
                    sabor.coerceIn(0f, 10f),
                    cuerpo.coerceIn(0f, 10f),
                    acidez.coerceIn(0f, 10f),
                    dulzura.coerceIn(0f, 10f)
                ))
            }
        }
        allRelevantCoffees.forEach { c -> addSample(c.aroma, c.sabor, c.cuerpo, c.acidez, c.dulzura) }
        profileUserSensoryProfiles.forEach { p -> addSample(p.aroma, p.sabor, p.cuerpo, p.acidez, p.dulzura) }

        val sensoryProfile = if (samples.isEmpty()) {
            mapOf("Aroma" to 5f, "Sabor" to 5f, "Cuerpo" to 5f, "Acidez" to 5f, "Dulzura" to 5f)
        } else {
            val n = samples.size.toFloat()
            mapOf(
                "Aroma" to (samples.sumOf { it.aroma.toDouble() } / n).toFloat(),
                "Sabor" to (samples.sumOf { it.sabor.toDouble() } / n).toFloat(),
                "Cuerpo" to (samples.sumOf { it.cuerpo.toDouble() } / n).toFloat(),
                "Acidez" to (samples.sumOf { it.acidez.toDouble() } / n).toFloat(),
                "Dulzura" to (samples.sumOf { it.dulzura.toDouble() } / n).toFloat()
            )
        }

        ProfileUiState.Success(
            user = displayUser,
            posts = userPosts, 
            favoriteCoffees = if (targetUser.id == (activeUser?.id ?: -1)) {
                val favIds = myFavorites.map { it.coffeeId }.toSet()
                allCoffees.filter { favIds.contains(it.coffee.id) }
            } else emptyList(),
            userReviews = userReviewsForSensory.mapNotNull { review ->
                allCoffees.find { it.coffee.id == review.coffeeId }?.let { coffee ->
                    UserReviewInfo(coffee, review, targetUser.fullName, targetUser.avatarUrl)
                }
            },
            followers = followingMap.values.count { it.contains(targetUser.id) },
            following = followingMap[targetUser.id]?.size ?: 0,
            isFollowing = activeUser?.let { followingMap[it.id]?.contains(targetUser.id) } ?: false,
            isCurrentUser = targetUser.id == (activeUser?.id ?: -1),
            isEditing = isEditing,
            errorMessage = generalError,
            usernameError = usernameError,
            myFavoriteIds = myFavorites.map { fav -> fav.coffeeId }.toSet(),
            activeUser = activeUser,
            sensoryProfile = sensoryProfile,
            allUsers = allUsers,
            userLists = userLists,
            myFollowingIds = activeUser?.let { u -> followingMap[u.id] ?: emptySet() } ?: emptySet()
        )
    }
    .catch { e -> 
        Log.e("ProfileViewModel", "Error in UI state flow", e)
        emit(ProfileUiState.Error("Se ha producido un error inesperado."))
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState.Loading)

    init {
        viewModelScope.launch {
            connectivityObserver.observe().collect { 
                if (it == ConnectivityObserver.Status.Available) refreshData()
            }
        }
        viewModelScope.launch {
            uiState.collect { state ->
                if (state is ProfileUiState.Success) refreshProfileActivity(state)
            }
        }
    }

    private fun refreshProfileActivity(success: ProfileUiState.Success) {
        viewModelScope.launch(Dispatchers.IO) {
            val allCoffees = coffeeRepository.allCoffees.first()
            val allUsers = userRepository.getAllUsersFlow().first()
            val usersById = allUsers.associateBy { it.id }
            fun coffeeName(id: String?) = id?.let { allCoffees.find { c -> c.coffee.id == it }?.coffee?.nombre } ?: ""
            /** Resuelve nombre de café desde caché o Supabase (homogeneiza con webapp: mismo nombre en actividad). */
            suspend fun resolveCoffeeName(id: String?): String {
                if (id.isNullOrBlank()) return ""
                val fromCache = coffeeName(id)
                if (fromCache.isNotBlank()) return fromCache
                return coffeeRepository.getCoffeeById(id)?.nombre?.takeIf { it.isNotBlank() } ?: ""
            }

            val items = mutableListOf<ProfileActivityItem>()

            // Reglas de Actividad:
            // - Mi perfil: tus opiniones, tu "primera vez" por café, tus añadidos a listas; y lo mismo por cada usuario que sigues (solo listas públicas de seguidos).
            // - Perfil de terceros: solo actividad de ese usuario (opiniones, primera vez, añadidos a listas); no se muestra actividad de la gente que él sigue.
            success.userReviews.forEach { reviewInfo ->
                items.add(ProfileActivityItem.Review(reviewInfo))
            }

            // Diario: "primera vez" = café que ese usuario ha tomado exactamente una vez (entrada más antigua por café; excluir agua)
            suspend fun firstTimeFromDiary(entries: List<DiaryEntryEntity>, userId: Int, userName: String, avatarUrl: String?): List<ProfileActivityItem.FirstTimeCoffee> {
                val coffeeEntries = entries.filter { (it.type ?: "").uppercase() != "WATER" }
                val countByCoffeeId = coffeeEntries.mapNotNull { it.coffeeId }.groupingBy { it }.eachCount()
                val byCoffee = mutableMapOf<String, DiaryEntryEntity>()
                for (e in coffeeEntries) {
                    val cid = e.coffeeId ?: continue
                    if ((countByCoffeeId[cid] ?: 0) != 1) continue
                    val existing = byCoffee[cid]
                    if (existing == null || (e.timestamp < existing.timestamp)) byCoffee[cid] = e
                }
                return buildList {
                    for (e in byCoffee.values) {
                        val name = e.coffeeName.ifBlank { resolveCoffeeName(e.coffeeId) }
                        add(
                            ProfileActivityItem.FirstTimeCoffee(
                                userId = userId,
                                userName = userName,
                                avatarUrl = avatarUrl,
                                timestamp = e.timestamp,
                                coffeeId = e.coffeeId ?: "",
                                coffeeName = name
                            )
                        )
                    }
                }
            }

            // Listas: ítems con meta (solo públicos si es perfil ajeno); resolveCoffeeName para homogeneizar nombres con webapp
            suspend fun listItemsForUser(userId: Int, userName: String, avatarUrl: String?, onlyPublic: Boolean): List<ProfileActivityItem.AddedToList> {
                return try {
                    val rows = supabaseDataSource.getListItemsWithMetaForUser(userId).filter { !onlyPublic || it.isPublic }
                    buildList {
                        for (row in rows) {
                            add(
                                ProfileActivityItem.AddedToList(
                                    userId = userId,
                                    userName = userName,
                                    avatarUrl = avatarUrl,
                                    timestamp = row.createdAt,
                                    coffeeId = row.coffeeId,
                                    coffeeName = resolveCoffeeName(row.coffeeId),
                                    listId = row.listId,
                                    listName = row.listName
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val targetUser = success.user
            val targetUserAvatar = targetUser.avatarUrl

            // Actividad del usuario cuyo perfil se muestra: opiniones (ya añadidas), primera vez, añadidos a listas.
            // Para perfil de terceros: solo listas públicas de ese usuario. Para mi perfil: todas mis listas.
            try {
                val diaryEntries = supabaseDataSource.getDiaryEntries(targetUser.id)
                firstTimeFromDiary(diaryEntries, targetUser.id, targetUser.fullName, targetUserAvatar).forEach { items.add(it) }
            } catch (_: Exception) { }
            listItemsForUser(targetUser.id, targetUser.fullName, targetUserAvatar, onlyPublic = !success.isCurrentUser).forEach { items.add(it) }

            // Solo en mi perfil: añadir actividad de cada usuario que sigo (opiniones, primera vez, solo listas públicas).
            // Perfil de terceros: no se muestra actividad de la gente que ese usuario sigue.
            // Usar siempre getFollowingIdsForUser (BD) en mi perfil para evitar condición de carrera con el Flow (myFollowingIds puede llegar vacío).
            val myFollowingIds = if (success.isCurrentUser) {
                success.activeUser?.id?.let { userRepository.getFollowingIdsForUser(it) } ?: emptySet()
            } else {
                success.myFollowingIds
            }
            if (success.isCurrentUser && myFollowingIds.isNotEmpty()) {
                for (followedId in myFollowingIds) {
                    if (followedId == targetUser.id) continue
                    val followed = usersById[followedId] ?: userRepository.getUserById(followedId) ?: continue
                    val name = followed.fullName
                    val avatar = followed.avatarUrl
                    try {
                        val diaryEntries = supabaseDataSource.getDiaryEntries(followedId)
                        firstTimeFromDiary(diaryEntries, followedId, name, avatar).forEach { items.add(it) }
                    } catch (_: Exception) { }
                    listItemsForUser(followedId, name, avatar, onlyPublic = true).forEach { items.add(it) }
                }
                // Reviews de seguidos: no están en success.userReviews (que son solo del target). Hay que obtener allReviews y filtrar por myFollowingIds.
                val allReviews = coffeeRepository.allReviews.first()
                allReviews.filter { myFollowingIds.contains(it.userId) }.forEach { review ->
                    val reviewUser = usersById[review.userId] ?: userRepository.getUserById(review.userId)
                    allCoffees.find { it.coffee.id == review.coffeeId }?.let { coffee ->
                        items.add(ProfileActivityItem.Review(UserReviewInfo(coffee, review, reviewUser?.fullName, reviewUser?.avatarUrl)))
                    }
                }
            }

            _profileActivityItems.value = items.sortedByDescending { it.timestamp }
        }
    }

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userRepository.restoreSessionFromSupabaseIfNeeded()
                coffeeRepository.syncCoffees()
                coffeeRepository.syncFavoritesFromRemote()
                coffeeRepository.triggerRefresh()
                userRepository.syncUsers()
                userRepository.syncFollows()
                userRepository.triggerRefresh()
                socialRepository.triggerRefresh()
                userRepository.getActiveUser()?.let { user ->
                    _userLists.value = supabaseDataSource.getUserLists(user.id)
                    _coffeeIdsInUserLists.value = supabaseDataSource.getCoffeeIdsInUserLists(user.id).toSet()
                } ?: run {
                    _userLists.value = emptyList()
                    _coffeeIdsInUserLists.value = emptySet()
                }
                val profileUserId = if (requestedUserId == 0) userRepository.getActiveUser()?.id else requestedUserId
                _profileUserDiaryCoffeeIds.value = if (profileUserId != null) {
                    runCatching { supabaseDataSource.getDiaryEntries(profileUserId) }.getOrElse { emptyList() }
                        .filter { (it.type ?: "").uppercase() != "WATER" }
                        .mapNotNull { it.coffeeId }
                        .toSet()
                } else emptySet()
                // Tras sincronizar follows, recargar actividad de mi perfil con los IDs de usuarios que sigo.
                // Leer del repositorio (BD) para no depender del Flow followingMap, que puede no haber reemitido aún.
                val state = uiState.value
                if (state is ProfileUiState.Success && state.isCurrentUser) {
                    val myIds = state.activeUser?.id?.let { userRepository.getFollowingIdsForUser(it) } ?: emptySet()
                    refreshProfileActivity(state.copy(myFollowingIds = myIds))
                }
            } catch (e: Exception) { }
        }
    }

    fun createList(name: String, isPublic: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = userRepository.getActiveUser() ?: return@launch
                val newList = supabaseDataSource.createUserList(user.id, name, isPublic)
                _userLists.update { it + newList }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error creating list", e)
            }
        }
    }

    fun toggleEditMode() { _isEditing.value = !_isEditing.value }
    fun onAvatarChange(uri: Uri?) { _temporaryAvatarUri.value = uri }

    fun onSaveProfile(username: String, fullName: String, bio: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val activeUser = userRepository.getActiveUser() ?: return@launch
                val updatedUser = activeUser.copy(
                    username = username.trim(),
                    fullName = fullName,
                    avatarUrl = _temporaryAvatarUri.value?.toString() ?: activeUser.avatarUrl,
                    bio = bio,
                    email = email
                )
                userRepository.upsertUser(updatedUser)
                _isEditing.value = false
            } catch (e: Exception) {
                _error.value = "Error al guardar"
            }
        }
    }

    fun retry() {
        _error.value = null
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            _loggedOut.value = true
        }
    }

    fun requestAccountDeletion() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userRepository.requestAccountDeletionAndLogout()
                _loggedOut.value = true
            } catch (e: Exception) {
                _error.value = "No se pudo iniciar la eliminación de la cuenta"
            }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch(Dispatchers.IO) {
            val me = userRepository.getActiveUser() ?: return@launch
            val targetId = if (requestedUserId == 0) me.id else requestedUserId
            userRepository.toggleFollow(me.id, targetId = targetId)
        }
    }

    fun onToggleFavorite(coffeeId: String, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { 
            try { coffeeRepository.toggleFavorite(coffeeId, isFavorite) } catch (e: Exception) { }
        }
    }

    fun onToggleLike(postId: String) { /* posts removed */ }
    fun onAddComment(postId: String, text: String) { /* posts removed */ }
    fun deletePost(postId: String) { /* posts removed */ }
    fun updatePost(postId: String, newText: String, newImageUrl: String) { /* posts removed */ }

    fun deleteReview(coffeeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val me = userRepository.getActiveUser() ?: return@launch
            coffeeRepository.deleteReview(coffeeId, me.id)
        }
    }

    fun updateReview(coffeeId: String, rating: Float, comment: String, imageUrl: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val me = userRepository.getActiveUser() ?: return@launch
            reviewRepository.updateReview(
                Review(
                    user = me.toDomainUser(), coffeeId = coffeeId, rating = rating,
                    comment = comment, imageUrl = imageUrl, timestamp = System.currentTimeMillis()
                )
            )
            coffeeRepository.triggerRefresh()
        }
    }
}

private fun UserEntity.toDomainUser(): User = User(
    id = id, username = username, fullName = fullName, avatarUrl = avatarUrl, email = email, bio = bio
)
