package com.example.cafesito.domain

import androidx.compose.runtime.mutableStateOf

// --- Mock User Data ---
val currentUser = User(1, "rmoya", "Ricardo Moya", "https://rmoya.dev/assets/images/profile.jpg", "ricardo.moya@example.com", "Desarrollador Android y creador de contenido.")

val allUsers = mutableListOf(currentUser).apply {
    addAll(List(24) { i ->
        User(i + 2, "usuario${i+1}", "Usuario ${i + 1}", "https://i.pravatar.cc/150?u=usuario${i+1}@cafesito.com", "usuario${i+1}@example.com", "A GDE from Spain")
    })
}

// --- Mock Review Data ---
val sampleReviews = mutableStateOf(List(23) { i ->
    val user = if (i == 15) currentUser else allUsers.first { it.id != currentUser.id }
    Review(user, (3.5f + (i % 15) / 10f), "Este es el comentario de ejemplo número ${i + 1}. El café es muy bueno.")
})

// --- Mock Post Data ---
val samplePosts = mutableListOf(
    *List(15) { i ->
    val user = allUsers[i % allUsers.size]
    Post(
        id = "post$i",
        user = user,
        imageUrl = "https://picsum.photos/seed/${i+30}/800/600",
        comment = "Disfrutando de un café increíble. #${i+1}",
        timestamp = System.currentTimeMillis() - (i * 3600000L), // i hours ago
        initialLikes = (10..100).random(),
        comments = emptyList()
    )
}.toTypedArray()
)
