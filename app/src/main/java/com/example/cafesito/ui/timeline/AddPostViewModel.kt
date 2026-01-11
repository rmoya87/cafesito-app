package com.example.cafesito.ui.timeline

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.CoffeeRepository
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.data.ReviewEntity
import com.example.cafesito.domain.Post
import com.example.cafesito.domain.currentUser
import com.example.cafesito.domain.samplePosts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PostType { PUBLICATION, OPINION }

@HiltViewModel
class AddPostViewModel @Inject constructor(
    private val coffeeRepository: CoffeeRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep = _currentStep.asStateFlow()

    private val _postType = MutableStateFlow(PostType.PUBLICATION)
    val postType = _postType.asStateFlow()

    private val _imageSource = MutableStateFlow<Any?>(null)
    val imageSource = _imageSource.asStateFlow()

    private val _galleryImages = MutableStateFlow<List<Uri>>(
        List(20) { i -> Uri.parse("https://picsum.photos/seed/${i + 150}/400/400") }
    )
    val galleryImages = _galleryImages.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCoffee = MutableStateFlow<CoffeeWithDetails?>(null)
    val selectedCoffee = _selectedCoffee.asStateFlow()

    // Flujo unificado para la lista de cafés: Historial si está vacío, Resultados si hay texto
    val coffeeList: StateFlow<List<CoffeeWithDetails>> = combine(
        _searchQuery,
        coffeeRepository.allCoffees
    ) { query, all ->
        if (query.isEmpty()) {
            // Simulamos historial: los 10 primeros de la lista (ordenados por nombre en el DAO)
            // En una app real usaríamos una tabla 'recent_visits' con timestamp
            all.take(10)
        } else {
            all.filter {
                it.coffee.nombre.contains(query, ignoreCase = true) ||
                it.coffee.marca.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        _imageSource.value = _galleryImages.value.firstOrNull()
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
    fun setCapturedImage(bitmap: Bitmap?) { _imageSource.value = bitmap }
    fun goToStep(step: Int) { _currentStep.value = step }

    fun createPost(comment: String, onSuccess: () -> Unit) {
        val source = _imageSource.value ?: return
        val imageUrl = when (source) {
            is Uri -> source.toString()
            is Bitmap -> "https://picsum.photos/seed/capture_${System.currentTimeMillis()}/400/400"
            else -> return
        }
        val newPost = Post(id = "post_${System.currentTimeMillis()}", user = currentUser, imageUrl = imageUrl, comment = comment, timestamp = System.currentTimeMillis(), initialLikes = 0, comments = emptyList())
        samplePosts.add(0, newPost)
        onSuccess()
    }

    fun submitReview(rating: Float, comment: String, onSuccess: () -> Unit) {
        val coffeeId = _selectedCoffee.value?.coffee?.id ?: return
        viewModelScope.launch {
            val review = ReviewEntity(coffeeId = coffeeId, userId = currentUser.id, rating = rating, comment = comment, timestamp = System.currentTimeMillis())
            coffeeRepository.upsertReview(review)
            onSuccess()
        }
    }
}
