package com.cafesito.shared.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText

class KtorGreetingApi(
    private val client: HttpClient,
    private val baseUrl: String,
) : GreetingApi {
    override suspend fun fetchGreeting(userName: String): String {
        return client.get("$baseUrl/greeting") {
            parameter("name", userName)
        }.bodyAsText()
    }
}
