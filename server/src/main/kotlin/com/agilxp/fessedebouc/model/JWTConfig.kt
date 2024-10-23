package com.agilxp.fessedebouc.model

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.Clock
import java.util.*

data class JWTConfig(
    val name: String,
    val realm: String,
    val secret: String,
    val audience: String,
    val issuer: String,
)

fun JWTConfig.createToken(
    clock: Clock,
    accessToken: String,
    expirationSeconds: Long,
    userId: UUID,
    userEmail: String,
    googleId: String
): String =
    JWT.create()
        .withAudience(this.audience)
        .withIssuer(this.issuer)
        .withClaim("google_access_token", accessToken)
        .withClaim("google_id", googleId)
        .withClaim("user_id", userId.toString())
        .withClaim("user_email", userEmail)
        .withExpiresAt(clock.instant().plusSeconds(expirationSeconds))
        .sign(Algorithm.HMAC256(this.secret))
