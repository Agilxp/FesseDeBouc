package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.SimpleMessageDTO
import com.agilxp.fessedebouc.db.Token
import com.agilxp.fessedebouc.model.*
import com.agilxp.fessedebouc.repository.TokenRepository
import com.agilxp.fessedebouc.repository.UserRepository
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import java.time.Clock
import java.util.*

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

//private val tokens = mutableMapOf<String, String>()
//private val exchange = mutableMapOf<String, AuthResponse>()

fun Application.configureAuth(
    httpClient: HttpClient = applicationHttpClient,
    clock: Clock,
    jwtConfig: JWTConfig,
    oauthConfig: OAuthConfig,
    userRepository: UserRepository,
    tokenRepository: TokenRepository,
) {
    val jwtVerifier = JWT
        .require(Algorithm.HMAC256(jwtConfig.secret))
        .withAudience(jwtConfig.audience)
        .withIssuer(jwtConfig.issuer)
        .build()
    val redirects = mutableMapOf<String, String>()
    install(Authentication) {
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
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    SimpleMessageDTO("Token invalid, expired or missing")
                )
            }
        }
        oauth(oauthConfig.name) {
            urlProvider = { oauthConfig.redirectUrl }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = oauthConfig.name,
                    authorizeUrl = oauthConfig.authorizeUrl,
                    accessTokenUrl = oauthConfig.accessTokenUrl,
                    requestMethod = HttpMethod.Post,
                    clientId = oauthConfig.clientId,
                    clientSecret = oauthConfig.clientSecret,
                    defaultScopes = oauthConfig.defaultScopes,
                    onStateCreated = { call, state ->
                        call.request.queryParameters["redirectUrl"]?.let {
                            redirects[state] = it
                        }
                    }
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
                        val accessToken = jwtConfig.createToken(
                            clock,
                            principal.expiresIn,
                            user.id,
                            user.name,
                            userInfo.email,
                            userInfo.id
                        )
                        val refreshToken = jwtConfig.createToken(
                            clock,
                            86400, // 24 hours
                            user.id,
                            user.name,
                            userInfo.email,
                            userInfo.id
                        )
                        val code = UUID.randomUUID()
                        val token = Token(user.id, accessToken, refreshToken, code)
                        tokenRepository.saveToken(token)
                        redirects[state]?.let { redirect ->
                            var redirectUrl = redirect
                            redirectUrl += if (redirect.contains("?")) {
                                "&code=$code"
                            } else {
                                "?code=$code"
                            }
                            call.respondRedirect(redirectUrl)
                            return@get
                        }
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
        get("/logout") {
            call.respond(HttpStatusCode.OK)
        }
        route("/oauth") {
            get("/exchange") {
                val code: String = call.request.queryParameters["code"] ?: throw BadRequestException("Bad exchange")
                val token: Token = tokenRepository.getTokenByCode(UUID.fromString(code)) ?: throw BadRequestException("Bad exchange")
                val updatedToken = token.copy(code = null)
                tokenRepository.saveToken(updatedToken)
                val authResponse = AuthResponse(token.accessToken, token.refreshToken)
                call.respond(HttpStatusCode.OK, authResponse)
            }
            post("/refresh") {
                val request = call.receive<RefreshTokenRequest>()
                val refreshToken = request.refreshToken
                val decodedJWT = jwtVerifier.verify(refreshToken)
                if (decodedJWT.audience.contains(jwtConfig.audience)) {
                    val token = tokenRepository.getTokenByRefreshToken(refreshToken) ?: throw AuthenticationException("Bad refresh token")
                    val userId = token.userId.toString()
                    if (decodedJWT.claims["user_id"]?.asString() != userId) {
                        throw AuthenticationException("Bad refresh token")
                    }
                    val user = userRepository.getUserById(UUID.fromString(userId)) ?: throw AuthenticationException("Bad refresh token")
                    val newAccessToken = jwtConfig.createToken(
                        clock,
                        3600,
                        user.id,
                        user.name,
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