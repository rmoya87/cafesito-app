package com.cafesito.shared.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserTokenEntity(
    val id: Long = 0,
    @SerialName("user_id") val userId: Int,
    @SerialName("fcm_token") val fcmToken: String
)
