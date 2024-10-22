package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.EventDAO
import com.agilxp.fessedebouc.db.EventParticipants
import com.agilxp.fessedebouc.db.Events
import com.agilxp.fessedebouc.db.GroupDAO
import com.agilxp.fessedebouc.db.UserDAO
import com.agilxp.fessedebouc.model.EventDTO
import com.agilxp.fessedebouc.util.KOffsetDateTimeSerializer
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.upsert

class PostgresEventRepository : EventRepository {
    override suspend fun getEventsForGroup(group: GroupDAO): List<EventDAO> = suspendTransaction {
        EventDAO.find { (Events.group eq group.id) }.map { it }
    }

    override suspend fun getEventById(id: Int): EventDAO? = suspendTransaction {
        val event = EventDAO.findById(id)
        event
    }

    override suspend fun createEvent(eventDTO: EventDTO, userDAO: UserDAO, groupDAO: GroupDAO): Unit = suspendTransaction {
        EventDAO.new {
            name = eventDTO.name
            description = eventDTO.description
            start = KOffsetDateTimeSerializer.deserialize(eventDTO.start)
            end = KOffsetDateTimeSerializer.deserialize(eventDTO.end)
            location = eventDTO.location
            owner = userDAO
            group = groupDAO
        }
    }

    override suspend fun acceptEvent(event: EventDAO, user: UserDAO): Unit = suspendTransaction {
        EventParticipants.upsert {
            it[eventId] = event.id
            it[userId] = user.id
        }
    }

    override suspend fun declineEvent(event: EventDAO, user: UserDAO): Unit = suspendTransaction {
        EventParticipants.deleteWhere(1) { (eventId eq event.id) and (userId eq user.id) }
    }
}