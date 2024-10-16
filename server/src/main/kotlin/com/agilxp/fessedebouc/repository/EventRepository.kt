package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.Event
import com.agilxp.fessedebouc.model.EventDTO

interface EventRepository {
    suspend fun getEventsForGroup(groupId: Int): List<Event>
    suspend fun addEventToGroup(eventDTO: EventDTO, userId: Int, groupId: Int)
}