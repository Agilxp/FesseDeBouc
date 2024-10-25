package com.agilxp.fessedebouc.httpclient

import com.agilxp.fessedebouc.model.RefreshTokenRequest
import com.agilxp.fessedebouc.model.RefreshTokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

class KMHttpClient {
    companion object {
        private val bearerTokenStorage = mutableListOf<BearerTokens>()

        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        bearerTokenStorage.last()
                    }
                    refreshTokens {
                        val response = client.post("${baseUrl()}/oauth/refresh") {
                            contentType(ContentType.Application.Json)
                            setBody(RefreshTokenRequest(bearerTokenStorage.last().refreshToken!!))
                            markAsRefreshTokenRequest()
                        }
                        val refreshTokenInfo: RefreshTokenResponse = response.body()
                        bearerTokenStorage.add(BearerTokens(refreshTokenInfo.accessToken, oldTokens?.refreshToken))
                        bearerTokenStorage.last()
                    }
                    sendWithoutRequest {
                        it.url.host == hostname && it.url.port == port
                    }
                }
            }
        }

        fun baseUrl(): String = "$scheme://$hostname:$port"

    }
}

const val scheme = "http"
const val hostname = "localhost"
const val port = 8080


