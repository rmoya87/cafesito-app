package com.cafesito.shared.data.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ReviewDto(
    /**
     * El id se marca con EncodeDefault para que, si es null (inserción), 
     * NO se envíe en el JSON y no rompa la restricción not-null de Supabase.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: Long? = null,
    
    @SerialName("coffee_id") val coffeeId: String,
    @SerialName("user_id") val userId: Int,
    val rating: Float,
    val comment: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val aroma: Float? = null,
    val sabor: Float? = null,
    val cuerpo: Float? = null,
    val acidez: Float? = null,
    val dulzura: Float? = null,
    val timestamp: Long
    
    // IMPORTANTE: Se omiten campos técnicos que no existen en el esquema remoto actual
    // para evitar errores de mismatch de columnas.
)
