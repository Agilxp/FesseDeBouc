package com.agilxp.fessedebouc

import com.agilxp.fessedebouc.model.RefreshTokenRequest
import com.agilxp.fessedebouc.model.RefreshTokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.auth.providers.BearerAuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

interface Platform {
    val name: String
    val client: HttpClient
}

abstract class PlatformClass() : Platform {
    protected val bearerTokenStorage = mutableListOf<BearerTokens>()
    protected val CustomerResponseHandlerPlugin = createClientPlugin("CustomerResponseHandlerPlugin") {
        onResponse { response ->
            // Interceptor that throws exception when we get an HTTP error
            when (response.status) {
                HttpStatusCode.BadRequest -> throw BadRequestException(response.body() ?: "Bad Request")
                HttpStatusCode.Conflict -> throw ConflictException(response.body() ?: "Conflict")
                HttpStatusCode.InternalServerError -> throw UnknownServerException(
                    response.body() ?: "Internal Server Error"
                )
            }
        }
    }

    protected val customAuthConfig = BearerAuthConfig().apply {
        refreshTokens {
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
            it.url.host == hostname && it.url.port == port
        }
    }

    private val scheme = "http"
    private val hostname = "localhost"
    private val port = 8080

    protected val baseUrl = "$scheme://$hostname:$port"
}

expect fun getPlatform(): Platform