package com.agilxp.fessedebouc.model

import com.agilxp.fessedebouc.db.Group
import com.agilxp.fessedebouc.db.User
import io.ktor.websocket.*

class MessagingConnection(val session: DefaultWebSocketSession, val user: User, val group: Group) {
}