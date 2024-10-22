package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.EventDAO
import com.agilxp.fessedebouc.db.GroupDAO
import com.agilxp.fessedebouc.db.UserDAO
import com.agilxp.fessedebouc.model.EventDTO

interface EventRepository {
    suspend fun getEventsForGroup(group: GroupDAO): List<EventDAO>
    suspend fun getEventById(id: Int): EventDAO?
    suspend fun createEvent(eventDTO: EventDTO, userDAO: UserDAO, groupDAO: GroupDAO)
    suspend fun acceptEvent(event: EventDAO, user: UserDAO)
    suspend fun declineEvent(event: EventDAO, user: UserDAO)
}