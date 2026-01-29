package com.cafesito.app.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class ModelSerializationTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun userSerializesAndDeserializes() {
        val user = User(
            id = 1,
            username = "cafe_user",
            fullName = "Cafe User",
            avatarUrl = "https://example.com/avatar.png",
            email = "user@example.com",
            bio = "Bio",
            favoriteCoffeeIds = listOf("coffee-1", "coffee-2")
        )

        val encoded = json.encodeToString(User.serializer(), user)
        val decoded = json.decodeFromString(User.serializer(), encoded)

        assertEquals(user, decoded)
    }

    @Test
    fun commentSerializesAndDeserializes() {
        val user = User(
            id = 2,
            username = "commenter",
            fullName = "Commenter",
            avatarUrl = "https://example.com/avatar2.png",
            email = "commenter@example.com",
            bio = null
        )
        val comment = Comment(user = user, text = "Buen cafe")

        val encoded = json.encodeToString(Comment.serializer(), comment)
        val decoded = json.decodeFromString(Comment.serializer(), encoded)

        assertEquals(comment, decoded)
    }

    @Test
    fun postSerializesAndDeserializes() {
        val user = User(
            id = 3,
            username = "poster",
            fullName = "Poster",
            avatarUrl = "https://example.com/avatar3.png",
            email = "poster@example.com",
            bio = "Loves espresso"
        )
        val comment = Comment(user = user, text = "Me gusta")
        val post = Post(
            id = "post-1",
            user = user,
            imageUrl = "https://example.com/post.png",
            comment = "Mi cafe favorito",
            timestamp = 1700000000000,
            initialLikes = 10,
            comments = listOf(comment)
        )

        val encoded = json.encodeToString(Post.serializer(), post)
        val decoded = json.decodeFromString(Post.serializer(), encoded)

        assertEquals(post, decoded)
    }

    @Test
    fun reviewSerializesAndDeserializes() {
        val user = User(
            id = 4,
            username = "reviewer",
            fullName = "Reviewer",
            avatarUrl = "https://example.com/avatar4.png",
            email = "reviewer@example.com",
            bio = ""
        )
        val review = Review(
            user = user,
            coffeeId = "coffee-99",
            rating = 4.5f,
            comment = "Excelente",
            timestamp = 1700000001234
        )

        val encoded = json.encodeToString(Review.serializer(), review)
        val decoded = json.decodeFromString(Review.serializer(), encoded)

        assertEquals(review, decoded)
    }

    @Test
    fun suggestedUserInfoSerializesAndDeserializes() {
        val user = User(
            id = 5,
            username = "suggested",
            fullName = "Suggested",
            avatarUrl = "https://example.com/avatar5.png",
            email = "suggested@example.com",
            bio = null
        )
        val suggested = SuggestedUserInfo(
            user = user,
            followersCount = 120,
            followingCount = 42
        )

        val encoded = json.encodeToString(SuggestedUserInfo.serializer(), suggested)
        val decoded = json.decodeFromString(SuggestedUserInfo.serializer(), encoded)

        assertEquals(suggested, decoded)
    }
}
