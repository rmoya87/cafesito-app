package com.example.cafesito.data

import android.util.Log
import com.example.cafesito.ui.utils.ConnectivityObserver
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

class NoConnectivityException(message: String) : IOException(message)

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val supabaseClient: SupabaseClient,
    private val connectivityObserver: ConnectivityObserver
) {
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    
    // ✅ OPTIMIZACIÓN: Caché de usuarios en memoria
    private var _usersCache: List<UserEntity>? = null
    private var _lastUsersCacheTime: Long = 0
    private val USERS_CACHE_DURATION = 5 * 60 * 1000L // 5 minutos

    private suspend fun ensureConnected() {
        if (connectivityObserver.observe().first() != ConnectivityObserver.Status.Available) {
            throw NoConnectivityException("No hay conexión a internet.")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val followingMap: Flow<Map<Int, Set<Int>>> = _refreshTrigger.flatMapLatest {
        flow {
            try {
                ensureConnected()
                val follows = supabaseDataSource.getAllFollows()
                val map = follows.groupBy { it.followerId }
                    .mapValues { entry -> entry.value.map { it.followedId }.toSet() }
                emit(map)
            } catch (e: Exception) {
                emit(emptyMap())
            }
        }
    }

    fun triggerRefresh() {
        // ✅ Invalidar caché al refrescar
        _usersCache = null
        _lastUsersCacheTime = 0
        _refreshTrigger.tryEmit(Unit)
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
            e.printStackTrace()
            null
        }
    }

    suspend fun logout() {
        try {
            ensureConnected()
            supabaseClient.auth.signOut()
            userDao.deleteAllUsers()
            // ✅ Limpiar caché al logout
            _usersCache = null
            _lastUsersCacheTime = 0
        } catch (e: Exception) {
            Log.e("UserRepository", "Logout failed, local session preserved", e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllUsersFlow(): Flow<List<UserEntity>> = _refreshTrigger.flatMapLatest {
        flow {
            try {
                ensureConnected()
                emit(supabaseDataSource.getAllUsers())
            } catch (e: Exception) {
                emit(emptyList())
            }
        }
    }

    /**
     * ✅ OPTIMIZACIÓN: Caché con TTL para usuarios
     * Evita descargar 1000+ usuarios en cada pantalla
     */
    suspend fun getAllUsersList(): List<UserEntity> {
        val now = System.currentTimeMillis()
        
        // Retornar caché si es reciente
        if (_usersCache != null && (now - _lastUsersCacheTime) < USERS_CACHE_DURATION) {
            Log.d("UserRepository", "Usando caché de usuarios (${_usersCache!!.size} usuarios)")
            return _usersCache!!
        }
        
        ensureConnected()
        return try {
            val users = supabaseDataSource.getAllUsers()
            _usersCache = users
            _lastUsersCacheTime = now
            Log.d("UserRepository", "Usuarios descargados y cacheados (${users.size})")
            users
        } catch (e: Exception) {
            Log.e("UserRepository", "Error descargando usuarios, usando caché antigua")
            _usersCache ?: emptyList()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getActiveUserFlow(): Flow<UserEntity?> {
        return combine(supabaseClient.auth.sessionStatus, _refreshTrigger) { _, _ -> }
            .flatMapLatest {
                val uid = supabaseClient.auth.currentUserOrNull()?.id
                if (uid != null) {
                    flow {
                        try {
                            ensureConnected()
                            emit(supabaseDataSource.getUserByGoogleId(uid))
                        } catch (e: NoConnectivityException) {
                            emit(userDao.getUserByGoogleId(uid))
                        }
                    }
                } else {
                    flowOf(null)
                }
            }
    }

    suspend fun getActiveUser(): UserEntity? {
        ensureConnected()
        val currentUid = supabaseClient.auth.currentUserOrNull()?.id ?: return null
        return try {
            supabaseDataSource.getUserByGoogleId(currentUid)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserById(userId: Int): UserEntity? {
        ensureConnected()
        return try {
            supabaseDataSource.getUserById(userId)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserByGoogleId(googleId: String): UserEntity? {
        ensureConnected()
        return try {
            supabaseDataSource.getUserByGoogleId(googleId)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserByUsername(username: String): UserEntity? {
        ensureConnected()
        return try {
            supabaseDataSource.getUserByUsername(username)
        } catch (e: Exception) {
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
        ensureConnected()
        return try {
            val follows = supabaseDataSource.getAllFollows()
            follows.any { it.followerId == followerId && it.followedId == targetId }
        } catch (e: Exception) {
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
                    } catch (e: Exception) { }
                }
            }
            triggerRefresh()
        } catch (e: Exception) {
            Log.e("USER_REPO", "Error en toggleFollow: ${e.message}")
        }
    }

    suspend fun syncUsers() {
        triggerRefresh()
    }

    suspend fun syncFollows() {
        triggerRefresh()
    }
}
