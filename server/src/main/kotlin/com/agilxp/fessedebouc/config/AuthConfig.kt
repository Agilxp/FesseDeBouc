package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.model.AuthResponse
import com.agilxp.fessedebouc.model.JWTConfig
import com.agilxp.fessedebouc.model.OAuthConfig
import com.agilxp.fessedebouc.model.RefreshTokenRequest
import com.agilxp.fessedebouc.model.RefreshTokenResponse
import com.agilxp.fessedebouc.model.UserInfo
import com.agilxp.fessedebouc.model.UserSession
import com.agilxp.fessedebouc.model.createToken
import com.agilxp.fessedebouc.repository.UserRepository
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.json.Json
import java.time.Clock
import java.util.UUID

val applicationHttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(json = Json {
            encodeDefaults = true
            isLenient = true
            allowSpecialFloatingPointValues = true
            allowStructuredMapKeys = true
            prettyPrint = false
            useArrayPolymorphism = false
            ignoreUnknownKeys = true
        })
    }
}

private val tokens = mutableMapOf<String, String>()

fun Application.configureAuth(
    httpClient: HttpClient = applicationHttpClient,
    clock: Clock,
    jwtConfig: JWTConfig,
    oauthConfig: OAuthConfig,
    userRepository: UserRepository,
) {
    val jwtVerifier = JWT
        .require(Algorithm.HMAC256(jwtConfig.secret))
        .withAudience(jwtConfig.audience)
        .withIssuer(jwtConfig.issuer)
        .build()
    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.maxAgeInSeconds = 1800
            cookie.path = "/"
        }
    }
    install(Authentication) {
        session<UserSession>("auth-session") {
            validate { session ->
                if (session.accessToken.isNotBlank()) {
                    session
                } else {
                    null
                }
            }
            challenge {
                call.respondRedirect("/login")
            }
        }
        jwt(jwtConfig.name) {
            realm = jwtConfig.realm
            verifier(jwtVerifier)
            validate { credential ->
                if (
                    credential.payload.audience.contains(jwtConfig.audience)
                    && credential.payload.getClaim("user_email").asString() != ""
                    && credential.payload.getClaim("user_id").asString() != ""
                    && credential.payload.getClaim("google_id").asString() != ""
                ) {
                    val user = userRepository.getUserByGoogleId(credential.payload.getClaim("google_id").asString())
                    if (
                        user != null
                        && user.id.toString() == credential.payload.getClaim("user_id").asString()
                        && user.email == credential.payload.getClaim("user_email").asString()
                    ) {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
            challenge { scheme, realm ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    "Token to access ${HttpHeaders.WWWAuthenticate} $scheme realm=\"$realm\" is either invalid or expired."
                )
            }
        }
        oauth(oauthConfig.name) {
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = oauthConfig.name,
                    authorizeUrl = oauthConfig.authorizeUrl,
                    accessTokenUrl = oauthConfig.accessTokenUrl,
                    requestMethod = HttpMethod.Post,
                    clientId = oauthConfig.clientId,
                    clientSecret = oauthConfig.clientSecret,
                    defaultScopes = oauthConfig.defaultScopes,
                )
            }
            client = httpClient
        }
    }
    routing {
        authenticate(oauthConfig.name) {
            get("/login") {
                // Redirects to 'authorizeUrl' automatically
            }
            get("/callback") {
                val currentPrincipal: OAuthAccessTokenResponse.OAuth2? = call.principal()
                currentPrincipal?.let { principal ->
                    principal.state?.let { state ->
                        val userInfo = getUserInfo(principal.accessToken, oauthConfig, httpClient)
                        var user = userRepository.getUserByGoogleId(userInfo.id)
                        if (user == null) {
                            user = userRepository.createUser(userInfo.name, userInfo.email, userInfo.id)
                        }
                        call.sessions.set(
                            UserSession(
                                state,
                                principal.accessToken,
                                user.id,
                                userInfo.email,
                                userInfo.id
                            )
                        )
                        val accessToken = jwtConfig.createToken(
                            clock,
                            principal.expiresIn,
                            user.id,
                            userInfo.email,
                            userInfo.id
                        )
                        val refreshToken = jwtConfig.createToken(
                            clock,
                            86400, // 24 hours
                            user.id,
                            userInfo.email,
                            userInfo.id
                        )
                        tokens[refreshToken] = user.id.toString()
                        call.respond(HttpStatusCode.OK, AuthResponse(accessToken, refreshToken))
                    }
                }
            }
        }
        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/")
        }
        post("/oauth/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            val refreshToken = request.refreshToken
            val decodedJWT = jwtVerifier.verify(refreshToken)
            if (decodedJWT.audience.contains(jwtConfig.audience)) {
                val userId = tokens[refreshToken] ?: throw AuthenticationException("Bad refresh token")
                if (decodedJWT.claims["user_id"]?.asString() != userId) {
                    throw AuthenticationException("Bad refresh token")
                }
                val user = userRepository.getUserById(UUID.fromString(userId)) ?: throw AuthenticationException("Bad refresh token")
                val newAccessToken = jwtConfig.createToken(
                    clock,
                    3600,
                    user.id,
                    user.email,
                    user.googleId
                )
                call.respond(HttpStatusCode.OK, RefreshTokenResponse(newAccessToken))
            } else {
                throw AuthenticationException("Bad refresh token")
            }
        }
    }
}

suspend fun getUserInfo(
    accessToken: String,
    oauthConfig: OAuthConfig,
    httpClient: HttpClient
): UserInfo = httpClient.get(oauthConfig.userInfoUrl) {
    headers {
        append(HttpHeaders.Authorization, "Bearer $accessToken")
    }
}.body()