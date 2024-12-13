package com.agilxp.fessedebouc.httpclient

import com.agilxp.fessedebouc.getPlatform
import com.agilxp.fessedebouc.model.UserStatusDTO
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json

class UserHttpClient {

    companion object {
        suspend fun getUserStatus(callback: (UserStatusDTO) -> Unit): Unit {
            return getPlatform().client.webSocket("wss://fessedebouc.agilxp.com/ws/me?at=${getPlatform().getToken()}") {
                for (message in incoming) {
                    message as? Frame.Text ?: continue
                    val userStatusDTO = Json.decodeFromString<UserStatusDTO>(message.readText())
                    callback(userStatusDTO)
                }
            }
        }
    }
}