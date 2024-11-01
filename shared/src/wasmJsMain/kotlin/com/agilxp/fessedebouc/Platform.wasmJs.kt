package com.agilxp.fessedebouc

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json

class WasmPlatform: PlatformClass() {
    override val name: String = "Web with Kotlin/Wasm"

    override val client = HttpClient(Js) {
        // To handle JSON (de)serialization
        install(ContentNegotiation) {
            json()
        }
        // OAuth client handling
        install(Auth) {
            customAuthConfig
        }
        // Install the response interceptor
//        install(CustomerResponseHandlerPlugin)
        // Define a base URL so we don't need to add it to every request
        defaultRequest {
            url(baseUrl)
        }
    }
}

actual fun getPlatform(): Platform = WasmPlatform()