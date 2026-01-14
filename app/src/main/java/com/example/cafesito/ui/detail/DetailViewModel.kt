package com.example.cafesito.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.*
import com.example.cafesito.ui.profile.UserReviewInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val coffeeRepository: CoffeeRepository,
    private val socialRepository: SocialRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val coffeeId: String = checkNotNull(savedStateHandle["coffeeId"])

    val uiState: StateFlow<DetailUiState> = combine(
        userRepository.getActiveUserFlow(),
        coffeeRepository.getCoffeeWithDetailsById(coffeeId),
        socialRepository.getAllReviewsWithAuthor()
    ) { activeUser: UserEntity?, coffee: CoffeeWithDetails?, allReviews: List<ReviewWithAuthor> ->
        if (coffee == null) return@combine DetailUiState.Error("Café no encontrado")

        val reviewsForThisCoffee = allReviews
            .filter { it.review.coffeeId == coffeeId }
            .map { reviewWithAuthor ->
                UserReviewInfo(
                    coffeeDetails = coffee,
                    review = reviewWithAuthor.review,
                    authorName = reviewWithAuthor.author.fullName,
                    authorAvatarUrl = reviewWithAuthor.author.avatarUrl
                )
            }

        val userReview = allReviews.find { 
            it.review.coffeeId == coffeeId && it.review.userId == activeUser?.id 
        }?.review

        DetailUiState.Success(
            coffee = coffee,
            reviews = reviewsForThisCoffee,
            userReview = userReview
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState.Loading)

    fun toggleFavorite(isFavorite: Boolean) {
        viewModelScope.launch {
            coffeeRepository.toggleFavorite(coffeeId, isFavorite)
        }
    }

    fun submitReview(rating: Float, comment: String) {
        viewModelScope.launch {
            val user = userRepository.getActiveUser() ?: return@launch
            val review = ReviewEntity(
                coffeeId = coffeeId,
                userId = user.id,
                rating = rating,
                comment = comment,
                timestamp = System.currentTimeMillis()
            )
            coffeeRepository.upsertReview(review)
        }
    }
}

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(
        val coffee: CoffeeWithDetails, 
        val reviews: List<UserReviewInfo>,
        val userReview: ReviewEntity?
    ) : DetailUiState
    data class Error(val message: String) : DetailUiState
}
