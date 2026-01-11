package com.example.cafesito.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "coffees")
data class Coffee(
    @PrimaryKey val id: String,
    val especialidad: String,
    val marca: String,
    val paisOrigen: String,
    val variedadTipo: String,
    val nombre: String,
    val descripcion: String,
    val fuentePuntuacion: String,
    val puntuacionOficial: Double,
    val notasCata: String,
    val formato: String,
    val cafeina: String,
    val tueste: String,
    val proceso: String,
    val ratioRecomendado: String,
    val moliendaRecomendada: String,
    val aroma: Float,
    val sabor: Float,
    val retrogusto: Float,
    val acidez: Float,
    val cuerpo: Float,
    val uniformidad: Float,
    val dulzura: Float,
    val puntuacionTotal: Double,
    val codigoBarras: String,
    val imageUrl: String,
    val productUrl: String
)

@Entity(
    tableName = "local_favorites",
    foreignKeys = [
        ForeignKey(entity = Coffee::class, parentColumns = ["id"], childColumns = ["coffeeId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["coffeeId"])]
)
data class LocalFavorite(
    @PrimaryKey val coffeeId: String,
    val savedAt: Long
)

@Entity(
    tableName = "reviews_db",
    foreignKeys = [
        ForeignKey(entity = Coffee::class, parentColumns = ["id"], childColumns = ["coffeeId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index(value = ["coffeeId", "userId"], unique = true),
        Index(value = ["userId"])
    ]
)
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val coffeeId: String,
    val userId: Int,
    val rating: Float,
    val comment: String,
    val imageUrl: String? = null,
    val timestamp: Long
)

@Entity(tableName = "users_db")
data class UserEntity(
    @PrimaryKey val id: Int,
    val googleId: String? = null,
    val username: String,
    val fullName: String,
    val avatarUrl: String,
    val email: String,
    val bio: String?
)

@Entity(
    tableName = "posts_db",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["userId"])]
)
data class PostEntity(
    @PrimaryKey val id: String,
    val userId: Int,
    val imageUrl: String,
    val comment: String,
    val timestamp: Long
)

@Entity(
    tableName = "comments_db",
    foreignKeys = [
        ForeignKey(entity = PostEntity::class, parentColumns = ["id"], childColumns = ["postId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["postId"]), Index(value = ["userId"])]
)
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: String,
    val userId: Int,
    val text: String,
    val timestamp: Long
)

@Entity(
    tableName = "likes_db",
    primaryKeys = ["postId", "userId"],
    foreignKeys = [
        ForeignKey(entity = PostEntity::class, parentColumns = ["id"], childColumns = ["postId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["userId"])]
)
data class LikeEntity(
    val postId: String,
    val userId: Int
)

@Entity(
    tableName = "follows",
    primaryKeys = ["followerId", "followedId"],
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["followerId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["followedId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["followerId"]), Index(value = ["followedId"])]
)
data class FollowEntity(
    val followerId: Int,
    val followedId: Int,
    val createdAt: Long = System.currentTimeMillis()
)

// --- CLASES DE RELACIÓN PARA EL TIMELINE REAL ---

data class PostWithDetails(
    @Embedded val post: PostEntity,
    @Relation(
        parentColumn = "userId",
        entityColumn = "id"
    )
    val author: UserEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "postId"
    )
    val likes: List<LikeEntity> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "postId"
    )
    val comments: List<CommentEntity> = emptyList()
)

data class ReviewWithAuthor(
    @Embedded val review: ReviewEntity,
    @Relation(
        parentColumn = "userId",
        entityColumn = "id"
    )
    val author: UserEntity
)

data class CoffeeWithDetails(
    @Embedded val coffee: Coffee,
    @Relation(
        parentColumn = "id",
        entityColumn = "coffeeId"
    )
    val favorite: LocalFavorite? = null,
    @Relation(
        parentColumn = "id",
        entityColumn = "coffeeId"
    )
    val reviews: List<ReviewEntity> = emptyList()
) {
    val isFavorite: Boolean get() = favorite != null
    val averageRating: Float get() = if (reviews.isEmpty()) 0.0f else reviews.map { it.rating }.average().toFloat()

    @Ignore val origin: Origin? = null 
    @Ignore val sensoryProfile: SensoryProfile? = null
}

data class Origin(val countryName: String, val continent: String)
data class SensoryProfile(val coffeeId: String, val aroma: Float, val flavor: Float, val body: Float, val acidity: Float, val aftertaste: Float)
