package com.cafesito.app.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.DiaryRepository
import com.cafesito.app.data.FinishedCoffeeWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistorialViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    val finishedCoffees = diaryRepository.getFinishedCoffees()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
