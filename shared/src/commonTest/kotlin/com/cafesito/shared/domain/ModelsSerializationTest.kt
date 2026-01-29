package com.cafesito.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class ModelsSerializationTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun userSerializesRoundTrip() {
        val user = User(
            id = 1,
            username = "cafeuser",
            fullName = "Cafe User",
            avatarUrl = "https://example.com/avatar.png",
            email = "cafe@example.com",
            bio = "Bio",
            favoriteCoffeeIds = listOf("coffee-1", "coffee-2")
        )

        val encoded = json.encodeToString(User.serializer(), user)
        val decoded = json.decodeFromString(User.serializer(), encoded)

        assertEquals(user, decoded)
    }

    @Test
    fun postSerializesRoundTrip() {
        val user = User(
            id = 2,
            username = "barista",
            fullName = "Cafe Barista",
            avatarUrl = "https://example.com/barista.png",
            email = "barista@example.com",
            bio = null
        )
        val post = Post(
            id = "post-1",
            user = user,
            imageUrl = "https://example.com/post.png",
            comment = "Buen café",
            timestamp = 1_700_000_000_000,
            initialLikes = 42,
            comments = listOf(Comment(user = user, text = "Genial"))
        )

        val encoded = json.encodeToString(Post.serializer(), post)
        val decoded = json.decodeFromString(Post.serializer(), encoded)

        assertEquals(post, decoded)
    }

    @Test
    fun reviewSerializesRoundTrip() {
        val user = User(
            id = 3,
            username = "reviewer",
            fullName = "Cafe Reviewer",
            avatarUrl = "https://example.com/reviewer.png",
            email = "reviewer@example.com",
            bio = "Café lover"
        )
        val review = Review(
            user = user,
            coffeeId = "coffee-3",
            rating = 4.5f,
            comment = "Excelente",
            imageUrl = "https://example.com/review.png",
            timestamp = 1_700_000_000_100
        )

        val encoded = json.encodeToString(Review.serializer(), review)
        val decoded = json.decodeFromString(Review.serializer(), encoded)

        assertEquals(review, decoded)
    }

    @Test
    fun suggestedUserInfoSerializesRoundTrip() {
        val user = User(
            id = 4,
            username = "suggested",
            fullName = "Suggested User",
            avatarUrl = "https://example.com/suggested.png",
            email = "suggested@example.com",
            bio = null
        )
        val suggested = SuggestedUserInfo(
            user = user,
            followersCount = 10,
            followingCount = 5
        )

        val encoded = json.encodeToString(SuggestedUserInfo.serializer(), suggested)
        val decoded = json.decodeFromString(SuggestedUserInfo.serializer(), encoded)

        assertEquals(suggested, decoded)
    }
}
