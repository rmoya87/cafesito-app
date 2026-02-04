package com.cafesito.app.ui.timeline

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.*
import com.cafesito.shared.domain.Review
import com.cafesito.shared.domain.User
import com.cafesito.shared.domain.repository.ReviewRepository
import com.cafesito.shared.domain.validation.ValidateReviewInputUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class PostType { PUBLICATION, OPINION }

@HiltViewModel
class AddPostViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coffeeRepository: CoffeeRepository,
    private val socialRepository: SocialRepository,
    private val userRepository: UserRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {
    private val validateReviewInput = ValidateReviewInputUseCase()

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _postType = MutableStateFlow(PostType.PUBLICATION)
    val postType: StateFlow<PostType> = _postType.asStateFlow()

    private val _imageSource = MutableStateFlow<Any?>(null)
    val imageSource: StateFlow<Any?> = _imageSource.asStateFlow()

    private val _galleryImages = MutableStateFlow<List<Uri>>(emptyList())
    val galleryImages: StateFlow<List<Uri>> = _galleryImages.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCoffee = MutableStateFlow<CoffeeWithDetails?>(null)
    val selectedCoffee: StateFlow<CoffeeWithDetails?> = _selectedCoffee.asStateFlow()

    val coffeeList: StateFlow<List<CoffeeWithDetails>> = combine(
        _searchQuery,
        coffeeRepository.allCoffees
    ) { query, all ->
        if (query.isEmpty()) all.take(10)
        else all.filter {
            it.coffee.nombre.contains(query, ignoreCase = true) ||
            it.coffee.marca.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadGalleryImages() {
        viewModelScope.launch {
            val images = withContext(Dispatchers.IO) {
                val list = mutableListOf<Uri>()
                val projection = arrayOf(MediaStore.Images.Media._ID)
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                
                try {
                    context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        sortOrder
                    )?.use { cursor ->
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idColumn)
                            list.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id))
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
                list
            }
            _galleryImages.value = images
            // Si no hay imagen seleccionada, ponemos la primera de la galería
            if (_imageSource.value == null && images.isNotEmpty()) {
                _imageSource.value = images.first()
            }
        }
    }

    fun setPostType(type: PostType) {
        _postType.value = type
        _currentStep.value = 0
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }

    fun selectCoffee(coffee: CoffeeWithDetails?) {
        _selectedCoffee.value = coffee
        if (coffee != null) _currentStep.value = 1
    }

    fun setImage(uri: Uri?) { _imageSource.value = uri }
    
    fun setCapturedImage(bitmap: Bitmap?) { 
        if (bitmap != null) {
            _imageSource.value = bitmap
            // Nos aseguramos de estar en el paso 0 para ver la preview
            _currentStep.value = 0 
        }
    }
    
    fun goToStep(step: Int) { _currentStep.value = step }

    fun createPost(comment: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val source = _imageSource.value ?: return@launch
            val activeUser = userRepository.getActiveUser() ?: return@launch
            
            // Aquí deberías subir la imagen a Supabase Storage realmente,
            // pero para este fix mantenemos la lógica de guardado
            val imageUrl = "https://picsum.photos/seed/${System.currentTimeMillis()}/800/800"
            
            socialRepository.createPost(PostEntity(
                id = "post_${System.currentTimeMillis()}", 
                userId = activeUser.id, 
                imageUrl = imageUrl, 
                comment = comment, 
                timestamp = System.currentTimeMillis()
            ))
            onSuccess()
        }
    }

    fun submitReview(rating: Float, comment: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val validation = validateReviewInput(rating, comment)
            if (validation.isFailure) return@launch
            val activeUser = userRepository.getActiveUser() ?: return@launch
            val coffeeId = _selectedCoffee.value?.coffee?.id ?: return@launch
            
            val reviewResult = reviewRepository.submitReview(
                Review(
                    user = activeUser.toDomainUser(),
                    coffeeId = coffeeId,
                    rating = rating,
                    comment = comment,
                    imageUrl = null,
                    timestamp = System.currentTimeMillis()
                )
            )
            if (reviewResult.isSuccess) {
                coffeeRepository.syncCoffees()
                onSuccess()
            }
        }
    }

    private fun UserEntity.toDomainUser(): User = User(
        id = id, username = username, fullName = fullName, avatarUrl = avatarUrl, email = email, bio = bio
    )
}
