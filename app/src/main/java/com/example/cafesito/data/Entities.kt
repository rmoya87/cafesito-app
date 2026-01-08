package com.example.cafesito.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "origins")
data class Origin(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val countryName: String,
    val region: String? = null,
    val continent: String // Enum as String
)

@Entity(tableName = "score_sources")
data class ScoreSource(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String // Link to methodology
)

@Entity(
    tableName = "coffees",
    foreignKeys = [
        ForeignKey(entity = Origin::class, parentColumns = ["id"], childColumns = ["originId"]),
        ForeignKey(entity = ScoreSource::class, parentColumns = ["id"], childColumns = ["scoreSourceId"])
    ],
    indices = [
        androidx.room.Index(value = ["originId"]),
        androidx.room.Index(value = ["scoreSourceId"])
    ]
)
data class Coffee(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val brandRoaster: String,
    val originId: Int,
    val process: String, // Enum as String: Washed, Natural, Honey...
    val roastLevel: String, // Enum as String: Light, Medium, Dark
    val description: String,
    val imageUrl: String,
    val officialScore: Double,
    val scoreSourceId: Int
)

@Entity(
    tableName = "sensory_profiles",
    foreignKeys = [
        ForeignKey(entity = Coffee::class, parentColumns = ["id"], childColumns = ["coffeeId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class SensoryProfile(
    @PrimaryKey val coffeeId: Int, // 1:1 relationship, so PK is same as FK
    val aroma: Float,
    val flavor: Float,
    val body: Float,
    val acidity: Float,
    val aftertaste: Float,
    val balance: Float,
    val sweetness: Float,
    val clarity: Float,
    val freshness: Float
)

@Entity(
    tableName = "local_favorites",
    foreignKeys = [
        ForeignKey(entity = Coffee::class, parentColumns = ["id"], childColumns = ["coffeeId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class LocalFavorite(
    @PrimaryKey val coffeeId: Int,
    val savedAt: Long
)

@Entity(tableName = "users_db")
data class UserEntity(
    @PrimaryKey val id: Int,
    val username: String,
    val fullName: String,
    val avatarUrl: String,
    val email: String,
    val bio: String?
)

@Entity(
    tableName = "follows",
    primaryKeys = ["followerId", "followedId"],
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["followerId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["followedId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        androidx.room.Index(value = ["followerId"]),
        androidx.room.Index(value = ["followedId"])
    ]
)
data class FollowEntity(
    val followerId: Int,
    val followedId: Int,
    val createdAt: Long = System.currentTimeMillis()
)

data class CoffeeWithDetails(
    @Embedded val coffee: Coffee,
    @Relation(
        parentColumn = "originId",
        entityColumn = "id"
    )
    val origin: Origin?,
    @Relation(
        parentColumn = "scoreSourceId",
        entityColumn = "id"
    )
    val scoreSource: ScoreSource?,
    @Relation(
        parentColumn = "id",
        entityColumn = "coffeeId"
    )
    val sensoryProfile: SensoryProfile?
)
