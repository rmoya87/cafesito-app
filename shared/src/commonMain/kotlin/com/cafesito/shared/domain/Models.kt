package com.cafesito.shared.domain

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val username: String,
    val fullName: String,
    val avatarUrl: String,
    val email: String,
    val bio: String?,
    val favoriteCoffeeIds: List<String> = emptyList()
)

@Serializable
data class Post(
    val id: String,
    val user: User,
    val imageUrl: String,
    val comment: String,
    val timestamp: Long,
    val initialLikes: Int,
    val comments: List<Comment>
)

@Serializable
data class Comment(val user: User, val text: String)

@Serializable
data class Review(
    val user: User,
    val coffeeId: String,
    val rating: Float,
    val comment: String,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class SuggestedUserInfo(
    val user: User,
    val followersCount: Int,
    val followingCount: Int
)
