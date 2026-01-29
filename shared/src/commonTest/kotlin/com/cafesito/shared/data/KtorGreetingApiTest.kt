package com.cafesito.shared.data

import com.cafesito.shared.data.remote.KtorGreetingApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorGreetingApiTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun fetchGreetingHitsMockServer() = runTest {
        var capturedRequest: HttpRequestData? = null
        val engine = MockEngine { request ->
            capturedRequest = request
            respond(
                content = "Hola Ana",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/plain"),
            )
        }
        val client = HttpClient(engine)
        val api = KtorGreetingApi(client, baseUrl = "https://mock.local")

        val greeting = api.fetchGreeting("Ana")

        assertEquals("Hola Ana", greeting)
        assertEquals(Url("https://mock.local/greeting?name=Ana"), capturedRequest?.url)
        client.close()
    }
}
