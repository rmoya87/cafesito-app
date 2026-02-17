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

@Singleton
class SupabaseDataSource @Inject constructor(
    private val client: SupabaseClient
) {
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
    suspend fun getUserByGoogleId(googleId: String): UserEntity? = client.postgrest["users_db"].select { filter { eq("google_id", googleId) } }.decodeSingleOrNull<UserEntity>()
    suspend fun upsertUser(user: UserEntity) { client.postgrest["users_db"].upsert(user) }
    
    suspend fun insertUserToken(token: UserTokenEntity) { 
        try {
            // Sintaxis correcta para Supabase KT 2.6.x: onConflict se pasa como parámetro
            client.postgrest["user_fcm_tokens"].upsert(token, onConflict = "fcm_token")
        } catch (e: Exception) {
            Log.e("SupabaseDataSource", "Error inserting FCM token: ${e.message}")
        }
    }

    // --- SEGUIMIENTOS ---
    suspend fun getAllFollows(): List<FollowEntity> = client.postgrest["follows"].select().decodeList<FollowEntity>()
    suspend fun insertFollow(follow: FollowEntity) { client.postgrest["follows"].insert(follow) }
    suspend fun deleteFollow(followerId: Int, followedId: Int) { client.postgrest["follows"].delete { filter { eq("follower_id", followerId); eq("followed_id", followedId) } } }

    // --- CAFÉS ---
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

    // --- CAFÉS PERSONALIZADOS ---
    suspend fun getCustomCoffees(userId: Int): List<CustomCoffeeEntity> = client.postgrest["custom_coffees"].select { filter { eq("user_id", userId) } }.decodeList<CustomCoffeeEntity>()
    suspend fun insertCustomCoffee(coffee: CustomCoffeeEntity) = client.postgrest["custom_coffees"].upsert(coffee)
    suspend fun upsertCustomCoffee(coffee: CustomCoffeeEntity) = client.postgrest["custom_coffees"].upsert(coffee)
    
    suspend fun updateCustomCoffee(id: String, userId: Int, coffee: CustomCoffeeEntity) {
        client.postgrest["custom_coffees"].update({
            set("name", coffee.name)
            set("brand", coffee.brand)
            set("specialty", coffee.specialty)
            set("roast", coffee.roast)
            set("variety", coffee.variety)
            set("country", coffee.country)
            set("has_caffeine", coffee.hasCaffeine)
            set("format", coffee.format)
            set("image_url", coffee.imageUrl)
            set("total_grams", coffee.totalGrams)
        }) {
            filter {
                eq("id", id)
                eq("user_id", userId)
            }
        }
    }

    suspend fun deleteCustomCoffee(id: String, userId: Int) {
        client.postgrest["custom_coffees"].delete {
            filter {
                eq("id", id)
                eq("user_id", userId)
            }
        }
    }

    // --- FAVORITOS PERSONALIZADOS (RESTAURADOS) ---
    suspend fun getAllFavoritesCustom(): List<LocalFavoriteCustom> = client.postgrest["local_favorites_custom"].select().decodeList<LocalFavoriteCustom>()
    
    suspend fun getFavoritesCustomByUserId(userId: Int): List<LocalFavoriteCustom> = client.postgrest["local_favorites_custom"].select {
        filter { eq("user_id", userId) }
    }.decodeList<LocalFavoriteCustom>()

    suspend fun insertFavoriteCustom(favorite: LocalFavoriteCustom) { client.postgrest["local_favorites_custom"].upsert(favorite) }
    
    suspend fun deleteFavoriteCustom(coffeeId: String, userId: Int) {
        client.postgrest["local_favorites_custom"].delete {
            filter { eq("coffee_id", coffeeId); eq("user_id", userId) }
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

    suspend fun upsertSensoryProfile(profile: CoffeeSensoryProfileEntity) {
        client.postgrest["coffee_sensory_profiles"].upsert(profile)
    }

    // --- FAVORITOS (OFICIALES) ---
    suspend fun getAllFavorites(): List<LocalFavorite> = client.postgrest["local_favorites"].select().decodeList<LocalFavorite>()
    
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
            type = entry.type
        )
        return client.postgrest["diary_entries"].insert(insertData) { select() }.decodeSingle<DiaryEntryEntity>()
    }
    suspend fun upsertDiaryEntry(entry: DiaryEntryEntity) {
        client.postgrest["diary_entries"].upsert(entry)
    }

    suspend fun deleteDiaryEntry(entryId: Long) { client.postgrest["diary_entries"].delete { filter { eq("id", entryId) } } }

    // --- DESPENSA ---
    suspend fun getPantryItems(userId: Int): List<PantryItemEntity> = client.postgrest["pantry_items"].select { filter { eq("user_id", userId) } }.decodeList<PantryItemEntity>()
    suspend fun upsertPantryItem(item: PantryItemEntity) {
        client.postgrest["pantry_items"].upsert(item)
    }
    suspend fun deletePantryItem(coffeeId: String, userId: Int) { client.postgrest["pantry_items"].delete { filter { eq("coffee_id", coffeeId); eq("user_id", userId) } } }

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
}
