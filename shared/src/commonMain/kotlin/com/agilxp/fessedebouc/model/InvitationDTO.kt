package com.agilxp.fessedebouc.model

import kotlinx.serialization.Serializable

@Serializable
data class InvitationDTO(val email: String)

@Serializable
data class JoinGroupRequestDTO(
    val id: String,
    val group: GroupDTO,
    val status: String
)