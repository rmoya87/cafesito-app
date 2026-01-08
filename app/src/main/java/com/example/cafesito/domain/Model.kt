package com.example.cafesito.domain

// --- Data Models ---

data class User(
    val id: Int,
    val username: String, // Added
    val fullName: String, // Renamed from 'name'
    val avatarUrl: String,
    val email: String, 
    val bio: String?
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

data class Review(val user: User, val rating: Float, val comment: String)
