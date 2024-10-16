package com.agilxp.fessedebouc.model

import io.ktor.server.auth.Principal
import kotlinx.serialization.Serializable

@Serializable
data class UserSession(val state: String, val accessToken: String, val userId: Int, val userEmail: String, val googleId: String): Principal
