package com.cafesito.shared.data.remote

interface GreetingApi {
    suspend fun fetchGreeting(userName: String): String
}
