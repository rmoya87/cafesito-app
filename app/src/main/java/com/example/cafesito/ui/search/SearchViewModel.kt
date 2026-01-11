package com.example.cafesito.ui.search

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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Filtros seleccionados
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

    // Opciones atomizadas y limpias de nulos/vacíos
    val filterOptions: StateFlow<FilterOptions> = repository.allCoffees.map { list ->
        fun String.toAtomizedList() = this.split(",").map { it.trim() }.filter { it.isNotBlank() }

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
        _selectedSpecialty, _selectedVariety, _selectedFormat, _selectedGrind, _minRating
    ) { params -> params }.flatMapLatest { params ->
        val query = params[0] as String
        val origin = params[1] as String?
        val roast = params[2] as String?
        val specialty = params[3] as String?
        val variety = params[4] as String?
        val format = params[5] as String?
        val grind = params[6] as String?
        val rating = params[7] as Float

        repository.getFilteredCoffees(query, origin, roast, specialty, variety, format, grind)
            .map<List<CoffeeWithDetails>, SearchUiState> { coffees -> 
                val filtered = if (rating > 0) {
                    coffees.filter { it.averageRating >= rating }
                } else coffees
                SearchUiState.Success(filtered)
            }
    }.catch { 
        emit(SearchUiState.Error(it.message ?: "Error desconocido")) 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState.Loading)

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    
    fun setOrigin(value: String?) { _selectedOrigin.value = value }
    fun setRoast(value: String?) { _selectedRoast.value = value }
    fun setSpecialty(value: String?) { _selectedSpecialty.value = value }
    fun setVariety(value: String?) { _selectedVariety.value = value }
    fun setFormat(value: String?) { _selectedFormat.value = value }
    fun setGrind(value: String?) { _selectedGrind.value = value }
    fun setMinRating(value: Float) { _minRating.value = value }

    fun clearFilters() {
        _selectedOrigin.value = null
        _selectedRoast.value = null
        _selectedSpecialty.value = null
        _selectedVariety.value = null
        _selectedFormat.value = null
        _selectedGrind.value = null
        _minRating.value = 0f
    }

    fun toggleFavorite(coffeeId: String, isFavorite: Boolean) {
        viewModelScope.launch { repository.toggleFavorite(coffeeId, isFavorite) }
    }
}

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
    data class Success(val coffees: List<CoffeeWithDetails>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}
