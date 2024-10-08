package com.agilxp.fessedebouc

import com.agilxp.fessedebouc.model.UserInfo
import com.agilxp.fessedebouc.model.UserSession
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.websocket.*

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

val applicationHttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}


fun Application.module(httpClient: HttpClient = applicationHttpClient) {
    install(Sessions) {
        cookie<UserSession>("user_session")
    }
    val redirects = mutableMapOf<String, String>()
    install(Authentication) {
        oauth("auth-oauth-google") {
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = System.getenv("GOOGLE_CLIENT_ID"),
                    clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),
                    defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile"),
                    onStateCreated = { call, state ->
                        //saves new state with redirect url value
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
        authenticate("auth-oauth-google") {
            get("/login") {
                // Redirects to 'authorizeUrl' automatically
                // call.respondRedirect("/auth/oauth/google")
            }
            get("/callback") {
                val currentPrincipal: OAuthAccessTokenResponse.OAuth2? = call.principal()
                // redirects home if the url is not found before authorization
                currentPrincipal?.let { principal ->
                    principal.state?.let { state ->
                        call.sessions.set(UserSession(state, principal.accessToken))
                        redirects[state]?.let { redirect ->
                            call.respondRedirect(redirect)
                            return@get
                        }
                    }
                }
                call.respondRedirect("/home")
            }
        }
        get("/") {
            call.respondRedirect("/login")
        }
        get("/home") {
            val userSession: UserSession? = getSession(call)
            if (userSession != null) {
                val userInfo: UserInfo = getPersonalGreeting(httpClient, userSession)
                call.respondText("Hello, ${userInfo.name}! Welcome home!")
            }
        }
        get("/{path}") {
            val userSession: UserSession? = getSession(call)
            if (userSession != null) {
                val userInfo: UserInfo = getPersonalGreeting(httpClient, userSession)
                call.respondText("Hello, ${userInfo.name}!")
            }
        }
    }
}

private suspend fun getPersonalGreeting(
    httpClient: HttpClient,
    userSession: UserSession
): UserInfo = httpClient.get("https://www.googleapis.com/oauth2/v2/userinfo") {
    headers {
        append(HttpHeaders.Authorization, "Bearer ${userSession.token}")
    }
}.body()

private suspend fun getSession(
    call: ApplicationCall
): UserSession? {
    val userSession: UserSession? = call.sessions.get()
    //if there is no session, redirect to log in
    if (userSession == null) {
        val redirectUrl = URLBuilder("http://0.0.0.0:8080/login").run {
            parameters.append("redirectUrl", call.request.uri)
            build()
        }
        call.respondRedirect(redirectUrl)
        return null
    }
    return userSession
}