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

    // --- REALTIME LIKES --- (Corregido para v2.x)
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
    suspend fun upsertUser(user: UserEntity) { client.postgrest["users_db"].upsert(user) }

    // --- SEGUIMIENTOS ---
    suspend fun getAllFollows(): List<FollowEntity> = client.postgrest["follows"].select().decodeList<FollowEntity>()
    suspend fun insertFollow(follow: FollowEntity) { client.postgrest["follows"].insert(follow) }
    suspend fun deleteFollow(followerId: Int, followedId: Int) { client.postgrest["follows"].delete { filter { eq("follower_id", followerId); eq("followed_id", followedId) } } }

    // --- CAFÉS ---
    suspend fun getAllCoffees(): List<Coffee> = client.postgrest["coffees"].select { order("nombre", Order.ASCENDING) }.decodeList<Coffee>()
    suspend fun upsertCoffees(coffees: List<Coffee>) { client.postgrest["coffees"].upsert(coffees) }

    // --- PUBLICACIONES ---
    suspend fun getAllPosts(): List<PostEntity> = client.postgrest["posts_db"].select { order("timestamp", Order.DESCENDING) }.decodeList<PostEntity>()
    suspend fun insertPost(post: PostEntity) = client.postgrest["posts_db"].insert(post)

    // --- COMENTARIOS ---
    suspend fun getCommentsForPost(postId: String): List<CommentEntity> = client.postgrest["comments_db"].select { filter { eq("post_id", postId) }; order("timestamp", Order.ASCENDING) }.decodeList<CommentEntity>()
    suspend fun insertComment(comment: CommentEntity) = client.postgrest["comments_db"].insert(comment)

    // --- LIKES ---
    suspend fun getAllLikes(): List<LikeEntity> = client.postgrest["likes_db"].select().decodeList<LikeEntity>()
    suspend fun insertLike(like: LikeEntity) = client.postgrest["likes_db"].insert(like)
    suspend fun deleteLike(postId: String, userId: Int) { client.postgrest["likes_db"].delete { filter { eq("post_id", postId); eq("user_id", userId) } } }

    // --- NOTIFICACIONES ---
    suspend fun insertNotification(notification: NotificationEntity) {
        client.postgrest["notifications_db"].insert(notification)
    }
}
