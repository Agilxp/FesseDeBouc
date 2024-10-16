package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.Event
import com.agilxp.fessedebouc.db.EventDAO
import com.agilxp.fessedebouc.db.EventParticipants
import com.agilxp.fessedebouc.db.Events
import com.agilxp.fessedebouc.db.GroupDAO
import com.agilxp.fessedebouc.db.User
import com.agilxp.fessedebouc.db.UserDAO
import com.agilxp.fessedebouc.db.eventDAOToModel
import com.agilxp.fessedebouc.model.EventDTO
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.upsert

class PostgresEventRepository : EventRepository {
    override suspend fun getEventsForGroup(groupId: Int): List<Event> = suspendTransaction {
        EventDAO.find { (Events.group eq groupId) }.map { eventDAOToModel(it) }
    }

    override suspend fun getEventById(id: Int): Event? = suspendTransaction {
        val event = EventDAO.findById(id)
        if (event == null) {
            null
        } else {
            eventDAOToModel(event)
        }
    }

    override suspend fun addEventToGroup(eventDTO: EventDTO, userId: Int, groupId: Int): Unit = suspendTransaction {
        EventDAO.new {
            name = eventDTO.name
            description = eventDTO.description
            start = eventDTO.start
            end = eventDTO.end
            location = eventDTO.location
            owner = UserDAO[userId]
            group = GroupDAO[groupId]
        }
    }

    override suspend fun acceptEvent(event: Event, user: User): Unit = suspendTransaction {
        EventParticipants.upsert {
            it[eventId] = event.id
            it[userId] = user.id
        }
    }

    override suspend fun declineEvent(event: Event, user: User): Unit = suspendTransaction {
        EventParticipants.deleteWhere(1) { (eventId eq event.id) and (userId eq user.id) }
    }
}