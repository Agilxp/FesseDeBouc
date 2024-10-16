package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.Event
import com.agilxp.fessedebouc.db.User
import com.agilxp.fessedebouc.model.EventDTO

interface EventRepository {
    suspend fun getEventsForGroup(groupId: Int): List<Event>
    suspend fun getEventById(id: Int): Event?
    suspend fun addEventToGroup(eventDTO: EventDTO, userId: Int, groupId: Int)
    suspend fun acceptEvent(event: Event, user: User)
    suspend fun declineEvent(event: Event, user: User)
}