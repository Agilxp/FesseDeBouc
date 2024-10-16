package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.Event
import com.agilxp.fessedebouc.db.EventDAO
import com.agilxp.fessedebouc.db.Events
import com.agilxp.fessedebouc.db.GroupDAO
import com.agilxp.fessedebouc.db.UserDAO
import com.agilxp.fessedebouc.db.eventDAOToModel
import com.agilxp.fessedebouc.model.EventDTO

class PostgresEventRepository : EventRepository {
    override suspend fun getEventsForGroup(groupId: Int): List<Event> = suspendTransaction {
        EventDAO.find { (Events.group eq groupId) }.map { eventDAOToModel(it) }
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
}