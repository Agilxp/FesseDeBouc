package com.agilxp.fessedebouc.db

import com.agilxp.fessedebouc.util.SerializableOffsetDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestampWithTimeZone
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object Messages : IntIdTable("messages") {
    val content = text("content")
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)
    val sender =  reference("sender_id", Users)
    val group = reference("group_id", Groups)
}

class MessageDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<MessageDAO>(Messages)
    var content by Messages.content
    var createdAt by Messages.createdAt
    var sender by UserDAO referencedOn Messages.sender
    var group by GroupDAO referencedOn Messages.group
}

@Serializable
data class Message(
    val id: Int,
    val createdAt: SerializableOffsetDateTime,
    val content: String,
    val sender: User,
)

fun messageDAOToModel(messageDAO: MessageDAO) = Message(
    id = messageDAO.id.value,
    createdAt = messageDAO.createdAt,
    content = messageDAO.content,
    sender = userDAOToModel(messageDAO.sender),
)
