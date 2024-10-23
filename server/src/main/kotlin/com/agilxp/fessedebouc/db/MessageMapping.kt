package com.agilxp.fessedebouc.db

import com.agilxp.fessedebouc.model.MessageDTO
import com.agilxp.fessedebouc.util.KOffsetDateTimeSerializer
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestampWithTimeZone
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import java.time.OffsetDateTime

object Messages : IntIdTable("messages") {
    val content = text("content")
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)
    val sender = reference("sender_id", Users)
    val group = reference("group_id", Groups)
}

class MessageDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<MessageDAO>(Messages)

    var content by Messages.content
    var createdAt by Messages.createdAt
    var sender by UserDAO referencedOn Messages.sender
    var group by GroupDAO referencedOn Messages.group

    fun toDto() = MessageDTO(KOffsetDateTimeSerializer.serialize(createdAt), content, sender.toDto())

    fun toModel() = Message(
        id = id.value,
        createdAt = createdAt,
        content = content,
        sender = sender.toModel(),
    )
}

data class Message(
    val id: Int,
    val createdAt: OffsetDateTime,
    val content: String,
    val sender: User,
) {
    fun toDto() = MessageDTO(KOffsetDateTimeSerializer.serialize(createdAt), content, sender.toDto())
}