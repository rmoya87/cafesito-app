package com.example.cafesito.ui.timeline

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Enum de nivel superior para visibilidad global
enum class PostType { PUBLICATION, OPINION }

@HiltViewModel
class AddPostViewModel @Inject constructor(
    private val coffeeRepository: CoffeeRepository,
    private val socialRepository: SocialRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _postType = MutableStateFlow(PostType.PUBLICATION)
    val postType: StateFlow<PostType> = _postType.asStateFlow()

    private val _imageSource = MutableStateFlow<Any?>(null)
    val imageSource: StateFlow<Any?> = _imageSource.asStateFlow()

    private val _galleryImages = MutableStateFlow<List<Uri>>(
        List(20) { i -> Uri.parse("https://picsum.photos/seed/${i + 150}/400/400") }
    )
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

    init {
        _imageSource.value = _galleryImages.value.firstOrNull()
    }

    fun setPostType(type: PostType) {
        _postType.value = type
        _currentStep.value = 0
        _imageSource.value = _galleryImages.value.firstOrNull()
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }

    fun selectCoffee(coffee: CoffeeWithDetails?) {
        _selectedCoffee.value = coffee
        if (coffee != null) _currentStep.value = 1
    }

    fun setImage(uri: Uri?) { _imageSource.value = uri }
    fun setCapturedImage(bitmap: Bitmap?) { _imageSource.value = bitmap }
    fun goToStep(step: Int) { _currentStep.value = step }

    fun createPost(comment: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val source = _imageSource.value ?: return@launch
            val activeUser = userRepository.getActiveUser() ?: return@launch
            val imageUrl = when (source) {
                is Uri -> source.toString()
                is Bitmap -> "https://picsum.photos/seed/capture_${System.currentTimeMillis()}/800/800"
                else -> return@launch
            }
            socialRepository.createPost(PostEntity("post_${System.currentTimeMillis()}", activeUser.id, imageUrl, comment, System.currentTimeMillis()))
            onSuccess()
        }
    }

    fun submitReview(rating: Float, comment: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val activeUser = userRepository.getActiveUser() ?: return@launch
            val coffeeId = _selectedCoffee.value?.coffee?.id ?: return@launch
            val source = _imageSource.value
            val imageUrl = when (source) {
                is Uri -> if (source.toString().contains("picsum")) null else source.toString()
                is Bitmap -> "https://picsum.photos/seed/rev_${System.currentTimeMillis()}/800/800"
                else -> null
            }
            coffeeRepository.upsertReview(ReviewEntity(coffeeId = coffeeId, userId = activeUser.id, rating = rating, comment = comment, imageUrl = imageUrl, timestamp = System.currentTimeMillis()))
            onSuccess()
        }
    }
}
