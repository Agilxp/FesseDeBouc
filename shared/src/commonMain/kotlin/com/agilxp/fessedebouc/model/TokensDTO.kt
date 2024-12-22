package com.agilxp.fessedebouc.model

import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
)

@Serializable
data class TokenData(
    val aud: String,
    val iss: String,
    val google_id: String,
    val user_id: String,
    val user_name: String,
    val user_email: String,
    val exp: Long,
)
