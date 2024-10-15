package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.Group

interface GroupRepository {
    suspend fun getAllPublicGroups(): List<Group>
    suspend fun getGroupById(id: Int): Group?
}