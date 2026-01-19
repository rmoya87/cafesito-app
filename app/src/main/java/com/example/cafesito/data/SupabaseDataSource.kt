package com.example.cafesito.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseDataSource @Inject constructor(
    private val client: SupabaseClient
) {
    // --- STORAGE ---
    suspend fun uploadImage(bucket: String, path: String, byteArray: ByteArray): String {
        val bucketRef = client.storage.from(bucket)
        bucketRef.upload(path, byteArray, upsert = true)
        return bucketRef.publicUrl(path)
    }

    // --- REALTIME LIKES ---
    fun subscribeToLikes(): Flow<PostgresAction> {
        val channel = client.realtime.channel("likes-changes")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "likes_db"
        }
    }

    // --- USUARIOS ---
    suspend fun getAllUsers(): List<UserEntity> = client.postgrest["users_db"].select().decodeList<UserEntity>()
    suspend fun getUserById(id: Int): UserEntity? = client.postgrest["users_db"].select { filter { eq("id", id) } }.decodeSingleOrNull<UserEntity>()
    suspend fun getUserByUsername(username: String): UserEntity? = client.postgrest["users_db"].select { filter { eq("username", username) } }.decodeSingleOrNull<UserEntity>()
    suspend fun getUserByGoogleId(googleId: String): UserEntity? = client.postgrest["users_db"].select { filter { eq("google_id", googleId) } }.decodeSingleOrNull<UserEntity>()
    suspend fun upsertUser(user: UserEntity) { client.postgrest["users_db"].upsert(user) }

    // --- SEGUIMIENTOS ---
    suspend fun getAllFollows(): List<FollowEntity> = client.postgrest["follows"].select().decodeList<FollowEntity>()
    suspend fun insertFollow(follow: FollowEntity) { client.postgrest["follows"].insert(follow) }
    suspend fun deleteFollow(followerId: Int, followedId: Int) { client.postgrest["follows"].delete { filter { eq("follower_id", followerId); eq("followed_id", followedId) } } }

    // --- CAFÉS ---
    suspend fun getAllCoffees(): List<Coffee> = client.postgrest["coffees"].select { order("nombre", Order.ASCENDING) }.decodeList<Coffee>()
    suspend fun upsertCoffees(coffees: List<Coffee>) { client.postgrest["coffees"].upsert(coffees) }

    // --- CAFÉS PERSONALIZADOS ---
    suspend fun getCustomCoffees(userId: Int): List<CustomCoffeeEntity> = client.postgrest["custom_coffees"].select { filter { eq("user_id", userId) } }.decodeList<CustomCoffeeEntity>()
    suspend fun insertCustomCoffee(coffee: CustomCoffeeEntity) = client.postgrest["custom_coffees"].insert(coffee)
    suspend fun upsertCustomCoffee(coffee: CustomCoffeeEntity) = client.postgrest["custom_coffees"].upsert(coffee)
    
    suspend fun updateCustomCoffee(id: String, userId: Int, coffee: CustomCoffeeEntity) {
        // Realizamos una actualización parcial para evitar violar políticas RLS que prohíben cambiar el owner o el ID
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

    // --- PUBLICACIONES ---
    suspend fun getAllPosts(): List<PostEntity> = client.postgrest["posts_db"].select { order("timestamp", Order.DESCENDING) }.decodeList<PostEntity>()
    suspend fun insertPost(post: PostEntity) = client.postgrest["posts_db"].insert(post)
    suspend fun deletePost(postId: String) {
        client.postgrest["posts_db"].delete { filter { eq("id", postId) } }
    }

    // --- COMENTARIOS ---
    suspend fun getAllComments(): List<CommentEntity> = client.postgrest["comments_db"].select().decodeList<CommentEntity>()
    suspend fun getCommentsForPost(postId: String): List<CommentEntity> = client.postgrest["comments_db"].select { filter { eq("post_id", postId) }; order("timestamp", Order.ASCENDING) }.decodeList<CommentEntity>()
    suspend fun insertComment(comment: CommentEntity) = client.postgrest["comments_db"].insert(comment)
    suspend fun upsertComment(comment: CommentEntity) = client.postgrest["comments_db"].upsert(comment)
    suspend fun deleteComment(commentId: Int) {
        client.postgrest["comments_db"].delete { filter { eq("id", commentId) } }
    }

    // --- LIKES ---
    suspend fun getAllLikes(): List<LikeEntity> = client.postgrest["likes_db"].select().decodeList<LikeEntity>()
    suspend fun insertLike(like: LikeEntity) = client.postgrest["likes_db"].insert(like)
    suspend fun deleteLike(postId: String, userId: Int) { client.postgrest["likes_db"].delete { filter { eq("post_id", postId); eq("user_id", userId) } } }

    // --- RESEÑAS ---
    suspend fun getAllReviews(): List<ReviewEntity> = client.postgrest["reviews_db"].select().decodeList<ReviewEntity>()
    suspend fun upsertReview(review: ReviewEntity) { client.postgrest["reviews_db"].upsert(review) }
    suspend fun deleteReview(coffeeId: String, userId: Int) {
        client.postgrest["reviews_db"].delete {
            filter { eq("coffee_id", coffeeId); eq("user_id", userId) }
        }
    }

    // --- FAVORITOS ---
    suspend fun getAllFavorites(): List<LocalFavorite> = client.postgrest["local_favorites"].select().decodeList<LocalFavorite>()
    suspend fun insertFavorite(favorite: LocalFavorite) { client.postgrest["local_favorites"].insert(favorite) }
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

    suspend fun insertDiaryEntry(entry: DiaryEntryEntity) {
        // Usamos el DTO para evitar el error de Any y dejar que Supabase asigne el ID
        val insertData = DiaryEntryInsert(
            userId = entry.userId,
            coffeeId = entry.coffeeId,
            coffeeName = entry.coffeeName,
            coffeeBrand = entry.coffeeBrand,
            caffeineAmount = entry.caffeineAmount,
            amountMl = entry.amountMl,
            coffeeGrams = entry.coffeeGrams,
            preparationType = entry.preparationType,
            timestamp = entry.timestamp,
            type = entry.type
        )
        client.postgrest["diary_entries"].insert(insertData)
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
        client.postgrest["notifications_db"].insert(notification)
    }
}
