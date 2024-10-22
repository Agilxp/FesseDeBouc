package com.agilxp.fessedebouc.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDTO(
    val name: String,
    val email: String,
    val googleId: String,
)
