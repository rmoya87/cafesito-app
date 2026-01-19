package com.example.cafesito.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "coffees")
data class Coffee(
    @PrimaryKey val id: String,
    val especialidad: String = "",
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
    @SerialName("product_url") val productUrl: String = ""
)

@Serializable
@Entity(tableName = "custom_coffees")
data class CustomCoffeeEntity(
    @PrimaryKey val id: String,
    @SerialName("user_id") val userId: Int,
    val name: String,
    val brand: String,
    val specialty: String,
    val roast: String? = null,
    val variety: String? = null,
    val country: String,
    @SerialName("has_caffeine") val hasCaffeine: Boolean,
    val format: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("total_grams") val totalGrams: Int = 250
) {
    fun toCoffee(): Coffee = Coffee(
        id = id,
        nombre = name,
        marca = brand,
        especialidad = specialty,
        tueste = roast ?: "",
        variedadTipo = variety,
        paisOrigen = country,
        cafeina = if (hasCaffeine) "Sí" else "No",
        formato = format,
        imageUrl = imageUrl
    )
}

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
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "CUP"
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
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("type") val type: String
)

@Serializable
@Entity(tableName = "pantry_items", primaryKeys = ["coffeeId", "userId"])
data class PantryItemEntity(
    @SerialName("coffee_id") val coffeeId: String,
    @SerialName("user_id") val userId: Int,
    @SerialName("grams_remaining") val gramsRemaining: Int,
    @SerialName("total_grams") val totalGrams: Int,
    @SerialName("last_updated") val lastUpdated: Long = System.currentTimeMillis()
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
@Entity(tableName = "posts_db")
data class PostEntity(
    @PrimaryKey val id: String,
    @SerialName("user_id") val userId: Int,
    @SerialName("image_url") val imageUrl: String,
    val comment: String,
    val timestamp: Long
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

data class PostWithDetails(
    @Embedded val post: PostEntity,
    @Relation(parentColumn = "userId", entityColumn = "id")
    val author: UserEntity,
    @Relation(parentColumn = "id", entityColumn = "postId")
    val likes: List<LikeEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "postId")
    val comments: List<CommentEntity> = emptyList()
)

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

@Serializable
@Entity(tableName = "local_favorites", primaryKeys = ["coffeeId", "userId"])
data class LocalFavorite(
    @SerialName("coffee_id") val coffeeId: String,
    @SerialName("user_id") val userId: Int,
    @SerialName("saved_at") val savedAt: Long = System.currentTimeMillis()
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
    val timestamp: Long,
    val method: String? = null,
    val ratio: String? = null,
    @SerialName("water_temp") val waterTemp: Int? = null,
    @SerialName("extraction_time") val extractionTime: String? = null,
    @SerialName("grind_size") val grindSize: String? = null
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

data class PantryItemWithDetails(
    val pantryItem: PantryItemEntity,
    val coffee: Coffee,
    val isCustom: Boolean = false
)

data class UserReviewInfo(
    val coffeeDetails: CoffeeWithDetails, 
    val review: ReviewEntity, 
    val authorName: String?, 
    val authorAvatarUrl: String?
)

data class CommentWithAuthor(
    @Embedded val comment: CommentEntity,
    @Relation(parentColumn = "userId", entityColumn = "id")
    val author: UserEntity
)

data class ReviewWithAuthor(
    @Embedded val review: ReviewEntity,
    @Relation(parentColumn = "userId", entityColumn = "id")
    val author: UserEntity
)
