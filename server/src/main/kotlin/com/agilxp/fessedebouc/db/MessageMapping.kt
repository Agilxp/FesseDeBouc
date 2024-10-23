package com.agilxp.fessedebouc.db

import com.agilxp.fessedebouc.model.MessageDTO
import com.agilxp.fessedebouc.util.KOffsetDateTimeSerializer
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestampWithTimeZone
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import java.time.OffsetDateTime
import java.util.*

object Messages : UUIDTable("messages") {
    val content = text("content")
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)
    val sender = reference("sender_id", Users)
    val group = reference("group_id", Groups)
}

class MessageDAO(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<MessageDAO>(Messages)

    var content by Messages.content
    var createdAt by Messages.createdAt
    var sender by UserDAO referencedOn Messages.sender
    var group by GroupDAO referencedOn Messages.group

    fun toModel() = Message(
        id = id.value,
        createdAt = createdAt,
        content = content,
        sender = sender.toModel(),
    )
}

data class Message(
    val id: UUID,
    val createdAt: OffsetDateTime,
    val content: String,
    val sender: User,
) {
    fun toDto() = MessageDTO(KOffsetDateTimeSerializer.serialize(createdAt), content, sender.toDto())
}