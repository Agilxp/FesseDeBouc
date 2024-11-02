package com.agilxp.fessedebouc

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.http.*

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
                HttpStatusCode.OK, HttpStatusCode.Created, HttpStatusCode.Accepted -> return@onResponse
                HttpStatusCode.BadRequest -> throw BadRequestException(response.body() ?: "Bad Request")
                HttpStatusCode.Unauthorized -> throw UnauthorizedException(response.body() ?: "Bad Request")
                HttpStatusCode.Conflict -> throw ConflictException(response.body() ?: "Conflict")
                HttpStatusCode.InternalServerError -> throw UnknownServerException(
                    response.body() ?: "Internal Server Error"
                )
                else -> throw Exception("Unknown error")
            }
        }
    }

//    protected val customAuthConfig = BearerAuthConfig().apply {
//        refreshTokens {
//            println("Refreshing token")
//            val response = client.post("${baseUrl}/oauth/refresh") {
//                contentType(ContentType.Application.Json)
//                setBody(RefreshTokenRequest(bearerTokenStorage.last().refreshToken!!))
//                markAsRefreshTokenRequest()
//            }
//            val refreshTokenInfo: RefreshTokenResponse = response.body()
//            bearerTokenStorage.add(BearerTokens(refreshTokenInfo.accessToken, oldTokens?.refreshToken))
//            bearerTokenStorage.last()
//        }
//        loadTokens {
//            println("Loading tokens")
//            bearerTokenStorage.last()
//        }
//        sendWithoutRequest {
//            it.url.host == hostname
//        }
//    }

    private val scheme = "http"
    protected val hostname = "localhost"
    private val port = 8080

    protected val baseUrl = "$scheme://$hostname:$port"
}

expect fun getPlatform(): Platform