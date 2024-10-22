package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.GroupDAO
import com.agilxp.fessedebouc.db.MessageDAO
import com.agilxp.fessedebouc.db.Messages
import com.agilxp.fessedebouc.db.UserDAO

class MessageRepositoryImpl: MessageRepository {
    override suspend fun getMessagesForGroup(group: GroupDAO): List<MessageDAO> = suspendTransaction {
        MessageDAO.find { (Messages.group eq group.id) }.toList()
    }

    override suspend fun addMessageToGroup(message: String, userDAO: UserDAO, groupDAO: GroupDAO): Unit = suspendTransaction {
        MessageDAO.new {
            content = message
            sender = userDAO
            group = groupDAO
        }
    }
}