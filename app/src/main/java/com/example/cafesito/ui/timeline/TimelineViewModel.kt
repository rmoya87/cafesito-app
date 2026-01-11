package com.example.cafesito.ui.timeline

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

    private val _internalStates = combine(_pendingRemovalIds, _refreshTrigger) { pending, refresh -> 
        pending to refresh 
    }

    val uiState: StateFlow<TimelineUiState> = combine(
        userRepository.followingMap,
        coffeeRepository.allCoffees,
        coffeeRepository.allReviews,
        coffeeRepository.favorites,
        _internalStates
    ) { followingMap, allCoffees, reviews, favorites, internal ->
        val (pendingRemovals, _) = internal
        
        val myFollowing = followingMap[currentUser.id] ?: emptySet()
        val timelineUserIds = myFollowing + currentUser.id
        
        val filteredPosts = samplePosts
            .filter { timelineUserIds.contains(it.user.id) }
            .map { TimelineItem.PostItem(it) }

        val filteredReviews = reviews
            .filter { timelineUserIds.contains(it.userId) }
            .mapNotNull { review ->
                allCoffees.find { it.coffee.id == review.coffeeId }?.let { coffeeDetails ->
                    val author = allUsers.find { it.id == review.userId }
                    TimelineItem.ReviewItem(
                        UserReviewInfo(
                            coffeeDetails = coffeeDetails, 
                            review = review,
                            authorName = author?.fullName
                        )
                    )
                }
            }
            
        val favoriteItems = favorites
            .filter { fav -> timelineUserIds.contains(currentUser.id) } // Only show my own favorites for now or followed ones if available in entity
            .mapNotNull { fav ->
                allCoffees.find { it.coffee.id == fav.coffeeId }?.let { details ->
                    TimelineItem.FavoriteActionItem(details, fav.savedAt)
                }
            }

        val combinedItems = (filteredPosts + filteredReviews + favoriteItems)
            .sortedByDescending { it.timestamp }

        val filterOutFromSuggestions = myFollowing + currentUser.id
        val suggestedUsers = allUsers
            .filter { !filterOutFromSuggestions.contains(it.id) }
            .map { user ->
                val myFavIds = currentUser.favoriteCoffeeIds.toSet()
                val sharedCount = user.favoriteCoffeeIds.intersect(myFavIds).size
                user to sharedCount
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
                delay(800)
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
    
    data class FavoriteActionItem(val coffeeDetails: CoffeeWithDetails, override val timestamp: Long) : TimelineItem()
}

sealed interface TimelineUiState {
    data object Loading : TimelineUiState
    data class Success(
        val items: List<TimelineItem>,
        val suggestedUsers: List<SuggestedUserInfo>,
        val myFollowingIds: Set<Int>
    ) : TimelineUiState
}
