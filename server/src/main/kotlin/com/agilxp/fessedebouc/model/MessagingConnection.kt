package com.agilxp.fessedebouc.model

import com.agilxp.fessedebouc.db.Group
import com.agilxp.fessedebouc.db.User
import io.ktor.websocket.*

class MessagingConnection(val session: DefaultWebSocketSession, val user: User, val group: Group) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true // Check for reference equality
        if (other == null || other::class != this::class) return false // Check for null or class mismatch

        other as MessagingConnection // Safe cast to MessagingConnection

        return group.id == other.group.id && user.id == other.user.id // Compare fields
    }

    override fun hashCode(): Int {
        var result = session.hashCode()
        result = 31 * result + user.hashCode()
        result = 31 * result + group.hashCode()
        return result
    }
}