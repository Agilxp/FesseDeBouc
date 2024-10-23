package com.agilxp.fessedebouc.db

import com.agilxp.fessedebouc.model.EventDTO
import com.agilxp.fessedebouc.util.KOffsetDateTimeSerializer
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

enum class EventStatus {
    UNANSWERED,
    MAYBE,
    ACCEPTED,
    DECLINED,
}

object Events : IntIdTable("events") {
    val name = varchar("name", 255)
    val description = varchar("description", 255)
    val start = timestampWithTimeZone("start")
    val end = timestampWithTimeZone("end")
    val location = varchar("location", 255)
    val group = reference("group_id", Groups)
    val owner = reference("user_id", Users)
}

object EventParticipants : Table("event_participants") {
    val eventId = reference("event_id", Events, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val status = enumerationByName("status", 20, EventStatus::class).default(EventStatus.UNANSWERED)

    override val primaryKey = PrimaryKey(eventId, userId)
}

class EventDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<EventDAO>(Events)

    var name by Events.name
    var description by Events.description
    var start by Events.start
    var end by Events.end
    var location by Events.location
    var group by GroupDAO referencedOn Events.group
    var owner by UserDAO referencedOn Events.owner
    val accepted get() = getParticipation(this, EventStatus.ACCEPTED)
    val maybe get() = getParticipation(this, EventStatus.MAYBE)
    val declined get() = getParticipation(this, EventStatus.DECLINED)
    val unanswered get() = getParticipation(this, EventStatus.UNANSWERED)

    fun toModel() = Event(
        id = id.value,
        name = name,
        description = description,
        start = start,
        end = end,
        location = location,
        group = group.toModel(),
        owner = owner.toModel(),
        accepted = accepted.map { it.toModel() },
        maybe = maybe.map { it.toModel() },
        declined = declined.map { it.toModel() },
        unanswered = unanswered.map { it.toModel() },
    )
}

fun getParticipation(o: EventDAO, status: EventStatus): SizedIterable<UserDAO> {
    if (o.id._value == null) return emptySized()
    return transaction {
        val query = {
            UserDAO.wrapRows(
                Users
                    .join(
                        EventParticipants,
                        JoinType.INNER,
                    ).selectAll()
                    .where { EventParticipants.eventId eq o.id and (EventParticipants.status eq status) }
            )
        }
        query()
    }
}

data class Event(
    val id: Int,
    val name: String,
    val description: String,
    val start: OffsetDateTime,
    val end: OffsetDateTime,
    val location: String,
    val group: Group,
    val owner: User,
    val accepted: List<User>,
    val maybe: List<User>,
    val declined: List<User>,
    val unanswered: List<User>,
) {
    fun toDto() = EventDTO(
        name,
        description,
        KOffsetDateTimeSerializer.serialize(start),
        KOffsetDateTimeSerializer.serialize(end),
        location
    )
}
