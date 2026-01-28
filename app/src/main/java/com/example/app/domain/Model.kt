package com.cafesito.app.domain

// --- Data Models ---

data class User(
    val id: Int,
    val username: String,
    val fullName: String,
    val avatarUrl: String,
    val email: String, 
    val bio: String?,
    val favoriteCoffeeIds: List<String> = emptyList() // Changed to String
)

data class Post(
    val id: String,
    val user: User,
    val imageUrl: String,
    val comment: String,
    val timestamp: Long,
    val initialLikes: Int,
    val comments: List<Comment>
)

data class Comment(val user: User, val text: String)

data class Review(
    val user: User, 
    val coffeeId: String, // Changed to String
    val rating: Float, 
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SuggestedUserInfo(
    val user: User,
    val followersCount: Int,
    val followingCount: Int
)
