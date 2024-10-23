package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.Group
import com.agilxp.fessedebouc.db.Message
import com.agilxp.fessedebouc.db.User

interface MessageRepository {
    suspend fun getMessagesForGroup(group: Group): List<Message>
    suspend fun addMessageToGroup(message: String, user: User, group: Group)
}