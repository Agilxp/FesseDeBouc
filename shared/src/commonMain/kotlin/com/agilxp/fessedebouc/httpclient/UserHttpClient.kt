package com.agilxp.fessedebouc.httpclient

import com.agilxp.fessedebouc.getPlatform
import io.ktor.client.plugins.websocket.*

class UserHttpClient {

    companion object {
        suspend fun connectUserStatusWebSocket() {
            getPlatform().client.webSocket("/ws/me") {

            }
        }
    }
}