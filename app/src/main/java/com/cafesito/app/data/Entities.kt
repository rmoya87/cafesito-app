package com.cafesito.app.data

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- ENTIDAD DE SESIÓN ---

@Entity(tableName = "active_session")
data class ActiveSessionEntity(
    @PrimaryKey val id: Int = 1, // Fila única
    val userId: Int
)


// --- ENTIDADES PRINCIPALES ---

@Immutable
@Serializable
@Entity(tableName = "coffees")
data class Coffee(
    @PrimaryKey val id: String,
    val especialidad: String? = null,
    val marca: String = "",
    @SerialName("pais_origen") val paisOrigen: String? = null,
    @SerialName("variedad_tipo") val variedadTipo: String? = null,
    val nombre: String = "",
    val descripcion: String = "",
    @SerialName("fuente_puntuacion") val fuentePuntuacion: String? = null,
    @SerialName("puntuacion_oficial") val puntuacionOficial: Double? = null,
    @SerialName("notas_cata") val notasCata: String = "",
    val formato: String = "",
    val cafeina: String = "",
    val tueste: String = "",
    val proceso: String = "",
    @SerialName("ratio_recomendado") val ratioRecomendado: String? = null,
    @SerialName("molienda_recomendada") val moliendaRecomendada: String = "",
    val aroma: Float = 0f,
    val sabor: Float = 0f,
    val retrogusto: Float = 0f,
    val acidez: Float = 0f,
    val cuerpo: Float = 0f,
    val uniformidad: Float = 0f,
    val dulzura: Float = 0f,
    @SerialName("puntuacion_total") val puntuacionTotal: Double = 0.0,
    @SerialName("codigo_barras") val codigoBarras: String? = null,
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("product_url") val productUrl: String = "",
    @SerialName("is_custom") val isCustom: Boolean = false,
    @SerialName("user_id") val userId: Int? = null
)

@Serializable
@Entity(tableName = "users_db")
data class UserEntity(
    @PrimaryKey val id: Int,
    @SerialName("google_id") val googleId: String? = null,
    val username: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("avatar_url") val avatarUrl: String,
    val email: String,
    val bio: String?
)

@Serializable
@Entity(
    tableName = "user_fcm_tokens",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["userId"])]
)
data class UserTokenEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @SerialName("user_id") val userId: Int,
    @SerialName("fcm_token") val fcmToken: String
)

@Serializable
data class UserTokenUpsert(
    @SerialName("user_id") val userId: Int,
    @SerialName("fcm_token") val fcmToken: String
)

@Serializable
@Entity(tableName = "posts_db")
data class PostEntity(
    @PrimaryKey val id: String,
    @SerialName("user_id") val userId: Int,
    @SerialName("image_url") val imageUrl: String,
    val comment: String,
    val timestamp: Long
)

@Serializable
@Entity(
    tableName = "post_coffee_tags",
    foreignKeys = [
        ForeignKey(entity = PostEntity::class, parentColumns = ["id"], childColumns = ["postId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["coffeeId"])]
)
data class PostCoffeeTagEntity(
    @PrimaryKey
    @SerialName("post_id") val postId: String,
    @SerialName("coffee_id") val coffeeId: String,
    @SerialName("coffee_name") val coffeeName: String,
    @SerialName("coffee_brand") val coffeeBrand: String,
    @SerialName("coffee_image_url") val coffeeImageUrl: String,
    @SerialName("coffee_rating") val coffeeRating: Float? = null
)

@Serializable
@Entity(tableName = "likes_db", primaryKeys = ["postId", "userId"])
data class LikeEntity(
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: Int
)

@Serializable
@Entity(tableName = "comments_db")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: Int,
    val text: String,
    val timestamp: Long
)

@Serializable
@Entity(tableName = "notifications_db")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @SerialName("user_id") val userId: Int,
    val type: String,
    @SerialName("from_username") val fromUsername: String,
    val message: String,
    val timestamp: Long,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("related_id") val relatedId: String? = null
)

@Serializable
@Entity(tableName = "follows", primaryKeys = ["followerId", "followedId"])
data class FollowEntity(
    @SerialName("follower_id") val followerId: Int,
    @SerialName("followed_id") val followedId: Int,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "reviews_db")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @SerialName("coffee_id") val coffeeId: String,
    @SerialName("user_id") val userId: Int,
    val rating: Float,
    val comment: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val aroma: Float? = null,
    val sabor: Float? = null,
    val cuerpo: Float? = null,
    val acidez: Float? = null,
    val dulzura: Float? = null,
    val timestamp: Long,
    val method: String? = null,
    val ratio: String? = null,
    @SerialName("water_temp") val waterTemp: Int? = null,
    @SerialName("extraction_time") val extractionTime: String? = null,
    @SerialName("grind_size") val grindSize: String? = null
)


