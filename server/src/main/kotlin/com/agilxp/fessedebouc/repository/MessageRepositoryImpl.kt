package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.*
import com.agilxp.fessedebouc.model.PostMessageDTO

class MessageRepositoryImpl: MessageRepository {
    override suspend fun getMessagesForGroup(group: Group): List<Message> = suspendTransaction {
        MessageDAO.find { (Messages.group eq group.id) }.map { it.toModel() }
    }

    override suspend fun addMessageToGroup(message: PostMessageDTO, user: User, groupToAddMessage: Group): Unit = suspendTransaction {
        MessageDAO.new {
            content = message.content
            document = message.document
            documentContentType = message.documentContentType
            documentFileName = message.documentFileName
            sender = UserDAO[user.id]
            group = GroupDAO[groupToAddMessage.id]
        }
    }
}