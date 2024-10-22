package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.GroupDAO
import com.agilxp.fessedebouc.db.MessageDAO
import com.agilxp.fessedebouc.db.UserDAO

interface MessageRepository {
    suspend fun getMessagesForGroup(group: GroupDAO): List<MessageDAO>
    suspend fun addMessageToGroup(message: String, userDAO: UserDAO, groupDAO: GroupDAO)
}