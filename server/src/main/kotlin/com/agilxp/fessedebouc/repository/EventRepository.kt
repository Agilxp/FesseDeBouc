package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.Event
import com.agilxp.fessedebouc.db.Group
import com.agilxp.fessedebouc.db.User
import com.agilxp.fessedebouc.model.EventDTO
import java.util.*

interface EventRepository {
    suspend fun getEventsForGroup(group: Group): List<Event>
    suspend fun getEventById(id: UUID): Event?
    suspend fun findUnansweredEventsForUser(user: User): List<Event>
    suspend fun createEvent(eventDTO: EventDTO, user: User, groupForEvent: Group): Event
    suspend fun acceptEvent(event: Event, user: User)
    suspend fun declineEvent(event: Event, user: User)
    suspend fun maybeEvent(event: Event, user: User)
}