@Serializable
@Entity(tableName = "coffee_sensory_profiles", primaryKeys = ["coffeeId", "userId"])
data class CoffeeSensoryProfileEntity(
    @SerialName("coffee_id") val coffeeId: String,
    @SerialName("user_id") val userId: Int,
    val aroma: Float,
    val sabor: Float,
    val cuerpo: Float,
    val acidez: Float,
    val dulzura: Float,
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "local_favorites", primaryKeys = ["coffeeId", "userId"])
data class LocalFavorite(
    @SerialName("coffee_id") val coffeeId: String,
    @SerialName("user_id") val userId: Int,
    @SerialName("saved_at") val savedAt: Long = System.currentTimeMillis()
)

/** Lista personalizada del usuario (tabla user_lists en Supabase). */
@Serializable
data class UserListRow(
    val id: String,
    @SerialName("user_id") val userId: Long,
    val name: String,
    @SerialName("is_public") val isPublic: Boolean,
    @SerialName("created_at") val createdAt: String? = null
)

/** Ítem de lista (coffee_id en user_list_items). */
@Serializable
data class UserListItemRow(
    @SerialName("coffee_id") val coffeeId: String
)

/** Ítem de lista con metadatos para feed de actividad (solo listas públicas en perfiles ajenos). */
data class ListItemActivityRow(
    val listId: String,
    val listName: String,
    val isPublic: Boolean,
    val coffeeId: String,
    val createdAt: Long
)

@Serializable
data class UserListInsert(
    @SerialName("user_id") val userId: Long,
    val name: String,
    @SerialName("is_public") val isPublic: Boolean
)

@Serializable
data class UserListItemInsert(
    @SerialName("list_id") val listId: String,
    @SerialName("coffee_id") val coffeeId: String
)

@Serializable
@Entity(
    tableName = "diary_entries",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["userId"])]
)
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @SerialName("user_id") val userId: Int,
    @SerialName("coffee_id") val coffeeId: String? = null,
    @SerialName("coffee_name") val coffeeName: String,
    @SerialName("coffee_brand") val coffeeBrand: String = "",
    @SerialName("caffeine_mg") val caffeineAmount: Int,
    @SerialName("amount_ml") val amountMl: Int = 250,
    @SerialName("coffee_grams") val coffeeGrams: Int = 15,
    @SerialName("preparation_type") val preparationType: String = "Espresso",
    @SerialName("size_label") val sizeLabel: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "CUP",
    @SerialName("external_id") val externalId: String? = null
)

@Entity(tableName = "pending_diary_sync")
data class PendingDiarySyncEntity(
    @PrimaryKey val localEntryId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long? = null,
    val retryCount: Int = 0,
    val lastError: String? = null
)

@Serializable
@Entity(tableName = "pantry_items")
data class PantryItemEntity(
    @PrimaryKey @SerialName("id") val id: String,
    @SerialName("coffee_id") val coffeeId: String,
    @SerialName("user_id") val userId: Int,
    @SerialName("grams_remaining") val gramsRemaining: Int,
    @SerialName("total_grams") val totalGrams: Int,
    @SerialName("last_updated") val lastUpdated: Long = System.currentTimeMillis()
)

/** Café marcado como "terminado" desde la despensa del diario. Ordenado por fecha de finalización. */
@Serializable
@Entity(
    tableName = "finished_coffees",
    indices = [Index(value = ["userId"]), Index(value = ["finished_at"])]
)
data class FinishedCoffeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @SerialName("user_id") val userId: Int,
    @SerialName("coffee_id") val coffeeId: String,
    @ColumnInfo(name = "finished_at") @SerialName("finished_at") val finishedAtMs: Long = System.currentTimeMillis()
)

/** Inserción en Supabase pantry_historical (sin id; la tabla usa identity). */
@Serializable
data class FinishedCoffeeInsert(
    @SerialName("user_id") val userId: Int,
    @SerialName("coffee_id") val coffeeId: String,
    @SerialName("finished_at") val finishedAtMs: Long
)

// --- CLASES AUXILIARES PARA INSERCIÓN (NO SON ENTIDADES) ---

