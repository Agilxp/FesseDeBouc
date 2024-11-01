package com.agilxp.fessedebouc

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json

class JVMPlatform : PlatformClass() {
    override val name: String = "Java ${System.getProperty("java.version")}"

    override val client = HttpClient(CIO) {
        // To handle JSON (de)serialization
        install(ContentNegotiation) {
            json()
        }
        // OAuth client handling
        install(Auth) {
            customAuthConfig
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

