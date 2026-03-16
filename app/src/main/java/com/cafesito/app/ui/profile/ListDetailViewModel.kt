package com.cafesito.app.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.CoffeeRepository
import com.cafesito.app.data.ListMemberRow
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
import kotlinx.coroutines.withContext
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

    /** public | invitation | private. Para mostrar "Añadirme" solo si es pública y para avatares/miembros. */
    val listPrivacy = MutableStateFlow("private")

    private val _itemIds = MutableStateFlow<List<String>>(emptyList())
    val isLoading = MutableStateFlow(true)

    val listCoffees = combine(_itemIds, coffeeRepository.allCoffees) { ids, allCoffees ->
        ids.mapNotNull { id -> allCoffees.find { it.coffee.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** True si la lista es propia del usuario de la ruta (puede editar/compartir); false si es compartida con él. */
    val isOwnList = MutableStateFlow(true)

    /** True si el usuario actual es miembro (tiene la lista en sus listas compartidas). Para listas públicas/invitación no propias: mostrar "Salir" si true, "Unirse" si false. */
    val isMember = MutableStateFlow(false)

    /** Número de miembros (dueño + user_list_members). Solo cuando lista es pública o por invitación y soy dueño. */
    private val _listMembers = MutableStateFlow<List<ListMemberRow>>(emptyList())
    val listMemberCount = MutableStateFlow(0)

    /** Hasta 3 usuarios para mostrar avatares en la TopBar (dueño + primeros 2 miembros). */
    val listMemberPreviews = MutableStateFlow<List<UserEntity>>(emptyList())

    /** True si el usuario puede añadir/quitar cafés: es dueño, o es miembro y la lista tiene members_can_edit. */
    val canEditList = MutableStateFlow(false)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (listId.isNotBlank()) {
                val owned = supabaseDataSource.getCachedUserLists(userId)
                val me = userRepository.getActiveUser()?.id ?: userId
                val shared = supabaseDataSource.getCachedSharedWithMeLists(me)
                val list = (owned + shared).distinctBy { it.id }.find { it.id == listId }
                list?.let {
                    listName.value = it.name
                    listIsPublic.value = it.isPublic
                    listPrivacy.value = it.privacy?.takeIf { p -> p in listOf("public", "invitation", "private") }
                        ?: if (it.isPublic) "public" else "private"
                    val own = it.userId == userId.toLong()
                    isOwnList.value = own
                    isMember.value = !own
                    canEditList.value = own || (!own && (it.membersCanEdit == true))
                    if (listPrivacy.value == "public" || listPrivacy.value == "invitation") {
                        val members = supabaseDataSource.fetchListMembersByListId(listId)
                        _listMembers.value = members
                        listMemberCount.value = 1 + members.size
                        val ownerId = it.userId.toInt()
                        val memberIds = listOf(ownerId) + members.map { m -> m.userId.toInt() }
                        val previews = memberIds.distinct().take(3).mapNotNull { id -> userRepository.getUserById(id) }
                        listMemberPreviews.value = previews
                    }
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
                val owned = supabaseDataSource.getCachedUserLists(userId)
                val me = userRepository.getActiveUser()?.id ?: userId
                val shared = supabaseDataSource.getCachedSharedWithMeLists(me)
                val list = (owned + shared).distinctBy { it.id }.find { it.id == listId }
                list?.let {
                    listName.value = it.name
                    listIsPublic.value = it.isPublic
                    listPrivacy.value = it.privacy?.takeIf { p -> p in listOf("public", "invitation", "private") }
                        ?: if (it.isPublic) "public" else "private"
                    val own = it.userId == me.toLong()
                    isMember.value = !own
                    canEditList.value = own || (!own && (it.membersCanEdit == true))
                    if (listPrivacy.value == "public" || listPrivacy.value == "invitation") {
                        val members = supabaseDataSource.fetchListMembersByListId(listId)
                        _listMembers.value = members
                        listMemberCount.value = 1 + members.size
                        val ownerId = it.userId.toInt()
                        val memberIds = listOf(ownerId) + members.map { m -> m.userId.toInt() }
                        listMemberPreviews.value = memberIds.distinct().take(3).mapNotNull { id -> userRepository.getUserById(id) }
                    }
                }
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
                supabaseDataSource.invalidateUserListsCache(userId)
                userRepository.getActiveUser()?.id?.let { supabaseDataSource.invalidateUserListsCache(it) }
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
            runCatching {
                supabaseDataSource.joinPublicList(listId)
                isMember.value = true
            }
            userRepository.getActiveUser()?.id?.let { supabaseDataSource.invalidateUserListsCache(it) }
        }
    }

    /** Salir de la lista (cuando eres miembro pero no dueño). Tras salir invoca onLeft (navegación en Main). */
    fun leaveList(onLeft: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val me = userRepository.getActiveUser()?.id ?: return@launch
            runCatching { supabaseDataSource.leaveList(listId, me) }
            supabaseDataSource.invalidateUserListsCache(me)
            supabaseDataSource.invalidateUserListsCache(userId)
            withContext(Dispatchers.Main) { onLeft() }
        }
    }
}
