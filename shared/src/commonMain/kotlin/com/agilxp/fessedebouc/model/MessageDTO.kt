package com.agilxp.fessedebouc.model

import kotlinx.serialization.Serializable

@Serializable
data class MessageDTO(
    val createdAt: String,
    val document: String?,
    val content: String?,
    val sender: UserDTO,
)

@Serializable
data class PostMessageDTO(
    val document: String? = null,
    val content: String? = null,
) {
    fun isEmpty(): Boolean {
        return document.isNullOrEmpty() && content.isNullOrEmpty()
    }
}
