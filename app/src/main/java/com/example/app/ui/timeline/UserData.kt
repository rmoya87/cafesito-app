package com.cafesito.app.ui.timeline

import com.cafesito.app.domain.User

// Mock current user
val currentUser = User(1, "rmoya", "Ricardo Moya", "https://rmoya.dev/assets/images/profile.jpg", "ricardo.moya@example.com", "Desarrollador Android y creador de contenido.")

// Mock list of all users
val allUsers = List(25) { i ->
    if (i == 0) currentUser
    else User(i + 1, "usuario${i}", "Usuario ${i}", "https://i.pravatar.cc/150?u=usuario${i}@cafesito.com", "usuario$i@example.com", "A GDE from Spain")
}
