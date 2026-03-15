package com.cafesito.app.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.CoffeeRepository
import com.cafesito.app.data.SupabaseDataSource
import com.cafesito.app.data.UserEntity
import com.cafesito.app.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val supabaseDataSource: SupabaseDataSource,
    private val coffeeRepository: CoffeeRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val userId: Int = savedStateHandle["userId"] ?: 0
    val listId: String = savedStateHandle["listId"] ?: ""
    val listName = MutableStateFlow((savedStateHandle["listName"] as? String)?.takeIf { it.isNotBlank() } ?: "Lista")
    val listIsPublic = MutableStateFlow(false)

    private val _itemIds = MutableStateFlow<List<String>>(emptyList())
    val isLoading = MutableStateFlow(true)

    val listCoffees = combine(_itemIds, coffeeRepository.allCoffees) { ids, allCoffees ->
        ids.mapNotNull { id -> allCoffees.find { it.coffee.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** True si la lista es propia del usuario de la ruta (puede editar/compartir); false si es compartida con él. */
    val isOwnList = MutableStateFlow(true)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (listId.isNotBlank()) {
                val owned = supabaseDataSource.getUserLists(userId)
                val me = userRepository.getActiveUser()?.id ?: userId
                val shared = supabaseDataSource.getSharedWithMeLists(me)
                val list = (owned + shared).distinctBy { it.id }.find { it.id == listId }
                list?.let {
                    listName.value = it.name
                    listIsPublic.value = it.isPublic
                    isOwnList.value = it.userId == userId.toLong()
                }
                val items = supabaseDataSource.getUserListItems(listId)
                _itemIds.value = items.map { it.coffeeId }
            }
            isLoading.value = false
        }
    }

    fun removeFromList(coffeeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            supabaseDataSource.removeUserListItem(listId, coffeeId)
            _itemIds.update { it.filter { id -> id != coffeeId } }
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            if (listId.isNotBlank()) {
                val items = supabaseDataSource.getUserListItems(listId)
                _itemIds.value = items.map { it.coffeeId }
            }
        }
    }

    fun updateList(name: String, isPublic: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (listId.isNotBlank()) {
                supabaseDataSource.updateUserList(listId, name, isPublic)
                listName.value = name
                listIsPublic.value = isPublic
            }
        }
    }

    fun deleteList() {
        viewModelScope.launch(Dispatchers.IO) {
            if (listId.isNotBlank()) {
                supabaseDataSource.deleteUserList(listId)
            }
        }
    }

    private val _usersForInvite = MutableStateFlow<List<UserEntity>>(emptyList())
    val usersForInvite: StateFlow<List<UserEntity>> = _usersForInvite.asStateFlow()

    fun loadUsersForInvite() {
        viewModelScope.launch(Dispatchers.IO) {
            val all = userRepository.getAllUsersList()
            _usersForInvite.value = all.filter { it.id != userId }
        }
    }

    fun inviteUser(inviteeId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { supabaseDataSource.createListInvitation(listId, inviteeId) }
        }
    }

    /** Unirse a una lista pública (cuando no es tu lista). Idempotente si ya eres miembro. */
    fun joinPublicList() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { supabaseDataSource.joinPublicList(listId) }
        }
    }
}
