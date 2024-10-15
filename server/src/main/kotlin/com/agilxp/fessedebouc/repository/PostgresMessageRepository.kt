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

    override suspend fun saveMessage(message: Message): Unit = suspendTransaction {
        MessageDAO.new {
            content = message.content
            sender = UserDAO[message.sender.id]
        }
    }
}