package com.cafesito.app.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.SupabaseDataSource
import com.cafesito.app.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListJoinViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val supabaseDataSource: SupabaseDataSource,
    private val userRepository: UserRepository
) : ViewModel() {

    val listId: String = savedStateHandle["listId"] ?: ""

    private val _joinInfo = MutableStateFlow<SupabaseDataSource.ListInfoForJoin?>(null)
    val joinInfo: StateFlow<SupabaseDataSource.ListInfoForJoin?> = _joinInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isJoining = MutableStateFlow(false)
    val isJoining: StateFlow<Boolean> = _isJoining.asStateFlow()

    private val _ownerUsername = MutableStateFlow<String?>(null)
    val ownerUsername: StateFlow<String?> = _ownerUsername.asStateFlow()

    init {
        viewModelScope.launch {
            val info = supabaseDataSource.getListInfoForJoin(listId)
            _joinInfo.value = info
            _isLoading.value = false
            info?.userId?.toInt()?.let { uid ->
                _ownerUsername.value = userRepository.getUserById(uid)?.username
            }
        }
    }

    /** Une al usuario actual a la lista; invalida caché de listas. Lanza si la operación falla. */
    suspend fun join() {
        if (_isJoining.value) return
        _isJoining.value = true
        try {
            supabaseDataSource.joinListByLink(listId)
            userRepository.getActiveUserFlow().first()?.id?.let { supabaseDataSource.invalidateUserListsCache(it) }
        } finally {
            _isJoining.value = false
        }
    }
}
