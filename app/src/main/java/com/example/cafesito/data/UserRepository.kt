package com.example.cafesito.data

import com.example.cafesito.domain.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val supabaseDataSource: SupabaseDataSource
) {
    val followingMap: Flow<Map<Int, Set<Int>>> = userDao.getAllFollows().map { follows ->
        follows.groupBy { it.followerId }
            .mapValues { entry -> entry.value.map { it.followedId }.toSet() }
    }

    fun getAllUsersFlow(): Flow<List<UserEntity>> = userDao.getAllUsers()
    fun getActiveUserFlow(): Flow<UserEntity?> = userDao.getActiveUserFlow()
    suspend fun getActiveUser(): UserEntity? = userDao.getActiveUserSync()

    suspend fun getUserById(userId: Int): UserEntity? {
        val local = userDao.getUserById(userId)
        if (local != null) return local
        return supabaseDataSource.getUserById(userId)?.also { userDao.upsertUser(it) }
    }

    suspend fun getUserByUsername(username: String): UserEntity? = supabaseDataSource.getUserByUsername(username)

    suspend fun upsertUser(user: UserEntity) {
        supabaseDataSource.upsertUser(user)
        userDao.upsertUser(user)
    }

    suspend fun isFollowing(followerId: Int, targetId: Int): Boolean {
        val follows = userDao.getAllFollowsList()
        return follows.any { it.followerId == followerId && it.followedId == targetId }
    }

    suspend fun toggleFollow(followerId: Int, targetId: Int) {
        val currentlyFollowing = isFollowing(followerId, targetId)
        val currentUser = getActiveUser() ?: return

        if (currentlyFollowing) {
            supabaseDataSource.deleteFollow(followerId, targetId)
            userDao.deleteFollow(FollowEntity(followerId, targetId))
        } else {
            val follow = FollowEntity(followerId, targetId)
            supabaseDataSource.insertFollow(follow)
            userDao.insertFollow(follow)

            // NOTIFICACIÓN: Alguien te sigue
            val notification = NotificationEntity(
                userId = targetId,
                type = "FOLLOW",
                fromUsername = currentUser.username,
                message = "ha empezado a seguirte.",
                timestamp = System.currentTimeMillis(),
                relatedId = currentUser.id.toString()
            )
            supabaseDataSource.insertNotification(notification)
        }
    }
    
    suspend fun syncUsers() {
        val remoteUsers = supabaseDataSource.getAllUsers()
        userDao.insertUsers(remoteUsers)
    }

    suspend fun syncFollows() {
        val remoteFollows = supabaseDataSource.getAllFollows()
        remoteFollows.forEach { userDao.insertFollow(it) }
    }
}

fun UserEntity.toDomain() = User(id = id, username = username, fullName = fullName, avatarUrl = avatarUrl, email = email, bio = bio)
