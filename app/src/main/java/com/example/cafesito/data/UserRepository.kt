package com.example.cafesito.data

import com.example.cafesito.domain.User
import com.example.cafesito.domain.currentUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    // Mapa reactivo: ID de Usuario -> Set de IDs a los que sigue
    val followingMap: Flow<Map<Int, Set<Int>>> = userDao.getAllFollows().map { follows ->
        follows.groupBy { it.followerId }
            .mapValues { entry -> entry.value.map { it.followedId }.toSet() }
    }

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

// Mappers
fun UserEntity.toDomain() = User(
    id = id,
    username = username,
    fullName = fullName,
    avatarUrl = avatarUrl,
    email = email,
    bio = bio
)

fun User.toEntity() = UserEntity(
    id = id,
    username = username,
    fullName = fullName,
    avatarUrl = avatarUrl,
    email = email,
    bio = bio
)
