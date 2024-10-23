package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.Group
import com.agilxp.fessedebouc.db.Message
import com.agilxp.fessedebouc.db.User
import com.agilxp.fessedebouc.model.PostMessageDTO

interface MessageRepository {
    suspend fun getMessagesForGroup(group: Group): List<Message>
    suspend fun addMessageToGroup(message: PostMessageDTO, user: User, group: Group)
}