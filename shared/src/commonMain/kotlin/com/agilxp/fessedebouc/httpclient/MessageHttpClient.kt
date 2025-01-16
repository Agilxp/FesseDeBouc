package com.agilxp.fessedebouc.httpclient

import com.agilxp.fessedebouc.UnknownServerException
import com.agilxp.fessedebouc.getPlatform
import com.agilxp.fessedebouc.model.GroupDTO
import com.agilxp.fessedebouc.model.MessageDTO
import com.agilxp.fessedebouc.model.PostMessageDTO
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

class MessageHttpClient {

    companion object {
        @Throws(UnknownServerException::class, CancellationException::class)
        suspend fun getGroupMessages(group: GroupDTO): List<MessageDTO> {
            val response = getPlatform().client.get("messages/${group.id}")
            return response.body<List<MessageDTO>>()
        }

        @Throws(UnknownServerException::class, CancellationException::class)
        suspend fun postMessageToGroup(message: PostMessageDTO, session: DefaultWebSocketSession) {
            if (message.isEmpty()) {
                return
            }
            session.send(Frame.Text(Json.encodeToString<PostMessageDTO>(message)))
        }

        suspend fun createMessageSession(group: GroupDTO, callback: (MessageDTO) -> Unit): DefaultWebSocketSession {
            val session =
                getPlatform().client.webSocketSession { url("wss://fessedebouc.agilxp.com/ws/messages/${group.id}?at=${getPlatform().getToken()}") }
            session.launch {
                for (message in session.incoming) {
                    message as? Frame.Text ?: continue
                    try {
                        val newMessage = Json.decodeFromString<MessageDTO>(message.readText())
                        callback(newMessage)
                    } catch (e: Exception) {
                        println("WS Exception: ${e.message}")
                        continue
                    }
                }
            }
            return session
        }
    }
}