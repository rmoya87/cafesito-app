package com.cafesito.app.ui.timeline

import android.content.ContentUris
import android.content.Context
import android.util.Log
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
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
    private val reviewRepository: ReviewRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val validateReviewInput = ValidateReviewInputUseCase()

    private val _currentStep = MutableStateFlow(savedStateHandle.get<Int>("currentStep") ?: 0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _postType = MutableStateFlow(savedStateHandle.get<PostType>("postType") ?: PostType.PUBLICATION)
    val postType: StateFlow<PostType> = _postType.asStateFlow()

    private val _imageSource = MutableStateFlow<Any?>(
        savedStateHandle.get<String>("imageUri")?.let { Uri.parse(it) }
    )
    val imageSource: StateFlow<Any?> = _imageSource.asStateFlow()

    private val _galleryImages = MutableStateFlow<List<Uri>>(emptyList())
    val galleryImages: StateFlow<List<Uri>> = _galleryImages.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCoffee = MutableStateFlow<CoffeeWithDetails?>(null)
    val selectedCoffee: StateFlow<CoffeeWithDetails?> = _selectedCoffee.asStateFlow()

    init {
        savedStateHandle.get<String>("selectedCoffeeId")?.let { id ->
            viewModelScope.launch {
                coffeeRepository.allCoffees.collectLatest { list ->
                    val coffee = list.find { it.coffee.id == id }
                    if (coffee != null) {
                        _selectedCoffee.value = coffee
                    }
                }
            }
        }
    }

    private val _comment = MutableStateFlow(savedStateHandle.get<String>("comment") ?: "")
    val comment: StateFlow<String> = _comment.asStateFlow()

    private val _rating = MutableStateFlow(savedStateHandle.get<Float>("rating") ?: 0f)
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
        }
    }

    fun setPostType(type: PostType) {
        _postType.value = type
        savedStateHandle.set("postType", type)
        _currentStep.value = 0
        savedStateHandle.set("currentStep", 0)
        // Limpiar imágenes al cambiar de tipo si es necesario
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }

    fun selectCoffee(coffee: CoffeeWithDetails?) {
        _selectedCoffee.value = coffee
        if (coffee != null) {
            savedStateHandle.set("selectedCoffeeId", coffee.coffee.id)
            _currentStep.value = 1
            savedStateHandle.set("currentStep", 1)
        } else {
            savedStateHandle.remove<String>("selectedCoffeeId")
        }
    }

    fun onCommentChanged(text: String) { 
        _comment.value = text 
        savedStateHandle.set("comment", text)
    }
    
    fun onRatingChanged(value: Float) { 
        _rating.value = value 
        savedStateHandle.set("rating", value)
    }

    fun setImage(uri: Uri?) { 
        _imageSource.value = uri 
        savedStateHandle.set("imageUri", uri?.toString())
        
        // Navegación automática al siguiente paso tras elegir imagen
        if (uri != null) {
            if (_postType.value == PostType.PUBLICATION && _currentStep.value == 0) {
                _currentStep.value = 1
                savedStateHandle.set("currentStep", 1)
            } else if (_postType.value == PostType.OPINION && _currentStep.value == 1) {
                _currentStep.value = 2
                savedStateHandle.set("currentStep", 2)
            }
        }
    }

    fun setCapturedImage(bitmap: Bitmap?) { 
        if (bitmap != null) {
            _imageSource.value = bitmap
            val nextStep = if (_postType.value == PostType.PUBLICATION) 1 else 2
            _currentStep.value = nextStep
            savedStateHandle.set("currentStep", nextStep)

            viewModelScope.launch {
                val path = saveBitmapToLocal(bitmap)
                val fileUri = Uri.fromFile(File(path))
                _imageSource.value = fileUri
                savedStateHandle.set("imageUri", fileUri.toString())
            }
        }
    }
    
    fun goToStep(step: Int) { 
        _currentStep.value = step 
        savedStateHandle.set("currentStep", step)
    }

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
            socialRepository.syncSocialData()
            
            clearState()
            onSuccess()
        }
    }

    fun submitReview(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val ratingValue = _rating.value
            val commentText = _comment.value
            
            if (ratingValue == 0f) return@launch
            
            val activeUser = userRepository.getActiveUser() ?: return@launch
            val coffeeId = _selectedCoffee.value?.coffee?.id ?: return@launch
            
            val source = _imageSource.value
            val imageUrl = when (source) {
                is Uri -> source.toString()
                is Bitmap -> saveBitmapToLocal(source)
                else -> null
            }

            try {
                val existingReviewResult = reviewRepository.getReviewByUserAndCoffee(activeUser.id, coffeeId)
                val existingReviewId = existingReviewResult.getOrNull()?.id
                
                val reviewResult = reviewRepository.submitReview(
                    Review(
                        id = existingReviewId,
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
                    socialRepository.syncSocialData()
                    clearState()
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("AddPostVM", "Excepción al guardar reseña", e)
            }
        }
    }

    private fun clearState() {
        _comment.value = ""
        _rating.value = 0f
        _imageSource.value = null
        _selectedCoffee.value = null
        _currentStep.value = 0
        
        savedStateHandle.remove<String>("comment")
        savedStateHandle.remove<Float>("rating")
        savedStateHandle.remove<String>("imageUri")
        savedStateHandle.remove<String>("selectedCoffeeId")
        savedStateHandle.remove<Int>("currentStep")
    }

    private fun UserEntity.toDomainUser(): User = User(
        id = id, username = username, fullName = fullName, avatarUrl = avatarUrl, email = email, bio = bio
    )
}
