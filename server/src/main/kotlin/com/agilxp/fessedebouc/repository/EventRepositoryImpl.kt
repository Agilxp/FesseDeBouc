package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.*
import com.agilxp.fessedebouc.model.EventDTO
import com.agilxp.fessedebouc.util.KOffsetDateTimeSerializer
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.upsert
import java.util.*

class EventRepositoryImpl : EventRepository {
    override suspend fun getEventsForGroup(group: Group): List<Event> = suspendTransaction {
        EventDAO.find { (Events.group eq group.id) }.map { it.toModel() }
    }

    override suspend fun getEventById(id: UUID): Event? = suspendTransaction {
        val event = EventDAO.findById(id)
        event?.toModel()
    }

    override suspend fun findUnansweredEventsForUser(user: User): List<Event> = suspendTransaction {
        EventParticipants.select(EventParticipants.eventId)
            .where { (EventParticipants.userId eq user.id) and (EventParticipants.status eq EventStatus.UNANSWERED) }
            .mapNotNull { EventDAO.findById(it[EventParticipants.eventId]) }
            .map { it.toModel() }
    }

    override suspend fun createEvent(eventDTO: EventDTO, user: User, groupForEvent: Group): Event = suspendTransaction {
        val g = GroupDAO[groupForEvent.id]
        val event = EventDAO.new {
            name = eventDTO.name
            description = eventDTO.description
            start = KOffsetDateTimeSerializer.deserialize(eventDTO.start)
            end = KOffsetDateTimeSerializer.deserialize(eventDTO.end)
            location = eventDTO.location
            owner = UserDAO[user.id]
            group = g
        }
        g.users.forEach { u ->
            if (user.id != u.id.value) {
                EventParticipants.upsert {
                    it[eventId] = event.id
                    it[userId] = u.id
                    it[status] = EventStatus.UNANSWERED
                }
            }
        }
        event.toModel()
    }

    override suspend fun acceptEvent(event: Event, user: User): Unit = suspendTransaction {
        EventParticipants.upsert {
            it[eventId] = event.id
            it[userId] = user.id
            it[status] = EventStatus.ACCEPTED
        }
    }

    override suspend fun declineEvent(event: Event, user: User): Unit = suspendTransaction {
        EventParticipants.upsert {
            it[eventId] = event.id
            it[userId] = user.id
            it[status] = EventStatus.DECLINED
        }
    }

    override suspend fun maybeEvent(event: Event, user: User): Unit = suspendTransaction {
        EventParticipants.upsert {
            it[eventId] = event.id
            it[userId] = user.id
            it[status] = EventStatus.MAYBE
        }
    }
}