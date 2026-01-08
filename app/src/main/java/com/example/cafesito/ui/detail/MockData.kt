package com.example.cafesito.ui.detail

import androidx.compose.runtime.mutableStateOf

// --- Placeholder Data & Logic for Reviews ---
data class User(val id: Int, val name: String, val avatarUrl: String)
data class Review(val user: User, val rating: Float, val comment: String)

// Mock current user
val currentUser = User(1, "Ricardo Moya", "")

val sampleReviews = mutableStateOf(List(23) { i ->
    val user = if (i == 15) currentUser else User(i + 2, "Usuario ${i + 1}", "")
    Review(user, (3.5f + (i % 15) / 10f), "Este es el comentario de ejemplo número ${i + 1}. El café es muy bueno.")
})
