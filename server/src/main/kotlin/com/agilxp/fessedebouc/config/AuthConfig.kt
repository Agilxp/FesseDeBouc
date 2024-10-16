package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.applicationHttpClient
import com.agilxp.fessedebouc.db.User
import com.agilxp.fessedebouc.model.JWTConfig
import com.agilxp.fessedebouc.model.OAuthConfig
import com.agilxp.fessedebouc.model.UserInfo
import com.agilxp.fessedebouc.model.UserSession
import com.agilxp.fessedebouc.model.createToken
import com.agilxp.fessedebouc.repository.UserRepository
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import java.time.Clock


fun Application.configureAuth(
    httpClient: HttpClient = applicationHttpClient,
    clock: Clock,
    jwtConfig: JWTConfig,
    oauthConfig: OAuthConfig,
    userRepository: UserRepository,
) {
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
        jwt("auth-jwt") {
            realm = jwtConfig.realm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtConfig.secret))
                    .withAudience(jwtConfig.audience)
                    .withIssuer(jwtConfig.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtConfig.audience)) JWTPrincipal(credential.payload) else null
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
                // call.respondRedirect("/auth/oauth/google")
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
                        call.sessions.set(UserSession(state, principal.accessToken, user.id, userInfo.email, userInfo.id))
                        val jwtToken = jwtConfig.createToken(
                            clock,
                            principal.accessToken,
                            principal.expiresIn,
                            user.id,
                            userInfo.email,
                            userInfo.id
                        )
                        call.respondText(jwtToken, contentType = ContentType.Text.Plain)
                    }
                }
            }
        }
        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/")
        }
    }
}

suspend fun getUserInfo(
    accessToken: String,
    oauthConfig: OAuthConfig,
    httpClient: HttpClient
): UserInfo = httpClient.get(oauthConfig.userInfoUrl) {
    headers {
        append(HttpHeaders.Authorization, "Bearer ${accessToken}")
    }
}.body()