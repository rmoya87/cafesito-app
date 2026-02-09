package com.cafesito.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.UserRepository
import com.cafesito.shared.domain.User
import com.cafesito.shared.domain.SuggestedUserInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FollowViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val fullFollowingMap = userRepository.followingMap
    val activeUser = userRepository.getActiveUserFlow()

    val myFollowingIds = combine(
        activeUser,
        userRepository.followingMap
    ) { activeUser, map ->
        activeUser?.let { map[it.id] } ?: emptySet()
    }

    val allUsers = userRepository.getAllUsersFlow()

    val suggestedUsers: Flow<List<SuggestedUserInfo>> = combine(
        activeUser,
        userRepository.followingMap,
        allUsers
    ) { activeUser, map, dbUsers ->
        val activeUserId = activeUser?.id
        val myFollowing = activeUserId?.let { map[it] } ?: emptySet()
        val excludedIds = if (activeUserId != null) myFollowing + activeUserId else myFollowing
        val candidates = dbUsers.filter { it.id !in excludedIds }

        val relatedIds = if (myFollowing.isEmpty()) {
            emptySet()
        } else {
            myFollowing.flatMap { followedId -> map[followedId].orEmpty() }.toSet()
        }

        val friendsOfFriends = if (relatedIds.isEmpty()) {
            emptyList()
        } else {
            candidates.filter { relatedIds.contains(it.id) }
        }

        val sortedByFollowers = candidates.sortedWith(
            compareByDescending<com.cafesito.app.data.UserEntity> { user ->
                map.values.count { it.contains(user.id) }
            }.thenBy { it.username }
        )

        val selection = if (myFollowing.isEmpty()) {
            sortedByFollowers
        } else {
            friendsOfFriends.ifEmpty { sortedByFollowers }
        }

        selection.take(20).map { userEntity ->
            SuggestedUserInfo(
                user = User(
                    id = userEntity.id,
                    username = userEntity.username,
                    fullName = userEntity.fullName,
                    avatarUrl = userEntity.avatarUrl,
                    email = userEntity.email,
                    bio = userEntity.bio
                ),
                followersCount = map.values.count { it.contains(userEntity.id) },
                followingCount = map[userEntity.id]?.size ?: 0
            )
        }
    }

    fun followersState(userId: Int): Flow<List<SuggestedUserInfo>> = combine(
        userRepository.followingMap,
        userRepository.getAllUsersFlow()
    ) { map, dbUsers ->
        val followerIds = map.filter { it.value.contains(userId) }.keys
        dbUsers.filter { followerIds.contains(it.id) }.map { userEntity ->
            SuggestedUserInfo(
                user = User(
                    id = userEntity.id,
                    username = userEntity.username,
                    fullName = userEntity.fullName,
                    avatarUrl = userEntity.avatarUrl,
                    email = userEntity.email,
                    bio = userEntity.bio
                ),
                followersCount = map.values.count { it.contains(userEntity.id) },
                followingCount = map[userEntity.id]?.size ?: 0
            )
        }
    }

    fun followingState(userId: Int): Flow<List<SuggestedUserInfo>> = combine(
        userRepository.followingMap,
        userRepository.getAllUsersFlow()
    ) { map, dbUsers ->
        val followingIds = map[userId] ?: emptySet()
        dbUsers.filter { followingIds.contains(it.id) }.map { userEntity ->
            SuggestedUserInfo(
                user = User(
                    id = userEntity.id,
                    username = userEntity.username,
                    fullName = userEntity.fullName,
                    avatarUrl = userEntity.avatarUrl,
                    email = userEntity.email,
                    bio = userEntity.bio
                ),
                followersCount = map.values.count { it.contains(userEntity.id) },
                followingCount = map[userEntity.id]?.size ?: 0
            )
        }
    }

    fun loadInitialIfNeeded() {
        userRepository.triggerRefresh()
    }

    fun toggleFollow(targetId: Int) {
        viewModelScope.launch {
            val me = userRepository.getActiveUser() ?: return@launch
            userRepository.toggleFollow(me.id, targetId)
        }
    }
}
