package com.example.cafesito.ui.detail

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val coffeeRepository: CoffeeRepository,
    private val socialRepository: SocialRepository,
    private val userRepository: UserRepository,
    private val diaryRepository: DiaryRepository,
    @ApplicationContext private val context: Context
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

    fun addToPantry(grams: Int = 250) {
        viewModelScope.launch {
            diaryRepository.addToPantry(coffeeId, grams)
        }
    }

    fun submitReview(rating: Float, comment: String, imageUri: Uri? = null) {
        viewModelScope.launch {
            val user = userRepository.getActiveUser() ?: return@launch
            
            var uploadedImageUrl: String? = null
            
            // 1. Si hay una imagen nueva, la subimos a Supabase Storage
            imageUri?.let { uri ->
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) {
                        val fileName = "review_${user.id}_${System.currentTimeMillis()}.jpg"
                        uploadedImageUrl = socialRepository.uploadImage("reviews", fileName, bytes)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val review = ReviewEntity(
                coffeeId = coffeeId,
                userId = user.id,
                rating = rating,
                comment = comment,
                imageUrl = uploadedImageUrl ?: (uiState.value as? DetailUiState.Success)?.userReview?.imageUrl,
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
