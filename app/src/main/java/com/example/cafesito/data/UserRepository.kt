package com.example.cafesito.data

import com.example.cafesito.domain.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    val followingMap: Flow<Map<Int, Set<Int>>> = userDao.getAllFollows().map { follows ->
        follows.groupBy { it.followerId }
            .mapValues { entry -> entry.value.map { it.followedId }.toSet() }
    }

    fun getActiveUserFlow(): Flow<UserEntity?> = userDao.getActiveUserFlow()

    suspend fun getActiveUser(): UserEntity? = userDao.getActiveUserSync()

    suspend fun getUserById(userId: Int): UserEntity? = userDao.getUserById(userId)

    // FIX: Exponemos el método de guardado
    suspend fun upsertUser(user: UserEntity) = userDao.upsertUser(user)

    suspend fun isFollowing(followerId: Int, targetId: Int): Boolean {
        val follows = userDao.getAllFollowsList()
        return follows.any { it.followerId == followerId && it.followedId == targetId }
    }

    suspend fun toggleFollow(followerId: Int, targetId: Int) {
        val currentlyFollowing = isFollowing(followerId, targetId)
        if (currentlyFollowing) {
            userDao.deleteFollow(FollowEntity(followerId, targetId))
        } else {
            userDao.insertFollow(FollowEntity(followerId, targetId))
        }
    }
}

fun UserEntity.toDomain() = User(
    id = id,
    username = username,
    fullName = fullName,
    avatarUrl = avatarUrl,
    email = email,
    bio = bio
)
