package com.cafesito.shared.domain.repository

interface GreetingRepository {
    suspend fun fetchGreeting(userName: String): String
}
