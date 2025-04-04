package com.agilxp.fessedebouc.model

import kotlinx.datetime.Clock.System
import kotlinx.serialization.Serializable

@Serializable
data class MessageDTO(
    val createdAt: String,
    val document: String?,
    val documentContentType: String?,
    val documentFileName: String?,
    val content: String?,
    val sender: UserDTO,
    val event: EventDTO?,
    val group: GroupDTO,
)

@Serializable
data class PostMessageDTO(
    var document: String? = null,
    var content: String? = null,
    var documentContentType: String? = null,
    var documentFileName: String? = null,
) {
    fun isEmpty(): Boolean {
        return document.isNullOrEmpty() && content.isNullOrEmpty()
    }

    fun toMessageDTO(sender: UserDTO, group: GroupDTO): MessageDTO {
        return MessageDTO(
            createdAt = System.now().toString(),
            document = document,
            documentContentType = documentContentType,
            documentFileName = documentFileName,
            content = content,
            sender = sender,
            event = null,
            group = group
        )
    }
}
