package com.agilxp.fessedebouc.httpclient

import com.agilxp.fessedebouc.ConflictException
import com.agilxp.fessedebouc.UnknownServerException
import com.agilxp.fessedebouc.getPlatform
import com.agilxp.fessedebouc.model.GroupDTO
import com.agilxp.fessedebouc.model.MessageDTO
import com.agilxp.fessedebouc.model.PostMessageDTO
import io.ktor.client.call.*
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.*
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import kotlin.coroutines.cancellation.CancellationException

class MessageHttpClient {

    companion object {
        @Throws(UnknownServerException::class, CancellationException::class)
        suspend fun getGroupMessages(group: GroupDTO): List<MessageDTO> {
            val response = getPlatform().client.get("/messages/${group.id}")
            println("Response in Group client: $response")
            return response.body<List<MessageDTO>>()
        }

        @Throws(UnknownServerException::class, ConflictException::class, CancellationException::class)
        suspend fun postMessageToGroup(group: GroupDTO, message: PostMessageDTO) {
            if (message.isEmpty()) {
                return
            }
            println("message: $message")
            getPlatform().client.submitFormWithBinaryData(
                "/messages/${group.id}",
                formData = formData {
                    append("content", message.content!!)
                }
            ) {
                onUpload { bytesSentTotal, contentLength ->
                    println("Sent $bytesSentTotal bytes from $contentLength")
                }
            }
        }
    }
}