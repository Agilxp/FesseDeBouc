package com.agilxp.fessedebouc.model

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.Clock

data class JWTConfig(
    val name: String,
    val realm: String,
    val secret: String,
    val audience: String,
    val issuer: String,
    val expirationSeconds: Long
)

fun JWTConfig.createToken(clock: Clock, accessToken: String, expirationSeconds: Long, userId: Int, userEmail: String, googleId: String): String =
    JWT.create()
        .withAudience(this.audience)
        .withIssuer(this.issuer)
        .withClaim("google_access_token", accessToken)
        .withClaim("google_id", googleId)
        .withClaim("user_id", userId)
        .withClaim("user_email", userEmail)
        .withExpiresAt(clock.instant().plusSeconds(expirationSeconds))
        .sign(Algorithm.HMAC256(this.secret))
