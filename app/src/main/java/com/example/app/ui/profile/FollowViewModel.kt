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
    
    // Exponemos el usuario activo para la UI
    val activeUser = userRepository.getActiveUserFlow()
    
    // Obtenemos dinámicamente el ID del usuario activo
    val myFollowingIds = combine(
        activeUser,
        userRepository.followingMap
    ) { activeUser, map ->
        activeUser?.let { map[it.id] } ?: emptySet()
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
                    id = entityIdToUserId(userEntity.id), // Just map the id directly if they match
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
    
    private fun entityIdToUserId(id: Int) = id // Assuming they are the same for now

    fun toggleFollow(targetId: Int) {
        viewModelScope.launch {
            val me = userRepository.getActiveUser() ?: return@launch
            userRepository.toggleFollow(me.id, targetId)
        }
    }
}
