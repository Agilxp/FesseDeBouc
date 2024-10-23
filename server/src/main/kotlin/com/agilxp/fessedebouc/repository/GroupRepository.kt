package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.Group
import com.agilxp.fessedebouc.db.User
import com.agilxp.fessedebouc.model.GroupDTO

interface GroupRepository {
    suspend fun getGroupById(id: Int): Group?
    suspend fun createGroup(group: GroupDTO): Group
    suspend fun updateGroup(groupId: Int, group: GroupDTO): Group
    suspend fun addUserToGroup(group: Group, user: User, admin: Boolean)
    suspend fun removeUserFromGroup(groupId: Int, userId: Int)
    suspend fun getGroupsForUser(user: User): List<Group>
    suspend fun findByName(groupName: String): List<Group>
}