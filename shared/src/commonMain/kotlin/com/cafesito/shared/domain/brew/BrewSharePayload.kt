package com.cafesito.shared.domain.brew

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Contrato compartido para serializar/deserializar una elaboración y compartirla
 * (texto, enlace, deep link). Mismo formato en Android y WebApp.
 */
@Serializable
data class BrewSharePayload(
    val method: String,
    val coffeeId: String? = null,
    val coffeeName: String? = null,
    val waterMl: Int,
    val ratio: Double,
    @SerialName("espresso_sec") val espressoTimeSeconds: Int? = null
) {
    fun toJson(): String = Companion.toJsonString(this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun toJsonString(payload: BrewSharePayload): String =
            json.encodeToString(serializer(), payload)

        fun fromJson(jsonString: String): BrewSharePayload? = runCatching {
            json.decodeFromString(serializer(), jsonString)
        }.getOrNull()
    }
}
