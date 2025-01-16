package com.agilxp.fessedebouc

import com.agilxp.fessedebouc.model.UserDTO
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.auth.providers.*
import kotlinx.serialization.json.Json

interface Platform {
    val name: String
    val client: HttpClient
    fun getUser(): UserDTO
    fun getToken(): String
}

abstract class PlatformClass() : Platform {
    protected val bearerTokenStorage = mutableListOf<BearerTokens>()

    protected val CustomerResponseHandlerPlugin = createClientPlugin("CustomerResponseHandlerPlugin") {
        onResponse { response ->
            // Interceptor that throws exception when we get an HTTP error
            when (response.status.value) {
                // DO NOT INTERCEPT 401 OR THE REFRESHING OF TOKENS WILL FAIL
                400 -> throw BadRequestException(
                    Json.decodeFromString<SimpleMessageDTO>(response.body()).message ?: "Bad Request"
                )

                in 403..429 -> throw ClientRequestException(
                    response,
                    Json.decodeFromString<SimpleMessageDTO>(response.body()).message ?: "Forbidden"
                )

                in 500..507 -> throw UnknownServerException(
                    Json.decodeFromString<SimpleMessageDTO>(response.body()).message ?: "Internal Server Error"
                )
            }
        }
    }

    private val scheme = "https"
    protected val hostname = "fessedebouc.agilxp.com"
    private val port = 443
    private val path = "/api/"

    protected val baseUrl = "$scheme://$hostname:$port$path"
    protected val wasmUrl = "$scheme://$hostname:$port"
//    protected val wasmUrl = "http://localhost:3000"
}

expect fun getPlatform(): Platform