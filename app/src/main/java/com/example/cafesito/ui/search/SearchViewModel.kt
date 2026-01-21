package com.example.cafesito.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.CoffeeRepository
import com.example.cafesito.data.CoffeeWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: CoffeeRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError = _syncError.asStateFlow()

    // Control de paginación
    private val _displayLimit = MutableStateFlow(10)
    private val PAGE_SIZE = 10
    private val LOAD_THRESHOLD = 2 

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _syncError.value = null
            try {
                repository.syncCoffees()
            } catch (e: Exception) {
                Log.e("SEARCH_VM", "Error sincronizando: ${e.message}")
                _syncError.value = "Error de red: ${e.localizedMessage}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedOrigin = MutableStateFlow<String?>(null)
    val selectedOrigin = _selectedOrigin.asStateFlow()

    private val _selectedRoast = MutableStateFlow<String?>(null)
    val selectedRoast = _selectedRoast.asStateFlow()

    private val _selectedSpecialty = MutableStateFlow<String?>(null)
    val selectedSpecialty = _selectedSpecialty.asStateFlow()

    private val _selectedVariety = MutableStateFlow<String?>(null)
    val selectedVariety = _selectedVariety.asStateFlow()

    private val _selectedFormat = MutableStateFlow<String?>(null)
    val selectedFormat = _selectedFormat.asStateFlow()

    private val _selectedGrind = MutableStateFlow<String?>(null)
    val selectedGrind = _selectedGrind.asStateFlow()

    private val _minRating = MutableStateFlow(0f)
    val minRating = _minRating.asStateFlow()

    val filterOptions: StateFlow<FilterOptions> = repository.allCoffees.map { list ->
        fun String?.toAtomizedList(): List<String> = this?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        FilterOptions(
            origins = list.flatMap { it.coffee.paisOrigen.toAtomizedList() }.distinct().sorted(),
            roasts = list.flatMap { it.coffee.tueste.toAtomizedList() }.distinct().sorted(),
            specialties = list.flatMap { it.coffee.especialidad.toAtomizedList() }.distinct().sorted(),
            varieties = list.flatMap { it.coffee.variedadTipo.toAtomizedList() }.distinct().sorted(),
            formats = list.flatMap { it.coffee.formato.toAtomizedList() }.distinct().sorted(),
            grinds = list.flatMap { it.coffee.moliendaRecomendada.toAtomizedList() }.distinct().sorted()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FilterOptions())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SearchUiState> = combine(
        _searchQuery, _selectedOrigin, _selectedRoast, 
        _selectedSpecialty, _selectedVariety, _selectedFormat, _selectedGrind, _minRating,
        _displayLimit,
        repository.favorites // Añadimos favoritos al combine para reaccionar a cambios
    ) { args: Array<Any?> ->
        FilterParams(
            query = args[0] as String,
            origin = args[1] as? String,
            roast = args[2] as? String,
            specialty = args[3] as? String,
            variety = args[4] as? String,
            format = args[5] as? String,
            grind = args[6] as? String,
            rating = args[7] as Float,
            limit = args[8] as Int
        )
    }.flatMapLatest { p ->
        repository.getFilteredCoffees(p.query, p.origin, p.roast, p.specialty, p.variety, p.format, p.grind)
            .map<List<CoffeeWithDetails>, SearchUiState> { coffees -> 
                val baseFiltered = if (p.rating > 0) coffees.filter { it.averageRating >= p.rating } else coffees
                
                val isSearching = p.query.isNotBlank() || p.origin != null || p.roast != null || 
                                 p.specialty != null || p.variety != null || p.format != null || 
                                 p.grind != null || p.rating > 0f

                val finalItems = if (isSearching) baseFiltered else baseFiltered.take(p.limit)
                
                SearchUiState.Success(finalItems, isPaginated = !isSearching)
            }
    }.catch { e ->
        emit(SearchUiState.Error(e.message ?: "Error desconocido")) 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState.Loading)

    fun onItemDisplayed(index: Int) {
        val currentItems = (uiState.value as? SearchUiState.Success)?.coffees?.size ?: 0
        val isPaginated = (uiState.value as? SearchUiState.Success)?.isPaginated ?: false
        
        if (isPaginated && index >= currentItems - LOAD_THRESHOLD) {
            _displayLimit.value += PAGE_SIZE
        }
    }

    fun onSearchQueryChanged(q: String) { _searchQuery.value = q }
    fun setOrigin(v: String?) { _selectedOrigin.value = v }
    fun setRoast(v: String?) { _selectedRoast.value = v }
    fun setSpecialty(v: String?) { _selectedSpecialty.value = v }
    fun setVariety(v: String?) { _selectedVariety.value = v }
    fun setFormat(v: String?) { _selectedFormat.value = v }
    fun setGrind(v: String?) { _selectedGrind.value = v }
    fun setMinRating(v: Float) { _minRating.value = v }

    fun clearFilters() {
        _selectedOrigin.value = null
        _selectedRoast.value = null
        _selectedSpecialty.value = null
        _selectedVariety.value = null
        _selectedFormat.value = null
        _selectedGrind.value = null
        _minRating.value = 0f
        _displayLimit.value = 10 
    }

    fun toggleFavorite(id: String, isFav: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(id, !isFav)
                // No necesitamos refrescar manualmente si el repository.favorites está en el combine del uiState
            } catch (e: Exception) {
                Log.e("SEARCH_VM", "Error al cambiar favorito", e)
                _syncError.value = "No se pudo actualizar el favorito"
            }
        }
    }
}

private data class FilterParams(
    val query: String, val origin: String?, val roast: String?, 
    val specialty: String?, val variety: String?, val format: String?, 
    val grind: String?, val rating: Float, val limit: Int
)

data class FilterOptions(
    val origins: List<String> = emptyList(),
    val roasts: List<String> = emptyList(),
    val specialties: List<String> = emptyList(),
    val varieties: List<String> = emptyList(),
    val formats: List<String> = emptyList(),
    val grinds: List<String> = emptyList()
)

sealed interface SearchUiState {
    data object Loading : SearchUiState
    data class Success(val coffees: List<CoffeeWithDetails>, val isPaginated: Boolean) : SearchUiState
    data class Error(val message: String) : SearchUiState
}
