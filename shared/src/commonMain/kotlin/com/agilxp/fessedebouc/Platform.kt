package com.agilxp.fessedebouc

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

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
                // DO NOT INTERCEPT 401 OR THE REFRESHING OF TOKENS WILL FAIL
                HttpStatusCode.BadRequest -> throw BadRequestException(
                    Json.decodeFromString<SimpleMessageDTO>(response.body()).message ?: "Bad Request"
                )

                HttpStatusCode.Conflict -> throw ConflictException(
                    Json.decodeFromString<SimpleMessageDTO>(response.body()).message ?: "Conflict"
                )

                HttpStatusCode.InternalServerError -> throw UnknownServerException(
                    Json.decodeFromString<SimpleMessageDTO>(response.body()).message ?: "Internal Server Error"
                )
            }
        }
    }

    private val scheme = "https"
    protected val hostname = "fessedebouc.agilxp.com"
    private val port = 443

    protected val baseUrl = "$scheme://$hostname:$port"
}

expect fun getPlatform(): Platform