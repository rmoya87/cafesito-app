package com.cafesito.app.ui.profile

import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.analytics.AnalyticsHelper
import com.cafesito.app.data.ListInvitationRow
import com.cafesito.app.data.ListMemberRow
import com.cafesito.app.data.SupabaseDataSource
import com.cafesito.app.data.UserEntity
import com.cafesito.app.data.UserListRow
import com.cafesito.app.data.UserRepository
import com.cafesito.app.share.DirectShareRepository
import com.cafesito.app.share.DirectShareShortcutPublisher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

@HiltViewModel
class ListOptionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val supabaseDataSource: SupabaseDataSource,
    private val userRepository: UserRepository,
    private val directShareRepository: DirectShareRepository,
    private val directShareShortcutPublisher: DirectShareShortcutPublisher,
    private val analyticsHelper: AnalyticsHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private suspend fun trackShareEventBackend(
        eventName: String,
        targetType: String,
        targetId: String? = null,
        shareMethod: String? = null
    ) {
        val metadata = buildJsonObject {
            put("list_id", listId)
            put("source", "list_options")
            if (!shareMethod.isNullOrBlank()) put("share_method", shareMethod)
        }
        supabaseDataSource.logShareEvent(
            eventName = eventName,
            platform = "android",
            originScreen = "list_options",
            contentType = "list",
            targetType = targetType,
            targetId = targetId ?: listId,
            metadata = metadata
        )
    }

    private val routeUserId: Int = savedStateHandle["userId"] ?: 0
    val listId: String = savedStateHandle["listId"] ?: ""

    private val _list = MutableStateFlow<UserListRow?>(null)
    val list: StateFlow<UserListRow?> = _list.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val listPrivacy: StateFlow<String> = MutableStateFlow("private").apply {
        viewModelScope.launch { _list.collect { row ->
            value = row?.privacy?.takeIf { it in listOf("public", "invitation", "private") }
                ?: if (row?.isPublic == true) "public" else "private"
        } }
    }.asStateFlow()

    val listMembersCanEdit: StateFlow<Boolean> = MutableStateFlow(false).apply {
        viewModelScope.launch { _list.collect { value = it?.membersCanEdit ?: false } }
    }.asStateFlow()

    val listMembersCanInvite: StateFlow<Boolean> = MutableStateFlow(false).apply {
        viewModelScope.launch { _list.collect { value = it?.membersCanInvite ?: false } }
    }.asStateFlow()

    val isOwner: StateFlow<Boolean> = MutableStateFlow(false).apply {
        viewModelScope.launch {
            _list.collect { row ->
                val me = userRepository.getActiveUser()?.id ?: 0
                value = row != null && row.userId == me.toLong()
            }
        }
    }.asStateFlow()

    private val _members = MutableStateFlow<List<ListMemberRow>>(emptyList())
    val members: StateFlow<List<ListMemberRow>> = _members.asStateFlow()

    private val _invitations = MutableStateFlow<List<ListInvitationRow>>(emptyList())
    val invitations: StateFlow<List<ListInvitationRow>> = _invitations.asStateFlow()

    private val _allUsers = MutableStateFlow<List<UserEntity>>(emptyList())
    val allUsers: StateFlow<List<UserEntity>> = _allUsers.asStateFlow()

    /** Usuarios para mostrar en la sección Miembros (dueño + miembros con nombre/avatar). */
    val memberUsers: StateFlow<List<UserEntity>> = combine(_list, _members, _allUsers) { listRow, mems, users ->
        val ownerId = listRow?.userId ?: 0L
        val ids = listOf(ownerId) + mems.map { it.userId }
        ids.distinct().mapNotNull { id -> users.find { it.id == id.toInt() } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _invitingId = MutableStateFlow<Int?>(null)
    val invitingId: StateFlow<Int?> = _invitingId.asStateFlow()

    private val _showCopyChip = MutableStateFlow(false)
    val showCopyChip: StateFlow<Boolean> = _showCopyChip.asStateFlow()

    /** IDs de usuarios con follow mutuo (para listas públicas: terceros solo ven en Miembros a estos). */
    private val _mutualFollowIds = MutableStateFlow<Set<Int>>(emptySet())
    val mutualFollowIds: StateFlow<Set<Int>> = _mutualFollowIds.asStateFlow()

    /** Usuario actual (para que el tercero se vea a sí mismo en la lista de miembros). */
    private val _currentUserId = MutableStateFlow(0)
    val currentUserId: StateFlow<Int> = _currentUserId.asStateFlow()

    init {
        loadData()
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.followingMap.collect { map ->
                val me = userRepository.getActiveUser()?.id ?: 0
                val myFollowing = map[me].orEmpty()
                val myFollowers = map.entries.filter { it.value.contains(me) }.map { it.key }.toSet()
                _mutualFollowIds.value = myFollowing.intersect(myFollowers)
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            if (listId.isBlank()) {
                _isLoading.value = false
                return@launch
            }
            val me = userRepository.getActiveUser()?.id ?: routeUserId
            _currentUserId.value = me
            // Invalidar caché para que al abrir Opciones se carguen permisos actualizados (members_can_invite, members_can_edit).
            supabaseDataSource.invalidateUserListsCache(routeUserId)
            supabaseDataSource.invalidateUserListsCache(me)
            val owned = supabaseDataSource.getCachedUserLists(routeUserId)
            val shared = supabaseDataSource.getCachedSharedWithMeLists(me)
            var listRow = (owned + shared).distinctBy { it.id }.find { it.id == listId }
                ?: supabaseDataSource.getUserListById(listId)
            // Asegurar lista fresca desde el servidor (p. ej. admin acaba de activar "permitir que los miembros inviten").
            supabaseDataSource.getUserListById(listId)?.let { fresh -> listRow = fresh }
            _list.value = listRow

            if (listRow != null) {
                _members.value = supabaseDataSource.fetchListMembersByListId(listId)
                _invitations.value = supabaseDataSource.fetchListInvitationsByListId(listId)
            }
            _allUsers.value = userRepository.getAllUsersList()
            _isLoading.value = false
        }
    }

    fun refresh() {
        loadData()
    }

    fun updatePrivacy(privacy: String, membersCanEdit: Boolean, membersCanInvite: Boolean? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val listRow = _list.value ?: return@launch
            runCatching {
                supabaseDataSource.updateUserListWithPrivacy(listId, listRow.name, privacy, membersCanEdit, membersCanInvite)
            }
            supabaseDataSource.invalidateUserListsCache(routeUserId)
            supabaseDataSource.invalidateUserListsCache(_currentUserId.value)
            _list.update { row ->
                row?.copy(
                    privacy = privacy,
                    membersCanEdit = membersCanEdit,
                    membersCanInvite = membersCanInvite ?: row.membersCanInvite
                )
            }
        }
    }

    fun inviteUser(inviteeId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _invitingId.value = inviteeId
            runCatching { supabaseDataSource.createListInvitation(listId, inviteeId) }
            _members.value = supabaseDataSource.fetchListMembersByListId(listId)
            _invitations.value = supabaseDataSource.fetchListInvitationsByListId(listId)
            _invitingId.value = null
        }
    }

    fun removeMember(userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { supabaseDataSource.leaveList(listId, userId) }
            supabaseDataSource.invalidateUserListsCache(routeUserId)
            supabaseDataSource.invalidateUserListsCache(_currentUserId.value)
            _members.value = supabaseDataSource.fetchListMembersByListId(listId)
        }
    }

    fun getShareUrl(): String {
        // Mismo esquema que WebApp para deep link; el dominio puede venir de BuildConfig en producción.
        return "https://cafesitoapp.com/profile/list/$listId"
    }

    fun copyLinkAndShowChip() {
        val url = getShareUrl()
        viewModelScope.launch(Dispatchers.Main) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("List URL", url))
            _showCopyChip.value = true
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            withContext(Dispatchers.Main) { _showCopyChip.value = false }
        }
    }

    /** Abre la hoja nativa de Android para compartir el enlace de la lista. */
    fun shareList() {
        val url = getShareUrl()
        viewModelScope.launch(Dispatchers.IO) {
            analyticsHelper.trackEvent(
                "share_opened",
                bundleOf("origin_screen" to "list_options", "content_type" to "list", "target_type" to "system_share")
            )
            trackShareEventBackend("share_opened", "system_share", shareMethod = "android_chooser")
            val targets = runCatching { directShareRepository.getSuggestedTargets(limit = 4) }
                .getOrDefault(emptyList())
            if (targets.isNotEmpty()) {
                directShareShortcutPublisher.publishSuggestedTargets(context, targets)
                analyticsHelper.trackEvent(
                    "share_target_shown",
                    bundleOf(
                        "origin_screen" to "list_options",
                        "content_type" to "list",
                        "target_type" to "direct",
                        "target_id" to "dynamic_shortcuts_${targets.size}"
                    )
                )
                trackShareEventBackend("share_target_shown", "direct", "dynamic_shortcuts_${targets.size}", "android_shortcuts")
            }
            withContext(Dispatchers.Main) {
                runCatching {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                    val chooserIntent = Intent.createChooser(sendIntent, "Compartir lista").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooserIntent)
                }.onSuccess {
                    analyticsHelper.trackEvent(
                        "share_completed",
                        bundleOf("origin_screen" to "list_options", "content_type" to "list", "target_type" to "system_share")
                    )
                    viewModelScope.launch(Dispatchers.IO) { trackShareEventBackend("share_completed", "system_share", shareMethod = "android_chooser") }
                }.onFailure {
                    analyticsHelper.trackEvent(
                        "share_failed",
                        bundleOf("origin_screen" to "list_options", "content_type" to "list", "target_type" to "system_share")
                    )
                    viewModelScope.launch(Dispatchers.IO) { trackShareEventBackend("share_failed", "system_share", shareMethod = "android_chooser") }
                }
            }
        }
    }

    fun updateList(name: String, privacy: String, membersCanEdit: Boolean, membersCanInvite: Boolean? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _list.value
            runCatching {
                supabaseDataSource.updateUserListWithPrivacy(
                    listId, name, privacy, membersCanEdit,
                    membersCanInvite ?: current?.membersCanInvite
                )
            }
            supabaseDataSource.invalidateUserListsCache(routeUserId)
            supabaseDataSource.invalidateUserListsCache(_currentUserId.value)
            _list.update { it?.copy(name = name, isPublic = privacy == "public", privacy = privacy, membersCanEdit = membersCanEdit, membersCanInvite = membersCanInvite ?: it.membersCanInvite) }
        }
    }

    fun deleteList() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { supabaseDataSource.deleteUserList(listId) }
            supabaseDataSource.invalidateUserListsCache(routeUserId)
            supabaseDataSource.invalidateUserListsCache(_currentUserId.value)
        }
    }

    fun leaveList() {
        viewModelScope.launch(Dispatchers.IO) {
            val me = userRepository.getActiveUser()?.id ?: return@launch
            runCatching { supabaseDataSource.leaveList(listId, me) }
            supabaseDataSource.invalidateUserListsCache(me)
            supabaseDataSource.invalidateUserListsCache(routeUserId)
        }
    }
}
