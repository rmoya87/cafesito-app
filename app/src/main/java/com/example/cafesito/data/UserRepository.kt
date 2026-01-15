package com.example.cafesito.data

import android.util.Log
import com.example.cafesito.domain.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val supabaseClient: SupabaseClient
) {
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val followingMap: Flow<Map<Int, Set<Int>>> = _refreshTrigger.flatMapLatest {
        flow {
            try {
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
        _refreshTrigger.tryEmit(Unit)
    }

    suspend fun signInWithSupabase(token: String): String? {
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
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            userDao.deleteAllUsers()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllUsersFlow(): Flow<List<UserEntity>> = _refreshTrigger.flatMapLatest {
        flow {
            try {
                emit(supabaseDataSource.getAllUsers())
            } catch (e: Exception) {
                emit(emptyList())
            }
        }
    }

    suspend fun getAllUsersList(): List<UserEntity> = try {
        supabaseDataSource.getAllUsers()
    } catch (e: Exception) {
        emptyList()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getActiveUserFlow(): Flow<UserEntity?> {
        return combine(supabaseClient.auth.sessionStatus, _refreshTrigger) { _, _ -> }
            .flatMapLatest {
                val uid = supabaseClient.auth.currentUserOrNull()?.id
                if (uid != null) {
                    flow { emit(supabaseDataSource.getUserByGoogleId(uid)) }
                } else {
                    flowOf(null)
                }
            }
    }

    suspend fun getActiveUser(): UserEntity? {
        val currentUid = supabaseClient.auth.currentUserOrNull()?.id ?: return null
        return try {
            supabaseDataSource.getUserByGoogleId(currentUid)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserById(userId: Int): UserEntity? = try {
        supabaseDataSource.getUserById(userId)
    } catch (e: Exception) {
        null
    }

    suspend fun getUserByGoogleId(googleId: String): UserEntity? = try {
        supabaseDataSource.getUserByGoogleId(googleId)
    } catch (e: Exception) {
        null
    }

    suspend fun getUserByUsername(username: String): UserEntity? = try {
        supabaseDataSource.getUserByUsername(username)
    } catch (e: Exception) {
        null
    }

    suspend fun upsertUser(user: UserEntity) {
        try {
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
            val follows = supabaseDataSource.getAllFollows()
            follows.any { it.followerId == followerId && it.followedId == targetId }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleFollow(followerId: Int, targetId: Int) {
        try {
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
