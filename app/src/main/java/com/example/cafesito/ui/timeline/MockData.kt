package com.example.cafesito.ui.timeline

import androidx.compose.runtime.mutableStateListOf
import com.example.cafesito.ui.detail.User
import java.util.Date

data class Post(
    val id: Int,
    val user: User,
    val imageUrl: String,
    val comment: String,
    val timestamp: Date,
    var initialLikes: Int,
    val comments: MutableList<Comment> = mutableStateListOf()
)

data class Comment(
    val user: User,
    val text: String
)

val currentUser = User(1, "Ricardo Moya", "https://i.pravatar.cc/150?u=a042581f4e29026704d")

val samplePosts = mutableStateListOf(
    Post(
        id = 1,
        user = User(2, "Anaïs", "https://i.pravatar.cc/150?u=a042581f4e29026705d"),
        imageUrl = "https://images.pexels.com/photos/312418/pexels-photo-312418.jpeg",
        comment = "¡Empezando el día con un buen café! ☕️ #café #mañana",
        timestamp = Date(),
        initialLikes = 132,
        comments = mutableStateListOf(
            Comment(User(3, "Gemma", ""), "¡Qué buena pinta!"),
            Comment(currentUser, "Yo también quiero uno así.")
        )
    ),
    Post(
        id = 2,
        user = User(3, "Gemma", "https://i.pravatar.cc/150?u=a042581f4e29026706d"),
        imageUrl = "https://images.pexels.com/photos/1695052/pexels-photo-1695052.jpeg",
        comment = "Descubriendo nuevas cafeterías por la ciudad. Esta tiene un encanto especial.",
        timestamp = Date(System.currentTimeMillis() - 86400000L * 2), // 2 days ago
        initialLikes = 89
    ),
    Post(
        id = 3,
        user = currentUser,
        imageUrl = "https://images.pexels.com/photos/4109744/pexels-photo-4109744.jpeg",
        comment = "Nada como un espresso doble para recargar las pilas a media tarde.",
        timestamp = Date(System.currentTimeMillis() - 86400000L * 5), // 5 days ago
        initialLikes = 245,
        comments = mutableStateListOf(
             Comment(User(2, "Anaïs", ""), "¡Totalmente de acuerdo!")
        )
    )
)
