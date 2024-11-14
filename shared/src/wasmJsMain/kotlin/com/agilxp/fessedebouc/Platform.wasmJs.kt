package com.agilxp.fessedebouc

import com.agilxp.fessedebouc.model.AuthResponse
import com.agilxp.fessedebouc.model.RefreshTokenRequest
import com.agilxp.fessedebouc.model.RefreshTokenResponse
import com.agilxp.fessedebouc.model.TokenData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.core.*
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.w3c.dom.get
import org.w3c.dom.url.URL

class WasmPlatform : PlatformClass() {

    override val name: String = "Web with Kotlin/Wasm"

    override val client = HttpClient(Js) {
        // To handle JSON (de)serialization
        install(ContentNegotiation) {
            json()
        }
        // OAuth client handling
        install(Auth) {
            bearer {
                refreshTokens {
                    println("Refreshing token")
                    val response = client.post("$baseUrl/oauth/refresh") {
                        contentType(ContentType.Application.Json)
                        setBody(RefreshTokenRequest(bearerTokenStorage.last().refreshToken!!))
                        markAsRefreshTokenRequest()
                    }
                    if (response.status.isSuccess()) {
                        val refreshTokenInfo: RefreshTokenResponse = response.body()
                        bearerTokenStorage.add(BearerTokens(refreshTokenInfo.accessToken, oldTokens?.refreshToken))
                        localStorage.setItem("at", refreshTokenInfo.accessToken)
                        bearerTokenStorage.last()
                    } else {
                        val code = URL(window.location.href).searchParams.get("code")
                        if (code.isNullOrEmpty()) {
                            window.location.href = "$baseUrl/login?redirectUrl=http://localhost:3000"
                        }
                        null
                    }
                }
                loadTokens {
                    val accessToken = localStorage["at"]
                    val refreshToken = localStorage["rt"]
                    val code = URL(window.location.href).searchParams.get("code")
                    if (isValidToken(accessToken) || isValidToken(refreshToken)) {
                        bearerTokenStorage.add(BearerTokens(accessToken!!, refreshToken))
                    } else if (!code.isNullOrEmpty()) {
                        val response = HttpClient(Js) {
                            install(ContentNegotiation) {
                                json()
                            }
                        }.use { client -> client.get("$baseUrl/oauth/exchange?code=$code") }
                        if (response.status.isSuccess()) {
                            val auth = response.body<AuthResponse>()
                            bearerTokenStorage.add(BearerTokens(auth.accessToken, auth.refreshToken))
                            localStorage.setItem("at", auth.accessToken)
                            localStorage.setItem("rt", auth.refreshToken)
                            println("Have now ${bearerTokenStorage.size} tokens")
                        }
                        window.location.href = "http://localhost:3000"
                    } else if (bearerTokenStorage.size == 0) {
                        println("No token and no code, login...")
                        window.location.href = "$baseUrl/login?redirectUrl=http://localhost:3000"
                    } else {
                        println("Looks like we are having an issue")
                    }
                    bearerTokenStorage.last()
                }
                sendWithoutRequest {
                    it.url.host == hostname
                }
            }
        }
        // Install the response interceptor
        install(CustomerResponseHandlerPlugin)
        // Define a base URL so we don't need to add it to every request
        defaultRequest {
            url(baseUrl)
        }
    }

    override fun getUserEmail(): String {
        val accessToken = localStorage["at"]
        if (accessToken.isNullOrEmpty()) {
            return ""
        }
        val parsedToken = Json.decodeFromString<TokenData>(window.atob(accessToken.split('.')[1]))
        return parsedToken.user_email
    }
}

fun isValidToken(token: String?): Boolean {
    if (token == null) {
        return false
    }
    val parsedToken = Json.decodeFromString<TokenData>(window.atob(token.split('.')[1]))
    return parsedToken.exp > Clock.System.now().toEpochMilliseconds() / 1000
}

actual fun getPlatform(): Platform = WasmPlatform()