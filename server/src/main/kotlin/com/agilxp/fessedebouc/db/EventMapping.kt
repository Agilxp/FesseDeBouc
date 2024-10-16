package com.agilxp.fessedebouc.db

import com.agilxp.fessedebouc.util.SerializableOffsetDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object Events : IntIdTable("events") {
    val name = varchar("name", 255)
    val description = varchar("description", 255)
    val start = timestampWithTimeZone("start")
    val end = timestampWithTimeZone("end")
    val location = varchar("location", 255)
    val group = reference("group_id", Groups)
    val owner = reference("user_id", Users)
}

object EventParticipants : IntIdTable("event_participants") {
    val eventId = reference("event_id", Events, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
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
    var participants by UserDAO via EventParticipants
}

@Serializable
data class Event(
    val id: Int,
    val name: String,
    val description: String,
    val start: SerializableOffsetDateTime,
    val end: SerializableOffsetDateTime,
    val location: String,
    val group: Group,
    val owner: User,
    val participants: List<User>
)

fun eventDAOToModel(eventDAO: EventDAO) = Event(
    id = eventDAO.id.value,
    name = eventDAO.name,
    description = eventDAO.description,
    start = eventDAO.start,
    end = eventDAO.end,
    location = eventDAO.location,
    group = groupDAOToModel(eventDAO.group),
    owner = userDAOToModel(eventDAO.owner),
    participants = eventDAO.participants.map { userDAOToModel(it) }
)
