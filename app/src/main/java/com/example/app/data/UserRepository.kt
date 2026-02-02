package com.cafesito.app.data

import android.util.Log
import com.cafesito.app.ui.utils.ConnectivityObserver
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val supabaseClient: SupabaseClient,
    private val connectivityObserver: ConnectivityObserver
) {
    private val _refreshTrigger = MutableStateFlow(0L)
    
    // ✅ OPTIMIZACIÓN: Caché de usuarios en memoria
    private var _usersCache: List<UserEntity>? = null
    private var _lastUsersCacheTime: Long = 0
    private companion object {
        const val UsersCacheDuration = 5 * 60 * 1000L // 5 minutos
    }

    private suspend fun ensureConnected() {
        if (connectivityObserver.observe().first() != ConnectivityObserver.Status.Available) {
            throw NoConnectivityException("No hay conexión a internet.")
        }
    }

    val followingMap: Flow<Map<Int, Set<Int>>> = _refreshTrigger
        .debounce(300)
        .flatMapLatest {
            flow<Map<Int, Set<Int>>> {
                try {
                    ensureConnected()
                    val follows = supabaseDataSource.getAllFollows()
                    val map = follows.groupBy { it.followerId }
                        .mapValues { entry -> entry.value.map { it.followedId }.toSet() }
                    emit(map)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emit(emptyMap())
                }
            }
        }.flowOn(Dispatchers.IO)

    fun triggerRefresh() {
        // ✅ Invalidar caché al refrescar
        _usersCache = null
        _lastUsersCacheTime = 0
        _refreshTrigger.value++
    }

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
            // Intentar cerrar sesión en el servidor
            try {
                if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
                    supabaseClient.auth.signOut()
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Remote logout failed", e)
            }
            
            // ✅ Siempre limpiar localmente para asegurar el cierre de sesión
            userDao.deleteAllUsers()
            _usersCache = null
            _lastUsersCacheTime = 0
            
            // Forzar actualización de flujos reactivos
            triggerRefresh()
        } catch (e: Exception) {
            Log.e("UserRepository", "Logout failed", e)
        }
    }

    fun getAllUsersFlow(): Flow<List<UserEntity>> = _refreshTrigger
        .debounce(300)
        .flatMapLatest {
            flow<List<UserEntity>> {
                try {
                    ensureConnected()
                    emit(supabaseDataSource.getAllUsers())
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emit(emptyList())
                }
            }
        }.flowOn(Dispatchers.IO)

    fun getNotificationsForUser(userId: Int): Flow<List<NotificationEntity>> = _refreshTrigger
        .debounce(300)
        .flatMapLatest {
            flow<List<NotificationEntity>> {
                try {
                    ensureConnected()
                    emit(supabaseDataSource.getNotificationsForUser(userId))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emit(emptyList())
                }
            }
        }.flowOn(Dispatchers.IO)

    /**
     * ✅ OPTIMIZACIÓN: Obtener un flujo de un usuario específico por ID
     */
    fun getUserByIdFlow(userId: Int): Flow<UserEntity?> = _refreshTrigger
        .debounce(300)
        .flatMapLatest {
            flow<UserEntity?> {
                try {
                    ensureConnected()
                    emit(supabaseDataSource.getUserById(userId))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emit(null)
                }
            }
        }.flowOn(Dispatchers.IO)

    /**
     * ✅ OPTIMIZACIÓN: Caché con TTL para usuarios
     */
    suspend fun getAllUsersList(): List<UserEntity> {
        val now = System.currentTimeMillis()
        
        if (_usersCache != null && (now - _lastUsersCacheTime) < UsersCacheDuration) {
            return _usersCache!!
        }
        
        return try {
            ensureConnected()
            val users = supabaseDataSource.getAllUsers()
            _usersCache = users
            _lastUsersCacheTime = now
            users
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _usersCache ?: emptyList()
        }
    }

    fun getActiveUserFlow(): Flow<UserEntity?> {
        return combine(supabaseClient.auth.sessionStatus, _refreshTrigger) { _, _ -> }
            .debounce(100)
            .flatMapLatest {
                val uid = supabaseClient.auth.currentUserOrNull()?.id
                if (uid != null) {
                    flow<UserEntity?> {
                        try {
                            ensureConnected()
                            emit(supabaseDataSource.getUserByGoogleId(uid))
                        } catch (_: NoConnectivityException) {
                            emit(userDao.getUserByGoogleId(uid))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            emit(null)
                        }
                    }
                } else {
                    flowOf<UserEntity?>(null)
                }
            }.flowOn(Dispatchers.IO)
    }

    suspend fun getActiveUser(): UserEntity? {
        val currentUid = supabaseClient.auth.currentUserOrNull()?.id ?: return null
        return try {
            ensureConnected()
            supabaseDataSource.getUserByGoogleId(currentUid)
        } catch (_: Exception) {
            userDao.getUserByGoogleId(currentUid)
        }
    }

    suspend fun getUserById(userId: Int): UserEntity? {
        return try {
            ensureConnected()
            supabaseDataSource.getUserById(userId)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getUserByGoogleId(googleId: String): UserEntity? {
        return try {
            ensureConnected()
            supabaseDataSource.getUserByGoogleId(googleId)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getUserByUsername(username: String): UserEntity? {
        return try {
            ensureConnected()
            supabaseDataSource.getUserByUsername(username)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun upsertUser(user: UserEntity) {
        try {
            ensureConnected()
            supabaseDataSource.upsertUser(user)
            triggerRefresh()
        } catch (e: Exception) {
            Log.e("USER_REPO", "Error upserting user: ${e.message}")
        }
    }

    suspend fun upsertLocalOnly(user: UserEntity) {
        userDao.upsertUser(user)
    }

    suspend fun isFollowing(followerId: Int, targetId: Int): Boolean {
        return try {
            ensureConnected()
            val follows = supabaseDataSource.getAllFollows()
            follows.any { it.followerId == followerId && it.followedId == targetId }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun toggleFollow(followerId: Int, targetId: Int) {
        try {
            ensureConnected()
            val currentlyFollowing = isFollowing(followerId, targetId)
            if (currentlyFollowing) {
                supabaseDataSource.deleteFollow(followerId, targetId)
            } else {
                supabaseDataSource.insertFollow(FollowEntity(followerId, targetId))
                getActiveUser()?.let { me ->
                    try {
                        supabaseDataSource.insertNotification(NotificationEntity(
                            userId = targetId,
                            type = "FOLLOW",
                            fromUsername = me.username,
                            message = "ha empezado a seguirte.",
                            timestamp = System.currentTimeMillis(),
                            relatedId = me.id.toString()
                        ))
                    } catch (_: Exception) { }
                }
            }
            triggerRefresh()
        } catch (e: Exception) {
            Log.e("USER_REPO", "Error en toggleFollow: ${e.message}")
        }
    }

    suspend fun markNotificationRead(notificationId: Int) {
        try {
            ensureConnected()
            supabaseDataSource.markNotificationRead(notificationId)
            triggerRefresh()
        } catch (e: Exception) {
            Log.e("USER_REPO", "Error marcando notificación como leída: ${e.message}")
        }
    }

    fun syncUsers() {
        triggerRefresh()
    }

    fun syncFollows() {
        triggerRefresh()
    }
}
