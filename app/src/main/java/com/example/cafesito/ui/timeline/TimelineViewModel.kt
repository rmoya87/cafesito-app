package com.example.cafesito.ui.timeline

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.CoffeeRepository
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.data.UserRepository
import com.example.cafesito.domain.*
import com.example.cafesito.ui.profile.UserReviewInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val coffeeRepository: CoffeeRepository
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(0)
    private val _pendingRemovalIds = MutableStateFlow<Set<Int>>(emptySet())

    val uiState: StateFlow<TimelineUiState> = combine(
        userRepository.followingMap,
        coffeeRepository.allCoffees,
        snapshotFlow { sampleReviews.value },
        _pendingRemovalIds,
        _refreshTrigger
    ) { followingMap, allCoffees, reviews, pendingRemovals, _ ->
        val myFollowing = followingMap[currentUser.id] ?: emptySet()
        
        // Para el carrusel, no filtramos todavía a los que acabamos de seguir (pendingRemovals)
        val filterOutFromSuggestions = myFollowing - pendingRemovals
        
        // Filtrar publicaciones de personas que sigo
        val filteredPosts = samplePosts.filter { myFollowing.contains(it.user.id) }
            .map { TimelineItem.PostItem(it) }

        // Filtrar opiniones de personas que sigo y mapear a info de café
        val filteredReviews = reviews.filter { myFollowing.contains(it.user.id) }
            .mapNotNull { review ->
                allCoffees.find { it.coffee.id == review.coffeeId }?.let { coffeeDetails ->
                    TimelineItem.ReviewItem(UserReviewInfo(coffeeDetails, review))
                }
            }

        // Mezclar y ordenar por fecha descendente
        val combinedItems = (filteredPosts + filteredReviews).sortedByDescending { it.timestamp }

        // Lógica de Sugerencias Proactiva con Stats
        val myFavs = currentUser.favoriteCoffeeIds.toSet()
        val suggestedUsers = allUsers
            .filter { it.id != currentUser.id && !filterOutFromSuggestions.contains(it.id) }
            .map { user ->
                val sharedCoffees = user.favoriteCoffeeIds.intersect(myFavs).size
                val mutuals = followingMap.filter { (followerId, followedSet) ->
                    myFollowing.contains(followerId) && followedSet.contains(user.id)
                }.size
                val score = (sharedCoffees * 2) + (mutuals * 3)
                user to score
            }
            .sortedByDescending { it.second }
            .map { (user, _) -> 
                SuggestedUserInfo(
                    user = user,
                    followersCount = followingMap.values.count { it.contains(user.id) },
                    followingCount = followingMap[user.id]?.size ?: 0
                )
            }
            .take(10)

        TimelineUiState.Success(
            items = combinedItems,
            suggestedUsers = suggestedUsers,
            myFollowingIds = myFollowing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimelineUiState.Loading
    )

    fun onAddComment(post: Post, text: String) {
        val postIndex = samplePosts.indexOfFirst { it.id == post.id }
        if (postIndex != -1) {
            val updatedComments = post.comments.toMutableList().apply { add(Comment(currentUser, text)) }
            val updatedPost = post.copy(comments = updatedComments)
            samplePosts[postIndex] = updatedPost
            _refreshTrigger.value++
        }
    }

    fun toggleFollowSuggestion(userId: Int) {
        viewModelScope.launch {
            val currentFollowingMap = userRepository.followingMap.first()
            val isCurrentlyFollowing = currentFollowingMap[currentUser.id]?.contains(userId) ?: false
            
            if (!isCurrentlyFollowing) {
                _pendingRemovalIds.update { it + userId }
                userRepository.toggleFollow(currentUser.id, userId)
                delay(1000)
                _pendingRemovalIds.update { it - userId }
                _refreshTrigger.value++
            } else {
                userRepository.toggleFollow(currentUser.id, userId)
                _refreshTrigger.value++
            }
        }
    }
}

sealed class TimelineItem {
    abstract val timestamp: Long

    data class PostItem(val post: Post) : TimelineItem() {
        override val timestamp: Long = post.timestamp
    }

    data class ReviewItem(val reviewInfo: UserReviewInfo) : TimelineItem() {
        override val timestamp: Long = reviewInfo.review.timestamp
    }
}

sealed interface TimelineUiState {
    data object Loading : TimelineUiState
    data class Success(
        val items: List<TimelineItem>,
        val suggestedUsers: List<SuggestedUserInfo>,
        val myFollowingIds: Set<Int>
    ) : TimelineUiState
}
