package com.cafesito.app.ui.detail

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.*
import com.cafesito.shared.domain.Review
import com.cafesito.shared.domain.User
import com.cafesito.shared.domain.repository.ReviewRepository
import com.cafesito.shared.domain.validation.ValidateReviewInputUseCase
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
    private val reviewRepository: ReviewRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val validateReviewInput = ValidateReviewInputUseCase()

    private val coffeeId: String = checkNotNull(savedStateHandle["coffeeId"])

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DetailUiState> = combine(
        combine(
            userRepository.getActiveUserFlow(),
            coffeeRepository.getCoffeeWithDetailsById(coffeeId),
            socialRepository.getReviewsForCoffee(coffeeId),
            socialRepository.getSensoryProfilesForCoffee(coffeeId),
            diaryRepository.getPantryItems()
        ) { activeUser, coffee, allReviews, sensoryProfiles, pantryItems ->
            PentaState(activeUser, coffee, allReviews, sensoryProfiles, pantryItems)
        },
        coffeeRepository.favorites
    ) { penta, favorites ->
        val activeUser = penta.activeUser
        val coffee = penta.coffee
        val allReviews = penta.allReviews
        val sensoryProfiles = penta.sensoryProfiles
        val pantryItems = penta.pantryItems

        if (coffee == null) return@combine DetailUiState.Error("Café no encontrado")

        val isFavoriteLocally = favorites.any { it.coffeeId == coffeeId && it.userId == activeUser?.id }
        val updatedCoffee = coffee.copy(favorite = if (isFavoriteLocally) LocalFavorite(coffeeId, activeUser?.id ?: 0) else null)

        val reviewsForThisCoffee = allReviews.map { reviewWithAuthor ->
            UserReviewInfo(
                coffeeDetails = updatedCoffee,
                review = reviewWithAuthor.review,
                authorName = reviewWithAuthor.author?.fullName,
                authorAvatarUrl = reviewWithAuthor.author?.avatarUrl
            )
        }

        val userReview = allReviews.find { it.review.userId == activeUser?.id }?.review


        val sensoryByUsers = sensoryProfiles.distinctBy { it.userId }
        val sensoryEditorsCount = sensoryByUsers.size
        fun avgIncludingBase(selector: (CoffeeSensoryProfileEntity) -> Float, baseValue: Float): Float {
            val values = listOf(baseValue.toDouble()) + sensoryByUsers.map { selector(it).toDouble() }
            return values.average().toFloat()
        }
        val sensoryAverages = mapOf(
            "Aroma" to avgIncludingBase({ it.aroma }, coffee.coffee.aroma),
            "Sabor" to avgIncludingBase({ it.sabor }, coffee.coffee.sabor),
            "Cuerpo" to avgIncludingBase({ it.cuerpo }, coffee.coffee.cuerpo),
            "Acidez" to avgIncludingBase({ it.acidez }, coffee.coffee.acidez),
            "Dulzura" to avgIncludingBase({ it.dulzura }, coffee.coffee.dulzura)
        )

        val pantryDetails = pantryItems.find { it.coffee.id == coffeeId }

        DetailUiState.Success(
            coffee = updatedCoffee,
            reviews = reviewsForThisCoffee,
            userReview = userReview,
            isCustom = coffee.coffee.isCustom,
            currentPantryItem = pantryDetails?.pantryItem,
            sensoryAverages = sensoryAverages,
            sensoryEditorsCount = sensoryEditorsCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState.Loading)

    private data class PentaState(
        val activeUser: UserEntity?,
        val coffee: CoffeeWithDetails?,
        val allReviews: List<ReviewWithAuthor>,
        val sensoryProfiles: List<CoffeeSensoryProfileEntity>,
        val pantryItems: List<PantryItemWithDetails>
    )

    fun loadInitialIfNeeded() {
        coffeeRepository.triggerRefresh()
        socialRepository.triggerRefresh()
    }

    fun toggleFavorite(shouldBeFavorite: Boolean) {
        viewModelScope.launch {
            coffeeRepository.toggleFavorite(coffeeId, shouldBeFavorite)
        }
    }

    fun updateStock(total: Int, remaining: Int, name: String? = null, brand: String? = null) {
        viewModelScope.launch {
            val currentState = uiState.value as? DetailUiState.Success
            if (name != null && brand != null) {
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
                    imageBytes = null,
                    totalGrams = total
                )
            }
            diaryRepository.updatePantryStockFull(coffeeId, total, remaining)
        }
    }


    fun submitSensoryProfile(aroma: Float, sabor: Float, cuerpo: Float, acidez: Float, dulzura: Float) {
        viewModelScope.launch {
            val user = userRepository.getActiveUser() ?: return@launch
            socialRepository.upsertSensoryProfile(
                CoffeeSensoryProfileEntity(
                    coffeeId = coffeeId,
                    userId = user.id,
                    aroma = aroma.coerceIn(0f, 10f),
                    sabor = sabor.coerceIn(0f, 10f),
                    cuerpo = cuerpo.coerceIn(0f, 10f),
                    acidez = acidez.coerceIn(0f, 10f),
                    dulzura = dulzura.coerceIn(0f, 10f),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun submitReview(rating: Float, comment: String, imageUri: Uri? = null) {
        viewModelScope.launch {
            val validation = validateReviewInput(rating, comment)
            if (validation.isFailure) return@launch
            val user = userRepository.getActiveUser() ?: return@launch
            val currentState = uiState.value as? DetailUiState.Success
            val existingReview = currentState?.userReview
            
            var uploadedImageUrl: String? = null
            
            // 1. Subida de imagen si se selecciona una nueva
            imageUri?.let { uri ->
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val fileName = "review_${user.id}_${System.currentTimeMillis()}.jpg"
                        uploadedImageUrl = socialRepository.uploadImage("reviews", fileName, bytes)
                    }
                } catch (e: Exception) {
                    Log.e("DETAIL_VM", "Error subiendo imagen: ${e.message}")
                }
            }

            // 2. Construcción de la entidad incluyendo el ID existente para evitar duplicados
            val review = Review(
                id = existingReview?.id,
                user = user.toDomainUser(),
                coffeeId = coffeeId,
                rating = rating,
                comment = comment,
                imageUrl = uploadedImageUrl ?: existingReview?.imageUrl,
                timestamp = System.currentTimeMillis()
            )

            try {
                val result = reviewRepository.updateReview(review)
                if (result.isFailure) {
                    Log.e("DETAIL_VM", "Error al guardar reseña: ${result.exceptionOrNull()?.message}")
                    return@launch
                }
                // Es crucial refrescar AMBOS repositorios para que la UI se actualice al instante
                socialRepository.triggerRefresh()
                coffeeRepository.triggerRefresh()
            } catch (e: Exception) {
                Log.e("DETAIL_VM", "Error fatal al guardar reseña: ${e.message}")
            }
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
        val currentPantryItem: PantryItemEntity?,
        val sensoryAverages: Map<String, Float>,
        val sensoryEditorsCount: Int
    ) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

private fun UserEntity.toDomainUser(): User = User(
    id = id,
    username = username,
    fullName = fullName,
    avatarUrl = avatarUrl,
    email = email,
    bio = bio
)
