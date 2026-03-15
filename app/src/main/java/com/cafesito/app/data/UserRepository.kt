package com.cafesito.app.data

import android.util.Log
import android.content.SharedPreferences
import com.cafesito.app.ui.utils.ConnectivityObserver
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val sessionDao: SessionDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val supabaseClient: SupabaseClient,
    private val connectivityObserver: ConnectivityObserver,
    private val sharedPreferences: SharedPreferences,
    private val externalScope: CoroutineScope
) {
    enum class AccountLifecycleSyncResult { None, Reactivated, Deleted }

    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    private val prefsLock = Any()

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
                fetch = { getAllFollowsWithAuthRetry() },
                saveFetchResult = { follows ->
                    withContext(Dispatchers.IO) { userDao.insertFollows(follows) }
                },
                scope = externalScope,
                connectivityObserver = connectivityObserver
            )
        }
        .distinctUntilChanged()
        .onStart { emit(emptyMap()) }
        .flowOn(Dispatchers.IO)

    suspend fun signInWithSupabase(token: String): String? = withContext(Dispatchers.IO) {
        ensureConnected()
        try {
            supabaseClient.auth.signInWith(IDToken) {
                idToken = token
                provider = Google
            }
            val uid = supabaseClient.auth.currentUserOrNull()?.id
            if (uid != null) {
                userDao.getUserByGoogleId(uid)?.let { localUser ->
                    sessionDao.setSession(ActiveSessionEntity(userId = localUser.id))
                }
            }
            uid
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
            sessionDao.clearSession()
            userDao.deleteAllUsers()
            // Limpiar caché de sincronización si existe
            triggerRefresh()
        } catch (e: Exception) {
            Log.e("UserRepository", "Logout failed", e)
        }
    }

    suspend fun requestAccountDeletionAndLogout() {
        val currentUser = getActiveUser() ?: return
        ensureConnected()
        val scheduledDeletionAt = System.currentTimeMillis() + ACCOUNT_DELETION_GRACE_MS
        supabaseDataSource.requestAccountDeletion(currentUser.id, scheduledDeletionAt)
        logout()
    }

    suspend fun syncAccountLifecycleOnLogin(userId: Int): AccountLifecycleSyncResult = withContext(Dispatchers.IO) {
        if (connectivityObserver.observe().first() != ConnectivityObserver.Status.Available) {
            return@withContext AccountLifecycleSyncResult.None
        }
        return@withContext try {
            val info = supabaseDataSource.getAccountLifecycleInfo(userId) ?: return@withContext AccountLifecycleSyncResult.None
            if (info.accountStatus != "inactive_pending_deletion") return@withContext AccountLifecycleSyncResult.None

            val now = System.currentTimeMillis()
            val scheduled = info.scheduledDeletionAt ?: 0L
            if (scheduled > 0L && scheduled <= now) {
                supabaseDataSource.hardDeleteAccountData(userId)
                logout()
                AccountLifecycleSyncResult.Deleted
            } else {
                supabaseDataSource.cancelAccountDeletion(userId)
                AccountLifecycleSyncResult.Reactivated
            }
        } catch (e: Exception) {
            // Si backend aún no tiene columnas de ciclo de vida, no bloqueamos login.
            AccountLifecycleSyncResult.None
        }
    }

    fun getActiveUserFlow(): Flow<UserEntity?> = sessionDao.getActiveSession()
        .flatMapLatest { session ->
            if (session != null) {
                userDao.getUserByIdFlow(session.userId)
            } else {
                flowOf(null)
            }
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    suspend fun getActiveUser(): UserEntity? = withContext(Dispatchers.IO) {
        val session = sessionDao.getActiveSession().first() ?: return@withContext null
        userDao.getUserById(session.userId)
    }
    
    suspend fun upsertLocalOnly(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.upsertUser(user)
        sessionDao.setSession(ActiveSessionEntity(userId = user.id))
    }

    suspend fun getUserByGoogleId(googleId: String): UserEntity? = withContext(Dispatchers.IO) {
        val local = userDao.getUserByGoogleId(googleId)
        if (local == null && connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                supabaseDataSource.getUserByGoogleId(googleId)?.also { remote ->
                    userDao.upsertUser(remote)
                    return@withContext remote
                }
            } catch (e: Exception) { }
        }
        local
    }

    // ... (resto de métodos omitidos para brevedad, se mantienen igual)
    
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
        }
        .onStart { emit(emptyList()) }
        .flowOn(Dispatchers.IO)

    fun getNotificationsForUser(userId: Int): Flow<List<NotificationEntity>> {
        // Realtime con reintento y backoff: hasta 3 reintentos con delay 1s, 2s, 4s si el canal falla; polling 3s como respaldo
        var retryAttempts = 0
        val realtimeFlow = supabaseDataSource.subscribeToNotifications(userId)
            .map { Unit }
            .catch { e ->
                Log.w("UserRepository", "Realtime notificaciones: ${e.message}")
                emit(Unit)
            }
            .retry { e ->
                if (retryAttempts >= 3) return@retry false
                retryAttempts++
                val delayMs = 1000L * (1 shl (retryAttempts - 1).coerceAtMost(2))
                Log.d("UserRepository", "Realtime reintento en ${delayMs}ms (intento $retryAttempts)")
                delay(delayMs)
                true
            }
        val pollingFlow = flow {
            while (true) {
                emit(Unit)
                delay(3_000)
            }
        }
        return merge(_refreshTrigger, realtimeFlow, pollingFlow)
            .flatMapLatest {
                flow {
                    val notifications = if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
                        try {
                            supabaseDataSource.getNotificationsForUser(userId)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } else emptyList()
                    emit(notifications)
                }
            }.flowOn(Dispatchers.IO)
    }

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


    suspend fun restoreSessionFromSupabaseIfNeeded() = withContext(Dispatchers.IO) {
        val activeSession = sessionDao.getActiveSession().first()
        if (activeSession != null) return@withContext

        val authUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return@withContext
        val remoteUser = getUserByGoogleId(authUserId) ?: return@withContext
        sessionDao.setSession(ActiveSessionEntity(userId = remoteUser.id))
    }

    suspend fun setSession(userId: Int) {
        sessionDao.setSession(ActiveSessionEntity(userId = userId))
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

    suspend fun getUserByUsername(username: String): UserEntity? {
        val local = userDao.getUserByUsername(username)
            ?: userDao.getAllUsers().first().firstOrNull { it.username.equals(username, ignoreCase = true) }
        if (local == null && connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                val remote = supabaseDataSource.getUserByUsername(username)
                    ?: supabaseDataSource.getUserByUsernameInsensitive(username)
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


    suspend fun touchUserInteraction() {
        val currentUser = getActiveUser() ?: return
        if (connectivityObserver.observe().first() != ConnectivityObserver.Status.Available) return

        try {
            supabaseDataSource.touchUserLastInteraction(currentUser.id)
        } catch (e: UnauthorizedRestException) {
            Log.w("UserRepository", "touchUserInteraction unauthorized, retrying after auth refresh", e)
            refreshAuthSession()
            supabaseDataSource.touchUserLastInteraction(currentUser.id)
        } catch (e: Exception) {
            Log.e("UserRepository", "touchUserInteraction failed", e)
        }
    }

    suspend fun updateFcmToken(token: String) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) return
        savePendingFcmToken(cleanToken)
        syncPendingFcmTokenIfAny()
    }

    suspend fun syncPendingFcmTokenIfAny() {
        val pendingToken = getPendingFcmToken() ?: return
        val currentUser = getActiveUser() ?: run {
            Log.d("UserRepository", "FCM token pending; no active user yet")
            return
        }
        if (connectivityObserver.observe().first() != ConnectivityObserver.Status.Available) {
            Log.d("UserRepository", "FCM token pending; offline")
            return
        }

        try {
            supabaseDataSource.insertUserToken(
                UserTokenEntity(userId = currentUser.id, fcmToken = pendingToken)
            )
            clearPendingFcmToken(pendingToken)
            Log.d("UserRepository", "FCM token synced for userId=${currentUser.id}")
        } catch (e: UnauthorizedRestException) {
            Log.w("UserRepository", "updateFcmToken unauthorized, trying one auth refresh and retry", e)
            try {
                refreshAuthSession()
                supabaseDataSource.insertUserToken(
                    UserTokenEntity(userId = currentUser.id, fcmToken = pendingToken)
                )
                clearPendingFcmToken(pendingToken)
                Log.d("UserRepository", "FCM token synced after auth refresh for userId=${currentUser.id}")
            } catch (retryError: Exception) {
                if (retryError is IllegalStateException && retryError.message?.contains("refresh token") == true) {
                    Log.w("UserRepository", "updateFcmToken skipped: session has no refresh token (FCM sync will retry after next sign-in)")
                } else {
                    Log.e("UserRepository", "updateFcmToken failed after auth refresh", retryError)
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "updateFcmToken failed", e)
        }
    }

    suspend fun toggleFollow(followerId: Int, targetId: Int) = withContext(Dispatchers.IO) {
        val follow = FollowEntity(followerId, targetId)
        val isFollowing = userDao.getAllFollows().first().any { it.followerId == followerId && it.followedId == targetId }

        if (isFollowing) userDao.deleteFollow(follow) else userDao.insertFollow(follow)

        if (connectivityObserver.observe().first() != ConnectivityObserver.Status.Available) return@withContext

        val remoteCall: suspend () -> Unit = {
            if (isFollowing) {
                supabaseDataSource.deleteFollow(followerId, targetId)
            } else {
                supabaseDataSource.insertFollow(follow)
                if (followerId != targetId) {
                    val followerUser = userDao.getUserById(followerId)
                    val fromUsername = followerUser?.username
                    if (!fromUsername.isNullOrBlank()) {
                        try {
                            supabaseDataSource.insertNotification(
                                NotificationEntity(
                                    userId = targetId,
                                    type = "FOLLOW",
                                    fromUsername = fromUsername,
                                    message = "ha empezado a seguirte",
                                    timestamp = System.currentTimeMillis(),
                                    relatedId = followerId.toString()
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("UserRepository", "Follow persisted but follow-notification failed", e)
                        }
                    }
                }
            }
        }

        try {
            remoteCall()
        } catch (e: UnauthorizedRestException) {
            Log.w("UserRepository", "toggleFollow unauthorized, trying one auth refresh and retry", e)
            refreshAuthSession()
            try {
                remoteCall()
            } catch (retryError: Exception) {
                if (isFollowing) userDao.insertFollow(follow) else userDao.deleteFollow(follow)
                triggerRefresh()
                Log.e("UserRepository", "toggleFollow failed after auth refresh", retryError)
                throw retryError
            }
        } catch (e: Exception) {
            if (isFollowing) userDao.insertFollow(follow) else userDao.deleteFollow(follow)
            triggerRefresh()
            Log.e("UserRepository", "toggleFollow remote sync failed", e)
            throw e
        }
    }

    suspend fun syncUsers() {
        val users = supabaseDataSource.getAllUsers()
        userDao.insertUsers(users)
        triggerRefresh()
    }

    suspend fun syncFollows() {
        val follows = getAllFollowsWithAuthRetry()
        userDao.insertFollows(follows)
        triggerRefresh()
    }

    /** IDs de usuarios que sigue el usuario dado; lee del DAO (estado actual tras sync). */
    suspend fun getFollowingIdsForUser(userId: Int): Set<Int> {
        val follows = userDao.getAllFollows().first()
        return follows.filter { it.followerId == userId }.map { it.followedId }.toSet()
    }

    private suspend fun getAllFollowsWithAuthRetry(): List<FollowEntity> {
        return try {
            supabaseDataSource.getAllFollows()
        } catch (e: UnauthorizedRestException) {
            Log.w("UserRepository", "getAllFollows unauthorized, trying one auth refresh and retry", e)
            refreshAuthSession()
            supabaseDataSource.getAllFollows()
        }
    }

    private suspend fun refreshAuthSession() {
        try {
            supabaseClient.auth.refreshCurrentSession()
        } catch (e: IllegalStateException) {
            // Sesión sin refresh token (p. ej. login con Google ID token); no se puede refrescar.
            Log.w("UserRepository", "Auth refresh not possible: ${e.message}. Session may need re-login for some operations.", e)
            throw e
        } catch (e: Exception) {
            Log.e("UserRepository", "Auth refresh failed", e)
            throw e
        }
    }

    suspend fun markNotificationRead(notificationId: Int) {
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                supabaseDataSource.markNotificationRead(notificationId)
            } catch (_: Exception) {
            }
        }
    }

    suspend fun markAllNotificationsRead(userId: Int) {
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                supabaseDataSource.markAllNotificationsRead(userId)
            } catch (_: Exception) {
            }
        }
    }

    suspend fun deleteNotification(notificationId: Int) {
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                supabaseDataSource.deleteNotification(notificationId)
            } catch (_: Exception) {
            }
        }
    }

    suspend fun insertUsers(users: List<UserEntity>) {
        userDao.insertUsers(users)
    }

    private fun savePendingFcmToken(token: String) {
        synchronized(prefsLock) {
            sharedPreferences.edit().putString(KEY_PENDING_FCM_TOKEN, token).apply()
        }
    }

    private fun getPendingFcmToken(): String? {
        return synchronized(prefsLock) {
            sharedPreferences.getString(KEY_PENDING_FCM_TOKEN, null)?.takeIf { it.isNotBlank() }
        }
    }

    private fun clearPendingFcmToken(expectedToken: String) {
        synchronized(prefsLock) {
            val current = sharedPreferences.getString(KEY_PENDING_FCM_TOKEN, null)
            if (current == expectedToken) {
                sharedPreferences.edit().remove(KEY_PENDING_FCM_TOKEN).apply()
            }
        }
    }

    private companion object {
        const val KEY_PENDING_FCM_TOKEN = "pending_fcm_token"
        const val ACCOUNT_DELETION_GRACE_MS = 30L * 24L * 60L * 60L * 1000L
    }
}
