package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.Message

interface MessageRepository {
    suspend fun getMessagesForGroup(groupId: Int): List<Message>
    suspend fun saveMessage(message: Message)
}