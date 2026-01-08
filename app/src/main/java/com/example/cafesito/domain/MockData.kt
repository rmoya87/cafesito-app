package com.example.cafesito.domain

import androidx.compose.runtime.mutableStateOf

// --- Mock User Data ---
val currentUser = User(
    id = 1, 
    username = "rmoya", 
    fullName = "Ricardo Moya", 
    avatarUrl = "https://rmoya.dev/assets/images/profile.jpg", 
    email = "ricardo.moya@example.com", 
    bio = "Desarrollador Android y creador de contenido.",
    favoriteCoffeeIds = listOf(1, 2, 3)
)

val allUsers = mutableListOf(currentUser).apply {
    addAll(List(24) { i ->
        User(
            id = i + 2, 
            username = "usuario${i+1}", 
            fullName = "Usuario ${i + 1}", 
            avatarUrl = "https://i.pravatar.cc/150?u=usuario${i+1}@cafesito.com", 
            email = "usuario${i+1}@example.com", 
            bio = "A GDE from Spain",
            favoriteCoffeeIds = if (i % 2 == 0) listOf(1, 4) else emptyList()
        )
    })
}

// --- Global Review Data ---
val sampleReviews = mutableStateOf(List(40) { i ->
    val user = allUsers[i % allUsers.size]
    // Distribute reviews across 5 mock coffee IDs
    val coffeeId = (i % 5) + 1 
    Review(user, coffeeId, (3.0f + (i % 20) / 10f), "Este café tiene un cuerpo excelente y un aroma persistente. Muy recomendado.")
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
