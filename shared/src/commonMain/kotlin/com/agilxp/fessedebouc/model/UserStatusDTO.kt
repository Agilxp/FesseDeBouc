package com.agilxp.fessedebouc.model

import kotlinx.serialization.Serializable

@Serializable
data class UserStatusDTO(
    val events: List<EventDTO>,
    val invitations: List<JoinGroupRequestDTO>,
    val unreadMessages: List<UnreadMessageDTO>,
)

@Serializable
data class UnreadMessageDTO(
    val groupDTO: GroupDTO,
    val count: Int,
)
