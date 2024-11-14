package com.agilxp.fessedebouc

import com.agilxp.fessedebouc.model.RefreshTokenRequest
import com.agilxp.fessedebouc.model.RefreshTokenResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import platform.UIKit.UIDevice

class IOSPlatform: PlatformClass() {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    override val client = HttpClient(Darwin) {
        // To handle JSON (de)serialization
        install(ContentNegotiation) {
            json()
        }
        // OAuth client handling
        install(Auth) {
            bearer {
                refreshTokens {
                    println("Refreshing token")
                    val response = client.post("${baseUrl}/oauth/refresh") {
                        contentType(ContentType.Application.Json)
                        setBody(RefreshTokenRequest(bearerTokenStorage.last().refreshToken!!))
                        markAsRefreshTokenRequest()
                    }
                    val refreshTokenInfo: RefreshTokenResponse = response.body()
                    bearerTokenStorage.add(BearerTokens(refreshTokenInfo.accessToken, oldTokens?.refreshToken))
                    bearerTokenStorage.last()
                }
                loadTokens {
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
        TODO("Not yet implemented")
    }
}

actual fun getPlatform(): Platform = IOSPlatform()