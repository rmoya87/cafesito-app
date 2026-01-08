package com.example.cafesito.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.UserRepository
import com.example.cafesito.domain.User
import com.example.cafesito.domain.allUsers
import com.example.cafesito.domain.currentUser
import com.example.cafesito.domain.SuggestedUserInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FollowViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val fullFollowingMap = userRepository.followingMap
    val myFollowingIds = userRepository.followingMap.map { it[currentUser.id] ?: emptySet() }

    fun followersState(userId: Int): Flow<List<SuggestedUserInfo>> = userRepository.followingMap.map { map ->
        val followerIds = map.filter { it.value.contains(userId) }.keys
        allUsers.filter { followerIds.contains(it.id) }.map { user ->
            SuggestedUserInfo(
                user = user,
                followersCount = map.values.count { it.contains(user.id) },
                followingCount = map[user.id]?.size ?: 0
            )
        }
    }

    fun followingState(userId: Int): Flow<List<SuggestedUserInfo>> = userRepository.followingMap.map { map ->
        val followingIds = map[userId] ?: emptySet()
        allUsers.filter { followingIds.contains(it.id) }.map { user ->
            SuggestedUserInfo(
                user = user,
                followersCount = map.values.count { it.contains(user.id) },
                followingCount = map[user.id]?.size ?: 0
            )
        }
    }

    fun toggleFollow(targetId: Int) {
        viewModelScope.launch {
            userRepository.toggleFollow(currentUser.id, targetId)
        }
    }
}
