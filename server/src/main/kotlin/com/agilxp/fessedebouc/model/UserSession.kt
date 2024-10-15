package com.agilxp.fessedebouc.model

import io.ktor.server.auth.Principal
import kotlinx.serialization.Serializable

@Serializable
data class UserSession(val state: String, val accessToken: String): Principal
