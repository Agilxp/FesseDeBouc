package com.agilxp.fessedebouc.model

import kotlinx.serialization.Serializable

@Serializable
data class UserStatusDTO(
    val events: List<EventDTO>,
    val invitations: List<JoinGroupRequestDTO>,
    val unreadMessages: List<UnreadMessageDTO>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserStatusDTO) return false
        if (events != other.events) return false
        if (invitations != other.invitations) return false
        if (unreadMessages != other.unreadMessages) return false
        return true
    }

    override fun hashCode(): Int {
        var result = events.hashCode()
        result = 31 * result + invitations.hashCode()
        result = 31 * result + unreadMessages.hashCode()
        return result
    }
}

@Serializable
data class UnreadMessageDTO(
    val groupDTO: GroupDTO,
    val count: Int,
)
