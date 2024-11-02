package com.agilxp.fessedebouc

import com.agilxp.fessedebouc.model.RefreshTokenRequest
import com.agilxp.fessedebouc.model.RefreshTokenResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

class JVMPlatform : PlatformClass() {
    override val name: String = "Java ${System.getProperty("java.version")}"

    override val client = HttpClient(CIO) {
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
                    val refreshTokenInfo: RefreshTokenResponse = response.body()
                    bearerTokenStorage.add(BearerTokens(refreshTokenInfo.accessToken, oldTokens?.refreshToken))
                    bearerTokenStorage.last()
                }
                loadTokens {
                    println("Loading tokens")
//                    val code = URL(window.location.href).searchParams.get("code")
//                    if (!code.isNullOrEmpty()) {
//                        val response = HttpClient(Js).use { client -> client.get("$baseUrl/oauth/exchange?code=$code") }
//                        val auth = response.body<AuthResponse>()
//                        bearerTokenStorage.add(BearerTokens(auth.accessToken, auth.refreshToken))
//                    } else if (bearerTokenStorage.size == 0) {
//                        window.location.href = "http://localhost:8080/login?redirectUrl=http://localhost:3000"
//                    }
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
}

actual fun getPlatform(): Platform = JVMPlatform()

