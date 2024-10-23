package com.agilxp.fessedebouc.model

import com.agilxp.fessedebouc.util.UUIDSerializer
import io.ktor.server.auth.*
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class UserSession(
    val state: String,
    val accessToken: String,
    @Serializable(with = UUIDSerializer::class)
    val userId: UUID,
    val userEmail: String,
    val googleId: String
) : Principal
