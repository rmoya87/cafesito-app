package com.cafesito.app.data

import android.util.Log
import com.cafesito.app.ui.utils.ConnectivityObserver
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val supabaseClient: SupabaseClient,
    private val connectivityObserver: ConnectivityObserver,
    private val externalScope: CoroutineScope
) {
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    fun triggerRefresh() {
        _refreshTrigger.tryEmit(Unit)
    }

    private suspend fun ensureConnected() {
        if (connectivityObserver.observe().first() != ConnectivityObserver.Status.Available) {
            throw NoConnectivityException("No hay conexión a internet.")
        }
    }

    val followingMap: Flow<Map<Int, Set<Int>>> = _refreshTrigger
        .flatMapLatest {
            networkBoundResource(
                resourceKey = "following_map",
                query = { 
                    userDao.getAllFollows().map { follows ->
                        follows.groupBy { it.followerId }
                            .mapValues { entry -> entry.value.map { it.followedId }.toSet() }
                    }
                },
                fetch = { supabaseDataSource.getAllFollows() },
                saveFetchResult = { follows ->
                    withContext(Dispatchers.IO) { userDao.insertFollows(follows) }
                },
                scope = externalScope,
                connectivityObserver = connectivityObserver
            )
        }.flowOn(Dispatchers.IO)

    suspend fun signInWithSupabase(token: String): String? {
        ensureConnected()
        return try {
            supabaseClient.auth.signInWith(IDToken) {
                idToken = token
                provider = Google
            }
            supabaseClient.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.e("UserRepository", "SignIn failed", e)
            null
        }
    }

    suspend fun logout() {
        try {
            if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
                supabaseClient.auth.signOut()
            }
            userDao.deleteAllUsers()
            lastSyncMap.clear()
            triggerRefresh()
        } catch (e: Exception) {
            Log.e("UserRepository", "Logout failed", e)
        }
    }

    fun getAllUsersFlow(): Flow<List<UserEntity>> = _refreshTrigger
        .flatMapLatest {
            networkBoundResource(
                resourceKey = "all_users",
                query = { userDao.getAllUsers() },
                fetch = { supabaseDataSource.getAllUsers() },
                saveFetchResult = { users ->
                    withContext(Dispatchers.IO) { userDao.insertUsers(users) }
                },
                scope = externalScope,
                connectivityObserver = connectivityObserver
            )
        }.flowOn(Dispatchers.IO)

    fun getNotificationsForUser(userId: Int): Flow<List<NotificationEntity>> = _refreshTrigger
        .flatMapLatest {
             flow {
                 val notifications = try {
                     supabaseDataSource.getNotificationsForUser(userId)
                 } catch (e: Exception) {
                     emptyList()
                 }
                 emit(notifications)
             }
        }.flowOn(Dispatchers.IO)

    fun getUserByIdFlow(userId: Int): Flow<UserEntity?> = _refreshTrigger
        .flatMapLatest {
            networkBoundResource(
                resourceKey = "profile_user_$userId",
                query = { flow { emit(userDao.getUserById(userId)) } },
                fetch = { supabaseDataSource.getUserById(userId) },
                saveFetchResult = { remote ->
                    if (remote != null) withContext(Dispatchers.IO) { userDao.upsertUser(remote) }
                },
                shouldFetch = { it == null },
                scope = externalScope,
                connectivityObserver = connectivityObserver
            )
        }.flowOn(Dispatchers.IO)

    suspend fun getAllUsersList(): List<UserEntity> = userDao.getAllUsers().first()

    fun getActiveUserFlow(): Flow<UserEntity?> = combine(
        supabaseClient.auth.sessionStatus,
        _refreshTrigger
    ) { status, _ -> status }.flatMapLatest { status ->
        // ✅ CORRECCIÓN ROBUSTA: Si el estado es de carga, esperamos para evitar NotAuthenticated falso.
        if (status::class.simpleName?.contains("Loading", ignoreCase = true) == true) {
            return@flatMapLatest flow { }
        }
        
        val uid = supabaseClient.auth.currentUserOrNull()?.id
        if (uid != null) {
            userDao.getUserByGoogleIdFlow(uid).onStart {
                if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
                    externalScope.launch {
                        try {
                            val remote = supabaseDataSource.getUserByGoogleId(uid)
                            if (remote != null) userDao.upsertUser(remote)
                        } catch (e: Exception) { }
                    }
                }
            }
        } else flowOf(null)
    }.flowOn(Dispatchers.IO)

    suspend fun getActiveUser(): UserEntity? {
        val currentUid = supabaseClient.auth.currentUserOrNull()?.id ?: return null
        val local = userDao.getUserByGoogleId(currentUid)
        if (local == null && connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                val remote = supabaseDataSource.getUserByGoogleId(currentUid)
                if (remote != null) {
                    userDao.upsertUser(remote)
                    return remote
                }
            } catch (e: Exception) { }
        }
        return local
    }

    suspend fun getUserById(userId: Int): UserEntity? {
        val local = userDao.getUserById(userId)
        if (local == null && connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                val remote = supabaseDataSource.getUserById(userId)
                if (remote != null) {
                    userDao.upsertUser(remote)
                    return remote
                }
            } catch (e: Exception) { }
        }
        return local
    }

    suspend fun getUserByGoogleId(googleId: String): UserEntity? {
        val local = userDao.getUserByGoogleId(googleId)
        if (local == null && connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                val remote = supabaseDataSource.getUserByGoogleId(googleId)
                if (remote != null) {
                    userDao.upsertUser(remote)
                    return remote
                }
            } catch (e: Exception) { }
        }
        return local
    }

    suspend fun getUserByUsername(username: String): UserEntity? {
        val local = userDao.getUserByUsername(username)
        if (local == null && connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                val remote = supabaseDataSource.getUserByUsername(username)
                if (remote != null) {
                    userDao.upsertUser(remote)
                    return remote
                }
            } catch (e: Exception) { }
        }
        return local
    }

    suspend fun upsertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.upsertUser(user)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch { try { supabaseDataSource.upsertUser(user) } catch (e: Exception) { } }
        }
    }

    suspend fun upsertLocalOnly(user: UserEntity) {
        userDao.upsertUser(user)
    }

    suspend fun updateFcmToken(token: String) {
        val currentUser = getActiveUser() ?: return
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch {
                try {
                    supabaseDataSource.insertUserToken(UserTokenEntity(userId = currentUser.id, fcmToken = token))
                } catch (e: Exception) { }
            }
        }
    }

    suspend fun toggleFollow(followerId: Int, targetId: Int) = withContext(Dispatchers.IO) {
        val follow = FollowEntity(followerId, targetId)
        val isFollowing = userDao.getAllFollows().first().any { it.followerId == followerId && it.followedId == targetId }
        
        if (isFollowing) userDao.deleteFollow(follow) else userDao.insertFollow(follow)

        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch {
                try {
                    if (isFollowing) supabaseDataSource.deleteFollow(followerId, targetId)
                    else supabaseDataSource.insertFollow(follow)
                } catch (e: Exception) { }
            }
        }
    }

    suspend fun syncUsers() {
        val users = supabaseDataSource.getAllUsers()
        userDao.insertUsers(users)
    }

    suspend fun syncFollows() {
        val follows = supabaseDataSource.getAllFollows()
        userDao.insertFollows(follows)
    }

    suspend fun markNotificationRead(notificationId: Int) {
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch { try { supabaseDataSource.markNotificationRead(notificationId) } catch (e: Exception) { } }
        }
    }

    suspend fun markAllNotificationsRead(userId: Int) {
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch { try { supabaseDataSource.markAllNotificationsRead(userId) } catch (e: Exception) { } }
        }
    }

    suspend fun deleteNotification(notificationId: Int) {
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch { try { supabaseDataSource.deleteNotification(notificationId) } catch (e: Exception) { } }
        }
    }

    suspend fun insertUsers(users: List<UserEntity>) {
        userDao.insertUsers(users)
    }
}
