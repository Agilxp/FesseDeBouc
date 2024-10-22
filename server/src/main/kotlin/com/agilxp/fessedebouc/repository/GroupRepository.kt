package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.GroupDAO
import com.agilxp.fessedebouc.db.UserDAO
import com.agilxp.fessedebouc.model.GroupDTO

interface GroupRepository {
    suspend fun getGroupById(id: Int): GroupDAO?
    suspend fun createGroup(group: GroupDTO): GroupDAO
    suspend fun updateGroup(groupId: Int, group: GroupDTO): GroupDAO
    suspend fun addUserToGroup(group: GroupDAO, user: UserDAO, admin: Boolean)
    suspend fun removeUserFromGroup(groupId: Int, userId: Int)
    suspend fun getGroupsForUser(user: UserDAO): List<GroupDAO>
    suspend fun findByName(groupName: String): List<GroupDAO>
}