package com.cafesito.app.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import java.time.Instant

@Singleton
class SupabaseDataSource @Inject constructor(
    private val client: SupabaseClient
) {
    @kotlinx.serialization.Serializable
    data class AccountLifecycleInfo(
        @kotlinx.serialization.SerialName("account_status") val accountStatus: String? = null,
        @kotlinx.serialization.SerialName("scheduled_deletion_at") val scheduledDeletionAt: Long? = null
    )
    @kotlinx.serialization.Serializable
    private data class UserTokenUpsert(
        @kotlinx.serialization.SerialName("user_id") val userId: Int,
        @kotlinx.serialization.SerialName("fcm_token") val fcmToken: String
    )

    @kotlinx.serialization.Serializable
    private data class NotificationInsert(
        @kotlinx.serialization.SerialName("user_id") val userId: Int,
        val type: String,
        @kotlinx.serialization.SerialName("from_username") val fromUsername: String,
        val message: String,
        val timestamp: Long,
        @kotlinx.serialization.SerialName("is_read") val isRead: Boolean = false,
        @kotlinx.serialization.SerialName("related_id") val relatedId: String? = null
    )
    // --- STORAGE ---
    suspend fun uploadImage(bucket: String, path: String, byteArray: ByteArray): String {
        val bucketRef = client.storage.from(bucket)
        bucketRef.upload(path, byteArray, upsert = true)
        return bucketRef.publicUrl(path)
    }

    // --- REALTIME ---
    fun subscribeToLikes(): Flow<PostgresAction> {
        val channel = client.realtime.channel("likes-realtime")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "likes_db"
        }
    }

    fun subscribeToComments(): Flow<PostgresAction> {
        val channel = client.realtime.channel("comments-realtime")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "comments_db"
        }
    }

    fun subscribeToPosts(): Flow<PostgresAction> {
        val channel = client.realtime.channel("posts-realtime")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "posts_db"
        }
    }

    @Suppress("DEPRECATION")
    fun subscribeToNotifications(userId: Int): Flow<PostgresAction> {
        val channel = client.realtime.channel("notifications-$userId")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "notifications_db"
        }
    }

    // --- USUARIOS ---
    suspend fun getAllUsers(): List<UserEntity> = client.postgrest["users_db"].select().decodeList<UserEntity>()
    suspend fun getUserById(id: Int): UserEntity? = client.postgrest["users_db"].select { filter { eq("id", id) } }.decodeSingleOrNull<UserEntity>()
    suspend fun getUserByUsername(username: String): UserEntity? = client.postgrest["users_db"].select { filter { eq("username", username) } }.decodeSingleOrNull<UserEntity>()
    suspend fun getUserByUsernameInsensitive(username: String): UserEntity? = client.postgrest["users_db"].select {
        filter { ilike("username", username) }
        limit(1)
    }.decodeSingleOrNull<UserEntity>()
    suspend fun getUserByGoogleId(googleId: String): UserEntity? = client.postgrest["users_db"].select { filter { eq("google_id", googleId) } }.decodeSingleOrNull<UserEntity>()
    suspend fun upsertUser(user: UserEntity) { client.postgrest["users_db"].upsert(user) }
    

    suspend fun touchUserLastInteraction(userId: Int) {
        client.postgrest["users_db"].update(
            {
                set("updated_at", Instant.now().toString())
            }
        ) {
            filter { eq("id", userId) }
        }
    }

    suspend fun requestAccountDeletion(userId: Int, scheduledDeletionAt: Long) {
        val now = System.currentTimeMillis()
        client.postgrest["users_db"].update(
            {
                set("account_status", "inactive_pending_deletion")
                set("deactivation_requested_at", now)
                set("scheduled_deletion_at", scheduledDeletionAt)
            }
        ) {
            filter { eq("id", userId) }
        }
    }

    suspend fun cancelAccountDeletion(userId: Int) {
        client.postgrest["users_db"].update(
            {
                set("account_status", "active")
                set("deactivation_requested_at", null as Long?)
                set("scheduled_deletion_at", null as Long?)
            }
        ) {
            filter { eq("id", userId) }
        }
    }

    suspend fun getAccountLifecycleInfo(userId: Int): AccountLifecycleInfo? =
        client.postgrest["users_db"]
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingleOrNull<AccountLifecycleInfo>()

    suspend fun hardDeleteAccountData(userId: Int) {
        val postIds = client.postgrest["posts_db"]
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList<PostEntity>()
            .map { it.id }

        if (postIds.isNotEmpty()) {
            client.postgrest["post_coffee_tags"].delete {
                filter { isIn("post_id", postIds) }
            }
        }

        client.postgrest["comments_db"].delete { filter { eq("user_id", userId) } }
        client.postgrest["likes_db"].delete { filter { eq("user_id", userId) } }
        client.postgrest["local_favorites"].delete { filter { eq("user_id", userId) } }
        client.postgrest["reviews_db"].delete { filter { eq("user_id", userId) } }
        client.postgrest["coffee_sensory_profiles"].delete { filter { eq("user_id", userId) } }
        client.postgrest["diary_entries"].delete { filter { eq("user_id", userId) } }
        client.postgrest["pantry_items"].delete { filter { eq("user_id", userId) } }
        client.postgrest["notifications_db"].delete { filter { eq("user_id", userId) } }
        client.postgrest["follows"].delete { filter { eq("follower_id", userId) } }
        client.postgrest["follows"].delete { filter { eq("followed_id", userId) } }
        client.postgrest["posts_db"].delete { filter { eq("user_id", userId) } }
        client.postgrest["users_db"].delete { filter { eq("id", userId) } }
    }

    suspend fun insertUserToken(token: UserTokenEntity) {
        try {
            // No enviamos `id` para evitar conflictos con PK autogenerada y forzar upsert por user_id.
            // Para Supabase kt 2.6.1, onConflict es un parámetro opcional de la función upsert
            client.postgrest["user_fcm_tokens"].upsert(
                value = UserTokenUpsert(
                    userId = token.userId,
                    fcmToken = token.fcmToken
                ),
                onConflict = "user_id"
            )
        } catch (e: Exception) {
            Log.e("SupabaseDataSource", "Error upserting FCM token", e)
            throw e
        }
    }

    // --- SEGUIMIENTOS ---
    suspend fun getAllFollows(): List<FollowEntity> = client.postgrest["follows"].select().decodeList<FollowEntity>()
    suspend fun insertFollow(follow: FollowEntity) { client.postgrest["follows"].insert(follow) }
    suspend fun deleteFollow(followerId: Int, followedId: Int) { client.postgrest["follows"].delete { filter { eq("follower_id", followerId); eq("followed_id", followedId) } } }

    // --- CAFÉS ---
    /** Obtiene todos los cafés desde Supabase (tabla coffees). Sin range PostgREST limita a 1000 filas. */
    suspend fun getAllCoffees(
        query: String? = null,
        origin: String? = null,
        roast: String? = null
    ): List<Coffee> = client.postgrest["coffees"].select {
        filter {
            if (!query.isNullOrBlank()) {
                or {
                    ilike("nombre", "%$query%")
                    ilike("marca", "%$query%")
                }
            }
            if (origin != null) eq("pais_origen", origin)
            if (roast != null) eq("tueste", roast)
        }
        order("nombre", Order.ASCENDING)
        range(0, 9999)
    }.decodeList<Coffee>()
    
    suspend fun getCoffeesPaginated(
        from: Long, 
        to: Long, 
        query: String? = null, 
        origins: Set<String> = emptySet(),
        roasts: Set<String> = emptySet(),
        specialties: Set<String> = emptySet(),
        formats: Set<String> = emptySet(),
        minRating: Float = 0f
    ): List<Coffee> = client.postgrest["coffees"].select {
        filter {
            if (!query.isNullOrBlank()) {
                or {
                    ilike("nombre", "%$query%")
                    ilike("marca", "%$query%")
                }
            }
            if (origins.isNotEmpty()) isIn("pais_origen", origins.toList())
            if (roasts.isNotEmpty()) isIn("tueste", roasts.toList())
            if (specialties.isNotEmpty()) isIn("especialidad", specialties.toList())
            if (formats.isNotEmpty()) isIn("formato", formats.toList())
            if (minRating > 0) gte("puntuacion_total", minRating)
        }
        order("nombre", Order.ASCENDING)
        range(from, to)
    }.decodeList<Coffee>()

    suspend fun upsertCoffees(coffees: List<Coffee>) { client.postgrest["coffees"].upsert(coffees) }
    
    suspend fun getCoffeesByIds(coffeeIds: List<String>): List<Coffee> {
        if (coffeeIds.isEmpty()) return emptyList()
        return client.postgrest["coffees"].select {
            filter {
                isIn("id", coffeeIds)
            }
        }.decodeList<Coffee>()
    }

    suspend fun getCoffeeByBarcode(barcode: String): Coffee? = client.postgrest["coffees"].select {
        filter {
            eq("codigo_barras", barcode)
        }
        limit(1)
    }.decodeSingleOrNull<Coffee>()

    // --- RECOMENDACIONES (RPC) ---
    suspend fun getRecommendationsRpc(userId: Int): List<Coffee> {
        return client.postgrest.rpc("get_coffee_recommendations", mapOf("target_user_id" to userId)).decodeList<Coffee>()
    }

    // --- CAFÉS PERSONALIZADOS (tabla coffees con is_custom = true y user_id) ---
    suspend fun getCustomCoffees(userId: Int): List<Coffee> = client.postgrest["coffees"].select {
        filter {
            eq("is_custom", true)
            eq("user_id", userId)
        }
    }.decodeList<Coffee>()

    suspend fun insertCustomCoffee(coffee: Coffee) = client.postgrest["coffees"].upsert(coffee)
    suspend fun upsertCustomCoffee(coffee: Coffee) = client.postgrest["coffees"].upsert(coffee)

    suspend fun updateCustomCoffee(id: String, userId: Int, coffee: Coffee) {
        client.postgrest["coffees"].update({
            set("nombre", coffee.nombre)
            set("marca", coffee.marca)
            set("especialidad", coffee.especialidad)
            set("tueste", coffee.tueste)
            set("variedad_tipo", coffee.variedadTipo)
            set("pais_origen", coffee.paisOrigen)
            set("cafeina", coffee.cafeina)
            set("formato", coffee.formato)
            set("image_url", coffee.imageUrl)
        }) {
            filter {
                eq("id", id)
                eq("user_id", userId)
            }
        }
    }

    suspend fun deleteCustomCoffee(id: String, userId: Int) {
        client.postgrest["coffees"].delete {
            filter {
                eq("id", id)
                eq("user_id", userId)
            }
        }
    }

    // --- PUBLICACIONES ---
    suspend fun getAllPosts(): List<PostEntity> = client.postgrest["posts_db"].select { order("timestamp", Order.DESCENDING) }.decodeList<PostEntity>()
    
    suspend fun getPostsByUserId(userId: Int): List<PostEntity> = client.postgrest["posts_db"].select {
        filter {
            eq("user_id", userId)
        }
        order("timestamp", Order.DESCENDING)
        limit(50) 
    }.decodeList<PostEntity>()
    
    suspend fun insertPost(post: PostEntity) = client.postgrest["posts_db"].upsert(post)
    suspend fun deletePost(postId: String) {
        client.postgrest["posts_db"].delete { filter { eq("id", postId) } }
    }

    suspend fun getAllPostCoffeeTags(): List<PostCoffeeTagEntity> =
        client.postgrest["post_coffee_tags"].select().decodeList<PostCoffeeTagEntity>()

    suspend fun upsertPostCoffeeTag(tag: PostCoffeeTagEntity) {
        client.postgrest["post_coffee_tags"].upsert(tag)
    }

    suspend fun deletePostCoffeeTag(postId: String) {
        client.postgrest["post_coffee_tags"].delete {
            filter { eq("post_id", postId) }
        }
    }

    // --- COMENTARIOS ---
    suspend fun getAllComments(): List<CommentEntity> = client.postgrest["comments_db"].select().decodeList<CommentEntity>()
    suspend fun getCommentsForPost(postId: String): List<CommentEntity> = client.postgrest["comments_db"].select { filter { eq("post_id", postId) }; order("timestamp", Order.ASCENDING) }.decodeList<CommentEntity>()
    suspend fun insertComment(comment: CommentInsert): CommentEntity {
        return client.postgrest["comments_db"].insert(comment) { select() }.decodeSingle()
    }
    suspend fun upsertComment(comment: CommentEntity) = client.postgrest["comments_db"].upsert(comment)
    suspend fun deleteComment(commentId: Int) {
        try {
            client.postgrest.rpc(
                "delete_comment",
                buildJsonObject {
                    put("p_comment_id", commentId)
                }
            )
        } catch (_: Exception) {
            client.postgrest["comments_db"].delete { filter { eq("id", commentId) } }
        }
    }
    suspend fun updateComment(commentId: Int, newText: String) {
        client.postgrest["comments_db"].update(
            {
                set("text", newText)
            }
        ) {
            filter { eq("id", commentId) }
        }
    }

    // --- LIKES ---
    suspend fun getAllLikes(): List<LikeEntity> = client.postgrest["likes_db"].select().decodeList<LikeEntity>()
    suspend fun insertLike(like: LikeEntity) = client.postgrest["likes_db"].upsert(like)
    suspend fun deleteLike(postId: String, userId: Int) { client.postgrest["likes_db"].delete { filter { eq("post_id", postId); eq("user_id", userId) } } }

    // --- RESEÑAS ---
    suspend fun getAllReviews(): List<ReviewEntity> = client.postgrest["reviews_db"].select().decodeList<ReviewEntity>()

    suspend fun getReviewsByCoffeeId(coffeeId: String): List<ReviewEntity> = client.postgrest["reviews_db"].select {
        filter {
            eq("coffee_id", coffeeId)
        }
    }.decodeList<ReviewEntity>()
    
    suspend fun upsertReview(review: ReviewEntity) {
        val insertData = ReviewInsert(
            id = if (review.id == 0L) null else review.id,
            coffeeId = review.coffeeId,
            userId = review.userId,
            rating = review.rating,
            comment = review.comment,
            imageUrl = review.imageUrl,
            aroma = review.aroma,
            sabor = review.sabor,
            cuerpo = review.cuerpo,
            acidez = review.acidez,
            dulzura = review.dulzura,
            timestamp = review.timestamp
        )
        client.postgrest["reviews_db"].upsert(insertData)
    }

    suspend fun deleteReview(coffeeId: String, userId: Int) {
        client.postgrest["reviews_db"].delete {
            filter { eq("coffee_id", coffeeId); eq("user_id", userId) }
        }
    }


    // --- PERFIL SENSORIAL ---
    suspend fun getSensoryProfilesByCoffeeId(coffeeId: String): List<CoffeeSensoryProfileEntity> = client.postgrest["coffee_sensory_profiles"].select {
        filter { eq("coffee_id", coffeeId) }
    }.decodeList<CoffeeSensoryProfileEntity>()

    /** Perfiles sensoriales del usuario (para ADN, igual que webapp). */
    suspend fun getSensoryProfilesByUserId(userId: Int): List<CoffeeSensoryProfileEntity> = client.postgrest["coffee_sensory_profiles"].select {
        filter { eq("user_id", userId) }
    }.decodeList<CoffeeSensoryProfileEntity>()

    suspend fun upsertSensoryProfile(profile: CoffeeSensoryProfileEntity) {
        client.postgrest["coffee_sensory_profiles"].upsert(profile)
    }

    // --- FAVORITOS (OFICIALES) ---
    suspend fun getFavoritesByUserId(userId: Int): List<LocalFavorite> = client.postgrest["local_favorites"].select {
        filter { eq("user_id", userId) }
    }.decodeList<LocalFavorite>()
    
    suspend fun insertFavorite(favorite: LocalFavorite) { client.postgrest["local_favorites"].upsert(favorite) }
    suspend fun deleteFavorite(coffeeId: String, userId: Int) {
        client.postgrest["local_favorites"].delete {
            filter { eq("coffee_id", coffeeId); eq("user_id", userId) }
        }
    }

    // --- DIARIO ---
    suspend fun getDiaryEntries(userId: Int): List<DiaryEntryEntity> = client.postgrest["diary_entries"].select { 
        filter { eq("user_id", userId) }
        order("timestamp", Order.DESCENDING) 
    }.decodeList<DiaryEntryEntity>()

    suspend fun insertDiaryEntry(entry: DiaryEntryEntity): DiaryEntryEntity {
        val insertData = DiaryEntryInsert(
            userId = entry.userId,
            coffeeId = entry.coffeeId,
            coffeeName = entry.coffeeName,
            coffeeBrand = entry.coffeeBrand,
            caffeineAmount = entry.caffeineAmount,
            amountMl = entry.amountMl,
            coffeeGrams = entry.coffeeGrams,
            preparationType = entry.preparationType,
            sizeLabel = entry.sizeLabel,
            timestamp = entry.timestamp,
            type = entry.type,
            pantryItemId = entry.pantryItemId
        )
        return client.postgrest["diary_entries"].insert(insertData) { select() }.decodeSingle<DiaryEntryEntity>()
    }
    suspend fun upsertDiaryEntry(entry: DiaryEntryEntity) {
        client.postgrest["diary_entries"].upsert(entry)
    }

    /** Borra solo la entrada del diario por id. No toca pantry_items (despensa). */
    suspend fun deleteDiaryEntry(entryId: Long) { client.postgrest["diary_entries"].delete { filter { eq("id", entryId) } } }

    // --- DESPENSA (varios registros por café; cada ítem tiene id único) ---
    suspend fun getPantryItems(userId: Int): List<PantryItemEntity> = client.postgrest["pantry_items"].select { filter { eq("user_id", userId) } }.decodeList<PantryItemEntity>()
    suspend fun upsertPantryItem(item: PantryItemEntity) {
        client.postgrest["pantry_items"].upsert(item)
    }
    suspend fun deletePantryItemById(id: String) {
        client.postgrest["pantry_items"].delete { filter { eq("id", id) } }
    }

    // --- HISTORIAL (pantry_historical): fuente de verdad Supabase
    suspend fun getFinishedCoffees(userId: Int): List<FinishedCoffeeEntity> =
        client.postgrest["pantry_historical"].select {
            filter { eq("user_id", userId) }
            order("finished_at", Order.DESCENDING)
        }.decodeList<FinishedCoffeeEntity>()

    suspend fun insertFinishedCoffee(insert: FinishedCoffeeInsert) {
        client.postgrest["pantry_historical"].insert(insert)
    }

    // --- NOTIFICACIONES ---
    suspend fun insertNotification(notification: NotificationEntity) {
        try {
            client.postgrest.rpc(
                "create_notification",
                buildJsonObject {
                    put("p_user_id", notification.userId)
                    put("p_type", notification.type)
                    put("p_from_username", notification.fromUsername)
                    put("p_message", notification.message)
                    put("p_timestamp", notification.timestamp)
                    notification.relatedId?.let { put("p_related_id", it) }
                }
            )
        } catch (rpcError: Exception) {
            Log.w("SupabaseDataSource", "RPC create_notification unavailable, falling back to direct insert: ${rpcError.message}")
            try {
                client.postgrest["notifications_db"].insert(
                    NotificationInsert(
                        userId = notification.userId,
                        type = notification.type,
                        fromUsername = notification.fromUsername,
                        message = notification.message,
                        timestamp = notification.timestamp,
                        isRead = notification.isRead,
                        relatedId = notification.relatedId
                    )
                )
            } catch (e: Exception) {
                Log.e("SupabaseDataSource", "Error inserting notification: ${e.message}")
                throw e
            }
        }
    }

    suspend fun getNotificationsForUser(userId: Int): List<NotificationEntity> {
        return try {
            client.postgrest.rpc(
                "get_notifications_for_user",
                buildJsonObject {
                    put("p_user_id", userId)
                }
            ).decodeList()
        } catch (rpcError: Exception) {
            Log.w("SupabaseDataSource", "RPC get_notifications_for_user unavailable, falling back to direct query: ${rpcError.message}")
            try {
                client.postgrest["notifications_db"].select {
                    filter { eq("user_id", userId) }
                    order("timestamp", Order.DESCENDING)
                }.decodeList<NotificationEntity>()
            } catch (e: Exception) {
                Log.e("SupabaseDataSource", "Error fetching notifications: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun markNotificationRead(notificationId: Int) {
        try {
            client.postgrest.rpc(
                "mark_notification_read",
                buildJsonObject {
                    put("p_notification_id", notificationId)
                }
            )
        } catch (rpcError: Exception) {
            Log.w("SupabaseDataSource", "RPC mark_notification_read unavailable, falling back to direct update: ${rpcError.message}")
            try {
                client.postgrest["notifications_db"].update({
                    set("is_read", true)
                }) {
                    filter { eq("id", notificationId) }
                }
            } catch (e: Exception) {
                Log.e("SupabaseDataSource", "Error marking read: ${e.message}")
            }
        }
    }

    suspend fun markAllNotificationsRead(userId: Int) {
        try {
            client.postgrest.rpc(
                "mark_all_notifications_read",
                buildJsonObject {
                    put("p_user_id", userId)
                }
            )
        } catch (rpcError: Exception) {
            Log.w("SupabaseDataSource", "RPC mark_all_notifications_read unavailable, falling back to direct update: ${rpcError.message}")
            try {
                client.postgrest["notifications_db"].update({
                    set("is_read", true)
                }) {
                    filter { eq("user_id", userId) }
                }
            } catch (e: Exception) {
                Log.e("SupabaseDataSource", "Error marking all read: ${e.message}")
            }
        }
    }

    suspend fun deleteNotification(notificationId: Int) {
        try {
            client.postgrest.rpc(
                "delete_notification",
                buildJsonObject {
                    put("p_notification_id", notificationId)
                }
            )
        } catch (rpcError: Exception) {
            Log.w("SupabaseDataSource", "RPC delete_notification unavailable, falling back to direct delete: ${rpcError.message}")
            try {
                client.postgrest["notifications_db"].delete {
                    filter { eq("id", notificationId) }
                }
            } catch (e: Exception) {
                Log.e("SupabaseDataSource", "Error deleting notification: ${e.message}")
            }
        }
    }

    // --- LISTAS PERSONALIZADAS (user_lists, user_list_items, invitaciones) ---
    /** Listas propias del usuario (donde user_id = userId). En 500/error devuelve lista vacía para no romper la app. */
    suspend fun getUserLists(userId: Int): List<UserListRow> = try {
        client.postgrest["user_lists"].select {
            filter { eq("user_id", userId.toLong()) }
        }.decodeList<UserListRow>()
    } catch (e: Exception) {
        Log.w("SupabaseDataSource", "getUserLists: ${e.message}")
        emptyList()
    }

    /** Obtiene una lista por ID (para deep link: abrir lista compartida). RLS permite si es pública, propia o miembro. */
    suspend fun getUserListById(listId: String): UserListRow? = try {
        client.postgrest["user_lists"].select {
            filter { eq("id", listId) }
        }.decodeList<UserListRow>().firstOrNull()
    } catch (e: Exception) {
        Log.w("SupabaseDataSource", "getUserListById: ${e.message}")
        null
    }

    /** Listas compartidas con el usuario (donde está en user_list_members). */
    suspend fun getSharedWithMeLists(userId: Int): List<UserListRow> {
        val memberRows = try {
            client.postgrest["user_list_members"].select {
                filter { eq("user_id", userId.toLong()) }
            }.decodeList<UserListMemberRow>()
        } catch (e: Exception) {
            Log.w("SupabaseDataSource", "getSharedWithMeLists members: ${e.message}")
            return emptyList()
        }
        val listIds = memberRows.map { it.listId }.distinct()
        if (listIds.isEmpty()) return emptyList()
        return try {
            client.postgrest["user_lists"].select {
                filter { isIn("id", listIds) }
            }.decodeList<UserListRow>()
        } catch (e: Exception) {
            Log.w("SupabaseDataSource", "getSharedWithMeLists lists: ${e.message}")
            emptyList()
        }
    }

    suspend fun createListInvitation(listId: String, inviteeId: Int): String {
        val raw = client.postgrest.rpc(
            "create_list_invitation",
            buildJsonObject {
                put("p_list_id", listId)
                put("p_invitee_id", inviteeId.toLong())
            }
        )
        return try {
            raw.decodeSingle<CreateListInvitationResult>().id
        } catch (_: Exception) {
            raw.decodeList<String>().firstOrNull()?.trim('"') ?: ""
        }
    }

    @kotlinx.serialization.Serializable
    private data class CreateListInvitationResult(
        @kotlinx.serialization.SerialName("create_list_invitation") val id: String
    )

    suspend fun acceptListInvitation(invitationId: String) {
        client.postgrest.rpc(
            "accept_list_invitation",
            buildJsonObject { put("p_invitation_id", invitationId) }
        )
    }

    suspend fun declineListInvitation(invitationId: String) {
        client.postgrest.rpc(
            "decline_list_invitation",
            buildJsonObject { put("p_invitation_id", invitationId) }
        )
    }

    suspend fun joinPublicList(listId: String) {
        client.postgrest.rpc(
            "join_public_list",
            buildJsonObject { put("p_list_id", listId) }
        )
    }

    suspend fun createUserList(userId: Int, name: String, isPublic: Boolean): UserListRow {
        val insert = UserListInsert(userId = userId.toLong(), name = name, isPublic = isPublic)
        val list = client.postgrest["user_lists"].insert(insert) { select() }.decodeList<UserListRow>()
        return list.firstOrNull() ?: throw IllegalStateException("createUserList: insert did not return row")
    }

    suspend fun getUserListItems(listId: String): List<UserListItemRow> = client.postgrest["user_list_items"].select {
        filter { eq("list_id", listId) }
    }.decodeList<UserListItemRow>()

    @kotlinx.serialization.Serializable
    private data class UserListItemRaw(
        @kotlinx.serialization.SerialName("list_id") val listId: String,
        @kotlinx.serialization.SerialName("coffee_id") val coffeeId: String,
        @kotlinx.serialization.SerialName("created_at") val createdAt: String? = null
    )

    /** Ítems de listas del usuario con nombre y visibilidad (para feed de actividad; solo listas públicas en perfiles ajenos). */
    suspend fun getListItemsWithMetaForUser(userId: Int): List<ListItemActivityRow> {
        val lists = getUserLists(userId)
        if (lists.isEmpty()) return emptyList()
        val listIds = lists.map { it.id }
        val listMap = lists.associate { it.id to Pair(it.name, it.isPublic) }
        val rows = try {
            client.postgrest["user_list_items"].select {
                filter { isIn("list_id", listIds) }
                order("created_at", Order.DESCENDING)
            }.decodeList<UserListItemRaw>()
        } catch (e: Exception) {
            Log.w("SupabaseDataSource", "getListItemsWithMetaForUser: ${e.message}")
            return emptyList()
        }
        return rows.mapNotNull { r ->
            val (name, isPublic) = listMap[r.listId] ?: return@mapNotNull null
            val ts = try {
                r.createdAt?.let { java.time.Instant.parse(it.replace("Z", "").plus("Z")).toEpochMilli() }
                    ?: System.currentTimeMillis()
            } catch (_: Exception) { System.currentTimeMillis() }
            ListItemActivityRow(listId = r.listId, listName = name, isPublic = isPublic, coffeeId = r.coffeeId, createdAt = ts)
        }
    }

    suspend fun addUserListItem(listId: String, coffeeId: String) {
        try {
            client.postgrest["user_list_items"].upsert(
                value = UserListItemInsert(listId = listId, coffeeId = coffeeId),
                onConflict = "list_id,coffee_id"
            )
        } catch (e: Exception) {
            // Duplicado o error de red: ignorar
            Log.w("SupabaseDataSource", "addUserListItem: ${e.message}")
        }
    }

    suspend fun removeUserListItem(listId: String, coffeeId: String) {
        client.postgrest["user_list_items"].delete {
            filter { eq("list_id", listId); eq("coffee_id", coffeeId) }
        }
    }

    suspend fun updateUserList(listId: String, name: String, isPublic: Boolean) {
        client.postgrest["user_lists"].update({
            set("name", name)
            set("is_public", isPublic)
        }) {
            filter { eq("id", listId) }
        }
    }

    /** Elimina la lista y sus ítems. No modifica pantry_items: los cafés de la despensa no se borran. */
    suspend fun deleteUserList(listId: String) {
        client.postgrest["user_list_items"].delete { filter { eq("list_id", listId) } }
        client.postgrest["user_lists"].delete { filter { eq("id", listId) } }
    }

    /** IDs de cafés que están en alguna lista custom del usuario (para icono activo en detalle). */
    suspend fun getCoffeeIdsInUserLists(userId: Int): List<String> {
        val lists = getUserLists(userId)
        if (lists.isEmpty()) return emptyList()
        val listIds = lists.map { it.id }
        return client.postgrest["user_list_items"].select {
            filter { isIn("list_id", listIds) }
        }.decodeList<UserListItemRow>().map { it.coffeeId }.distinct()
    }
}
