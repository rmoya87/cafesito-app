package com.cafesito.shared.data.local

interface GreetingCache {
    suspend fun getGreeting(userName: String): String?
    suspend fun saveGreeting(userName: String, greeting: String)
}
