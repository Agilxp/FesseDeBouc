package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.GroupDAO
import com.agilxp.fessedebouc.db.Message
import com.agilxp.fessedebouc.db.MessageDAO
import com.agilxp.fessedebouc.db.Messages
import com.agilxp.fessedebouc.db.UserDAO
import com.agilxp.fessedebouc.db.messageDAOToModel

class PostgresMessageRepository: MessageRepository {
    override suspend fun getMessagesForGroup(groupId: Int): List<Message> = suspendTransaction {
        MessageDAO.find { (Messages.group eq groupId) }.map { messageDAOToModel(it) }
    }

    override suspend fun addMessageToGroup(message: String, userId: Int, groupId: Int): Unit = suspendTransaction {
        MessageDAO.new {
            content = message
            sender = UserDAO[userId]
            group = GroupDAO[groupId]
        }
    }
}