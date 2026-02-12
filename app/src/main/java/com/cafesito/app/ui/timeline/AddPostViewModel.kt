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
    private val supabaseDataSource: SupabaseDataSource,
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

    val activeUser: StateFlow<UserEntity?> = userRepository.getActiveUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    private val _mentionQuery = MutableStateFlow("")
    val mentionSuggestions: StateFlow<List<UserEntity>> = _mentionQuery
        .debounce(120)
        .flatMapLatest { query ->
            if (query.length < 1) flowOf(emptyList())
            else userRepository.getAllUsersFlow().map { users ->
                users.filter { it.username.contains(query, ignoreCase = true) }.take(6)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATE_ADDED
                )
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                
                try {
                    val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    context.contentResolver.query(
                        queryUri,
                        projection,
                        null,
                        null,
                        sortOrder
                    )?.use { cursor ->
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idColumn)
                            val contentUri = ContentUris.withAppendedId(queryUri, id)
                            list.add(contentUri)
                        }
                    }
                } catch (e: Exception) { 
                    Log.e("AddPostVM", "Error loading gallery", e)
                }
                list
            }
            _galleryImages.value = images
            
            // Set first image as default if none selected
            if (_imageSource.value == null && images.isNotEmpty() && _postType.value == PostType.PUBLICATION) {
                _imageSource.value = images[0]
                savedStateHandle.set("imageUri", images[0].toString())
            }
        }
    }

    fun setPostType(type: PostType) {
        _postType.value = type
        savedStateHandle.set("postType", type)
        _currentStep.value = 0
        savedStateHandle.set("currentStep", 0)
        if (type == PostType.OPINION) {
            _imageSource.value = null
            savedStateHandle.remove<String>("imageUri")
        }
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }

    fun selectCoffee(coffee: CoffeeWithDetails?) {
        _selectedCoffee.value = coffee
        if (coffee != null) {
            savedStateHandle.set("selectedCoffeeId", coffee.coffee.id)
            if (_postType.value == PostType.OPINION) {
                _currentStep.value = 2 // Jump directly to ReviewDetailsStepPremium
                savedStateHandle.set("currentStep", 2)
            } else {
                _currentStep.value = 1
                savedStateHandle.set("currentStep", 1)
            }
        } else {
            savedStateHandle.remove<String>("selectedCoffeeId")
        }
    }

    fun onCommentChanged(text: String) { 
        _comment.value = text 
        savedStateHandle.set("comment", text)

        val lastWord = text.split(" ").lastOrNull().orEmpty()
        _mentionQuery.value = if (lastWord.startsWith("@") && lastWord.length > 1) {
            lastWord.removePrefix("@")
        } else {
            ""
        }
    }
    
    fun onRatingChanged(value: Float) { 
        _rating.value = value 
        savedStateHandle.set("rating", value)
    }

    fun setImage(uri: Uri?) { 
        _imageSource.value = uri 
        savedStateHandle.set("imageUri", uri?.toString())
    }

    fun setCapturedImage(bitmap: Bitmap?) { 
        if (bitmap != null) {
            _imageSource.value = bitmap
            viewModelScope.launch {
                val uri = saveBitmapToLocalUri(bitmap)
                _imageSource.value = uri
                savedStateHandle.set("imageUri", uri.toString())
            }
        }
    }
    
    fun goToStep(step: Int) { 
        _currentStep.value = step 
        savedStateHandle.set("currentStep", step)
    }

    private suspend fun saveBitmapToLocalUri(bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val filename = "shot_${UUID.randomUUID()}.jpg"
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        Uri.fromFile(file)
    }

    private suspend fun uploadImageAndGetUrl(source: Any, bucket: String): String? = withContext(Dispatchers.IO) {
        try {
            val imageBytes = when (source) {
                is Uri -> context.contentResolver.openInputStream(source)?.use { it.readBytes() }
                is Bitmap -> {
                    val tempFile = File(context.cacheDir, "temp_${UUID.randomUUID()}.jpg")
                    FileOutputStream(tempFile).use { out ->
                        source.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    val bytes = tempFile.readBytes()
                    tempFile.delete()
                    bytes
                }
                else -> null
            } ?: return@withContext null

            val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
            supabaseDataSource.uploadImage(bucket, fileName, imageBytes)
        } catch (e: Exception) {
            Log.e("AddPostVM", "Error uploading image", e)
            null
        }
    }

    fun createPost(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val activeUser = userRepository.getActiveUser() ?: return@launch
            
            val imageUrl = _imageSource.value?.let { uploadImageAndGetUrl(it, "posts") } ?: ""
            
            socialRepository.createPost(PostEntity(
                id = "post_${System.currentTimeMillis()}", 
                userId = activeUser.id, 
                imageUrl = imageUrl, 
                comment = _comment.value, 
                timestamp = System.currentTimeMillis()
            ))
            socialRepository.syncSocialData()
            
            clearState()
            onSuccess()
        }
    }

    fun submitReview(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (_rating.value == 0f) return@launch
            val activeUser = userRepository.getActiveUser() ?: return@launch
            val coffeeId = _selectedCoffee.value?.coffee?.id ?: return@launch
            
            val imageUrl = _imageSource.value?.let { uploadImageAndGetUrl(it, "reviews") }

            try {
                val existingReviewResult = reviewRepository.getReviewByUserAndCoffee(activeUser.id, coffeeId)
                val existingReviewId = existingReviewResult.getOrNull()?.id
                
                val reviewResult = reviewRepository.submitReview(
                    Review(
                        id = existingReviewId,
                        user = activeUser.toDomainUser(),
                        coffeeId = coffeeId,
                        rating = _rating.value,
                        comment = _comment.value,
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
        id = id, username = username, fullName = fullName, avatarUrl = avatarUrl, email = email, bio = bio, favoriteCoffeeIds = emptyList()
    )
}
