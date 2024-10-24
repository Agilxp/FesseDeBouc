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
    val content = text("content").nullable()
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)
    val document = largeText("document").nullable()
    val documentContentType = varchar("document_content_type", 255).nullable()
    val documentFileName = varchar("document_file_name", 255).nullable()
    val sender = reference("sender_id", Users)
    val group = reference("group_id", Groups)
    val event = reference("event_id", Events).nullable()
}

class MessageDAO(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<MessageDAO>(Messages)

    var content by Messages.content
    var createdAt by Messages.createdAt
    var document by Messages.document
    var documentContentType by Messages.documentContentType
    var documentFileName by Messages.documentFileName
    var sender by UserDAO referencedOn Messages.sender
    var group by GroupDAO referencedOn Messages.group
    var event by EventDAO optionalReferencedOn Messages.event

    fun toModel() = Message(
        id = id.value,
        createdAt = createdAt,
        document = document,
        documentContentType = documentContentType,
        documentFileName = documentFileName,
        content = content,
        sender = sender.toModel(),
        event = event?.toModel(),
        group = group.toModel(),
    )
}

data class Message(
    val id: UUID,
    val createdAt: OffsetDateTime,
    val document: String?,
    val documentContentType: String?,
    val documentFileName: String?,
    val content: String?,
    val sender: User,
    val event: Event?,
    val group: Group,
) {
    fun toDto() = MessageDTO(KOffsetDateTimeSerializer.serialize(createdAt), document, documentContentType, documentFileName, content, sender.toDto(), event?.toDto(), group.toDto())
}