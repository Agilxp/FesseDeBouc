package com.agilxp.fessedebouc.model

import kotlinx.serialization.Serializable

@Serializable
data class MessageDTO(
    val createdAt: String,
    val content: String,
    val sender: UserDTO,
)
