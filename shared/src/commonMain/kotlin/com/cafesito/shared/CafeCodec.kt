package com.cafesito.shared

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CafeProfile(
    val name: String,
    val rating: Int,
)

class CafeCodec(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun encode(profile: CafeProfile): String = withContext(Dispatchers.Default) {
        json.encodeToString(CafeProfile.serializer(), profile)
    }

    suspend fun decode(payload: String): CafeProfile = withContext(Dispatchers.Default) {
        json.decodeFromString(CafeProfile.serializer(), payload)
    }
}
