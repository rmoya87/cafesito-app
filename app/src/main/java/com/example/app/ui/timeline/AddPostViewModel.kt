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
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
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

    private val _comment = MutableStateFlow("")
    val comment: StateFlow<String> = _comment.asStateFlow()

    private val _rating = MutableStateFlow(0f)
    val rating: StateFlow<Float> = _rating.asStateFlow()

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

    fun onCommentChanged(text: String) { _comment.value = text }
    fun onRatingChanged(value: Float) { _rating.value = value }

    fun setImage(uri: Uri?) { 
        _imageSource.value = uri 
        if (_postType.value == PostType.PUBLICATION && uri != null) {
            _currentStep.value = 1
        }
    }
    
    fun setCapturedImage(bitmap: Bitmap?) { 
        if (bitmap != null) {
            _imageSource.value = bitmap
            if (_postType.value == PostType.PUBLICATION) {
                _currentStep.value = 1
            } else {
                _currentStep.value = 0 
            }
        }
    }
    
    fun goToStep(step: Int) { _currentStep.value = step }

    private suspend fun saveBitmapToLocal(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val filename = "shot_${UUID.randomUUID()}.jpg"
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath
    }

    fun createPost(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val source = _imageSource.value ?: return@launch
            val activeUser = userRepository.getActiveUser() ?: return@launch
            val commentText = _comment.value
            
            // Usamos la URI real si está disponible, si es Bitmap (de cámara) la guardamos temporalmente
            val imageUrl = when (source) {
                is Uri -> source.toString()
                is Bitmap -> saveBitmapToLocal(source)
                else -> "https://picsum.photos/seed/${System.currentTimeMillis()}/800/800"
            }
            
            socialRepository.createPost(PostEntity(
                id = "post_${System.currentTimeMillis()}", 
                userId = activeUser.id, 
                imageUrl = imageUrl, 
                comment = commentText, 
                timestamp = System.currentTimeMillis()
            ))
            socialRepository.syncSocialData() // Forzar refresco
            _comment.value = "" // Reset
            onSuccess()
        }
    }

    fun submitReview(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val ratingValue = _rating.value
            val commentText = _comment.value
            val validation = validateReviewInput(ratingValue, commentText)
            if (validation.isFailure) return@launch
            val activeUser = userRepository.getActiveUser() ?: return@launch
            val coffeeId = _selectedCoffee.value?.coffee?.id ?: return@launch
            
            val source = _imageSource.value
            val imageUrl = when (source) {
                is Uri -> source.toString()
                is Bitmap -> saveBitmapToLocal(source)
                else -> null
            }

            val reviewResult = reviewRepository.submitReview(
                Review(
                    user = activeUser.toDomainUser(),
                    coffeeId = coffeeId,
                    rating = ratingValue,
                    comment = commentText,
                    imageUrl = imageUrl,
                    timestamp = System.currentTimeMillis()
                )
            )
            if (reviewResult.isSuccess) {
                coffeeRepository.syncCoffees()
                socialRepository.syncSocialData() // Forzar refresco
                _comment.value = "" // Reset
                _rating.value = 0f // Reset
                onSuccess()
            }
        }
    }

    private fun UserEntity.toDomainUser(): User = User(
        id = id, username = username, fullName = fullName, avatarUrl = avatarUrl, email = email, bio = bio
    )
}
