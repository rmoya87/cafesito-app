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
        socialRepository.getAllReviewsWithAuthor(),
        diaryRepository.getPantryItems()
    ) { activeUser: UserEntity?, coffee: CoffeeWithDetails?, allReviews: List<ReviewWithAuthor>, pantryItems: List<PantryItemWithDetails> ->
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

        val pantryDetails = pantryItems.find { it.coffee.id == coffeeId }

        DetailUiState.Success(
            coffee = coffee,
            reviews = reviewsForThisCoffee,
            userReview = userReview,
            isCustom = pantryDetails?.isCustom ?: false,
            currentPantryItem = pantryDetails?.pantryItem
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState.Loading)

    fun toggleFavorite(isFavorite: Boolean) {
        viewModelScope.launch {
            coffeeRepository.toggleFavorite(coffeeId, isFavorite)
        }
    }

    fun updateStock(total: Int, remaining: Int, name: String? = null, brand: String? = null) {
        viewModelScope.launch {
            if (name != null && brand != null) {
                // Actualizar café personalizado
                val currentState = uiState.value as? DetailUiState.Success
                diaryRepository.updateCustomCoffee(
                    id = coffeeId,
                    name = name,
                    brand = brand,
                    specialty = currentState?.coffee?.coffee?.especialidad ?: "Arabica",
                    roast = currentState?.coffee?.coffee?.tueste,
                    variety = currentState?.coffee?.coffee?.variedadTipo,
                    country = currentState?.coffee?.coffee?.paisOrigen ?: "España",
                    hasCaffeine = currentState?.coffee?.coffee?.cafeina == "Sí",
                    format = currentState?.coffee?.coffee?.formato ?: "Grano",
                    imageBytes = null, // Mantener imagen actual
                    totalGrams = total
                )
            }
            // Siempre actualizamos el stock físico en la despensa
            diaryRepository.updatePantryStockFull(coffeeId, total, remaining)
        }
    }

    fun submitReview(rating: Float, comment: String, imageUri: Uri? = null) {
        viewModelScope.launch {
            val user = userRepository.getActiveUser() ?: return@launch
            
            var uploadedImageUrl: String? = null
            
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
        val userReview: ReviewEntity?,
        val isCustom: Boolean,
        val currentPantryItem: PantryItemEntity?
    ) : DetailUiState
    data class Error(val message: String) : DetailUiState
}
