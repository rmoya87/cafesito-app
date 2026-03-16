package com.cafesito.app.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.CoffeeRepository
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.data.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: CoffeeRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _displayLimit = MutableStateFlow(10)
    private val PAGE_SIZE = 10
    private val LOAD_THRESHOLD = 2

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches = _recentSearches.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedOrigins = MutableStateFlow<Set<String>>(emptySet())
    val selectedOrigins = _selectedOrigins.asStateFlow()

    private val _selectedRoasts = MutableStateFlow<Set<String>>(emptySet())
    val selectedRoasts = _selectedRoasts.asStateFlow()

    private val _selectedSpecialties = MutableStateFlow<Set<String>>(emptySet())
    val selectedSpecialties = _selectedSpecialties.asStateFlow()

    private val _selectedFormats = MutableStateFlow<Set<String>>(emptySet())
    val selectedFormats = _selectedFormats.asStateFlow()

    private val _minRating = MutableStateFlow(0f)
    val minRating = _minRating.asStateFlow()

    init { refreshData() }

    /**
     * Sincroniza la lista de cafés desde Supabase y actualiza la caché local.
     * Se ejecuta al abrir la pestaña Explorar y al hacer pull-to-refresh.
     * Si un café recién añadido en Supabase no aparece, desliza hacia abajo para refrescar.
     */
    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                syncManager.syncCoffeesIfNeeded(force = true)
                syncManager.syncFavoritesIfNeeded(force = true)
            } catch (e: Exception) {
                Log.e("SEARCH_VM", "Error al sincronizar desde Supabase: ${e.message}", e)
            } finally {
                repository.triggerRefresh()
                _isRefreshing.value = false
            }
        }
    }

    private fun String.toTitleCase(): String =
        this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    private fun String?.toAtomizedList(): List<String> = 
        this?.split(",")?.map { it.trim().toTitleCase() }?.filter { it.isNotBlank() } ?: emptyList()

    private val publicCoffees = repository.allCoffees
        .onStart { emit(emptyList()) }
        .map { list -> list.filter { !it.coffee.isCustom } }

    // ✅ OPTIMIZACIÓN: Añadir debounce de 300ms a la búsqueda
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private val debouncedSearchQuery = _searchQuery
        .debounce(300)
        .distinctUntilChanged()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val adaptiveFilterOptions: StateFlow<FilterOptions> = combine(
        listOf(publicCoffees, debouncedSearchQuery, _selectedOrigins, _selectedRoasts, _selectedSpecialties, _selectedFormats, _minRating)
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val coffees = args[0] as List<CoffeeWithDetails>
        @Suppress("UNCHECKED_CAST")
        val q = args[1] as String
        @Suppress("UNCHECKED_CAST")
        val origins = args[2] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val roasts = args[3] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val specialties = args[4] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val formats = args[5] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val rating = args[6] as Float

        val qAdapt = if (q.length >= 2) q else ""

        fun getOptionsFor(
            exOrigin: Boolean = false, exRoast: Boolean = false, 
            exSpecialty: Boolean = false, exFormat: Boolean = false
        ): List<CoffeeWithDetails> {
            return coffees.filter { item ->
                val c = item.coffee
                val matchQuery = qAdapt.isBlank() || c.nombre.contains(qAdapt, true) || c.marca.contains(qAdapt, true)
                val matchOrigin = exOrigin || origins.isEmpty() || c.paisOrigen.toAtomizedList().any { it in origins }
                val matchRoast = exRoast || roasts.isEmpty() || c.tueste.toAtomizedList().any { it in roasts }
                val matchSpecialty = exSpecialty || specialties.isEmpty() || c.especialidad.toAtomizedList().any { it in specialties }
                val matchFormat = exFormat || formats.isEmpty() || c.formato.toAtomizedList().any { it in formats }
                val matchRating = rating == 0f || item.averageRating >= rating
                matchQuery && matchOrigin && matchRoast && matchSpecialty && matchFormat && matchRating
            }
        }

        FilterOptions(
            origins = getOptionsFor(exOrigin = true).flatMap { it.coffee.paisOrigen.toAtomizedList() }.distinct().sorted(),
            roasts = getOptionsFor(exRoast = true).flatMap { it.coffee.tueste.toAtomizedList() }.distinct().sorted(),
            specialties = getOptionsFor(exSpecialty = true).flatMap { it.coffee.especialidad.toAtomizedList() }.distinct().sorted(),
            formats = getOptionsFor(exFormat = true).flatMap { it.coffee.formato.toAtomizedList() }.distinct().sorted()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FilterOptions())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SearchUiState> = combine(
        listOf(publicCoffees, debouncedSearchQuery, _selectedOrigins, _selectedRoasts, _selectedSpecialties, _selectedFormats, _minRating, _displayLimit)
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val coffees = args[0] as List<CoffeeWithDetails>
        @Suppress("UNCHECKED_CAST")
        val q = args[1] as String
        @Suppress("UNCHECKED_CAST")
        val origins = args[2] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val roasts = args[3] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val specialties = args[4] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val formats = args[5] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val rating = args[6] as Float
        @Suppress("UNCHECKED_CAST")
        val limit = args[7] as Int

        val filtered = coffees.filter { item ->
            val c = item.coffee
            val matchQuery = q.isBlank() ||
                c.nombre.contains(q, true) ||
                c.marca.contains(q, true) ||
                (c.codigoBarras?.contains(q, true) == true)
            val matchOrigin = origins.isEmpty() || c.paisOrigen.toAtomizedList().any { it in origins }
            val matchRoast = roasts.isEmpty() || c.tueste.toAtomizedList().any { it in roasts }
            val matchSpecialty = specialties.isEmpty() || c.especialidad.toAtomizedList().any { it in specialties }
            val matchFormat = formats.isEmpty() || c.formato.toAtomizedList().any { it in formats }
            val matchRating = rating == 0f || item.averageRating >= rating
            matchQuery && matchOrigin && matchRoast && matchSpecialty && matchFormat && matchRating
        }

        val isSearching = q.isNotBlank() || origins.isNotEmpty() || roasts.isNotEmpty() || specialties.isNotEmpty() || formats.isNotEmpty() || rating > 0f
        SearchUiState.Success(filtered, !isSearching) as SearchUiState
    }.catch { emit(SearchUiState.Error(it.message ?: "No se han podido cargar los datos.")) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState.Loading)

    fun onItemDisplayed(index: Int) {
        val state = uiState.value as? SearchUiState.Success ?: return
        if (state.isPaginated && index >= state.coffees.size - LOAD_THRESHOLD) _displayLimit.value += PAGE_SIZE
    }

    fun onSearchQueryChanged(q: String) { _searchQuery.value = q }
    fun addSearchToHistory(q: String) { if (q.isNotBlank() && !_recentSearches.value.contains(q)) _recentSearches.value = (listOf(q) + _recentSearches.value).take(5) }
    fun clearRecentSearches() { _recentSearches.value = emptyList() }
    fun toggleOrigin(v: String) { _selectedOrigins.value = _selectedOrigins.value.toggle(v) }
    fun toggleRoast(v: String) { _selectedRoasts.value = _selectedRoasts.value.toggle(v) }
    fun toggleSpecialty(v: String) { _selectedSpecialties.value = _selectedSpecialties.value.toggle(v) }
    fun toggleFormat(v: String) { _selectedFormats.value = _selectedFormats.value.toggle(v) }
    fun setMinRating(v: Float) { _minRating.value = v }
    private fun Set<String>.toggle(v: String) = if (contains(v)) this - v else this + v
    
    fun clearFilters() {
        _selectedOrigins.value = emptySet(); _selectedRoasts.value = emptySet()
        _selectedSpecialties.value = emptySet(); _selectedFormats.value = emptySet()
        _minRating.value = 0f; _displayLimit.value = 10
    }

    fun toggleFavorite(id: String, isFav: Boolean) { viewModelScope.launch { repository.toggleFavorite(id, !isFav) } }

    fun onBarcodeScanned(rawValue: String, onCoffeeFound: (String) -> Unit) {
        val normalizedCode = rawValue.filter { it.isDigit() }
        if (normalizedCode.isBlank()) return

        viewModelScope.launch {
            _searchQuery.value = normalizedCode
            clearFilters()

            val matchedCoffeeId = repository.findCoffeeIdByBarcode(normalizedCode)
            if (matchedCoffeeId != null) onCoffeeFound(matchedCoffeeId)
        }
    }
}

data class FilterOptions(val origins: List<String> = emptyList(), val roasts: List<String> = emptyList(), val specialties: List<String> = emptyList(), val formats: List<String> = emptyList())

sealed interface SearchUiState {
    data object Loading : SearchUiState
    data class Success(val coffees: List<CoffeeWithDetails>, val isPaginated: Boolean) : SearchUiState
    data class Error(val message: String) : SearchUiState
}