/**
 * IMPORTANTE: Los campos técnicos (method, extraction_time, etc.) se han omitido
 * temporalmente de esta clase para evitar errores de esquema en Supabase.
 */
@Serializable
data class ReviewInsert(
    val id: Long? = null,
    @SerialName("coffee_id") val coffeeId: String,
    @SerialName("user_id") val userId: Int,
    val rating: Float,
    val comment: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val aroma: Float? = null,
    val sabor: Float? = null,
    val cuerpo: Float? = null,
    val acidez: Float? = null,
    val dulzura: Float? = null,
    val timestamp: Long
)

@Serializable
data class DiaryEntryInsert(
    @SerialName("user_id") val userId: Int,
    @SerialName("coffee_id") val coffeeId: String? = null,
    @SerialName("coffee_name") val coffeeName: String,
    @SerialName("coffee_brand") val coffeeBrand: String = "",
    @SerialName("caffeine_mg") val caffeineAmount: Int,
    @SerialName("amount_ml") val amountMl: Int,
    @SerialName("coffee_grams") val coffeeGrams: Int,
    @SerialName("preparation_type") val preparationType: String,
    @SerialName("size_label") val sizeLabel: String? = null,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("type") val type: String
)

@Serializable
data class CommentInsert(
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: Int,
    val text: String,
    val timestamp: Long
)

// --- CLASES DE RELACIÓN (POJOs para UI y Repositorios) ---

@Immutable
data class PantryItemWithDetails(
    val pantryItem: PantryItemEntity,
    val coffee: Coffee,
    val isCustom: Boolean = false
)

@Immutable
data class FinishedCoffeeWithDetails(
    val finishedAtMs: Long,
    val coffee: Coffee
)

@Immutable
data class UserReviewInfo(
    val coffeeDetails: CoffeeWithDetails, 
    val review: ReviewEntity, 
    val authorName: String?, 
    val authorAvatarUrl: String?
)

/** Item del feed de actividad en perfil: opinión, primera vez café o añadido a lista pública. */
@Immutable
sealed class ProfileActivityItem {
    abstract val userId: Int
    abstract val userName: String
    abstract val avatarUrl: String?
    abstract val timestamp: Long

    data class Review(val reviewInfo: UserReviewInfo) : ProfileActivityItem() {
        override val userId: Int get() = reviewInfo.review.userId
        override val userName: String get() = reviewInfo.authorName ?: ""
        override val avatarUrl: String? get() = reviewInfo.authorAvatarUrl
        override val timestamp: Long get() = reviewInfo.review.timestamp
    }

    data class FirstTimeCoffee(
        override val userId: Int,
        override val userName: String,
        override val avatarUrl: String?,
        override val timestamp: Long,
        val coffeeId: String,
        val coffeeName: String
    ) : ProfileActivityItem()

    data class AddedToList(
        override val userId: Int,
        override val userName: String,
        override val avatarUrl: String?,
        override val timestamp: Long,
        val coffeeId: String,
        val coffeeName: String,
        val listId: String,
        val listName: String
    ) : ProfileActivityItem()
}

@Immutable
data class CoffeeWithDetails(
    @Embedded val coffee: Coffee,
    @Relation(parentColumn = "id", entityColumn = "coffeeId")
    val favorite: LocalFavorite? = null,
    @Relation(parentColumn = "id", entityColumn = "coffeeId")
    val reviews: List<ReviewEntity> = emptyList()
) {
    val isFavorite: Boolean get() = favorite != null
    val averageRating: Float get() = if (reviews.isEmpty()) 0.0f else reviews.map { it.rating }.average().toFloat()
}

@Immutable
data class PostWithDetails(
    @Embedded val post: PostEntity,
    @Relation(parentColumn = "userId", entityColumn = "id")
    val author: UserEntity?, // CAMBIO: Puede ser null durante logout
    @Relation(parentColumn = "id", entityColumn = "postId")
    val likes: List<LikeEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "postId")
    val comments: List<CommentEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "postId")
    val coffeeTag: PostCoffeeTagEntity? = null
)

@Immutable
data class CommentWithAuthor(
    @Embedded val comment: CommentEntity,
    @Relation(parentColumn = "userId", entityColumn = "id")
    val author: UserEntity? // CAMBIO: Puede ser null
)

@Immutable
data class ReviewWithAuthor(
    @Embedded val review: ReviewEntity,
    @Relation(parentColumn = "userId", entityColumn = "id")
    val author: UserEntity? // CAMBIO: Puede ser null
)
