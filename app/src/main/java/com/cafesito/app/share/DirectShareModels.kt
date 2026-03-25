package com.cafesito.app.share

/**
 * Contrato base de destino de share directo para Android/Web.
 * Se alimentará desde backend en siguientes iteraciones.
 */
data class DirectShareTarget(
    val id: String,
    val type: DirectShareTargetType,
    val label: String,
    val deepLink: String,
    val rankScore: Double,
    val avatarUrl: String? = null
)

enum class DirectShareTargetType {
    LIST,
    CONTACT
}
