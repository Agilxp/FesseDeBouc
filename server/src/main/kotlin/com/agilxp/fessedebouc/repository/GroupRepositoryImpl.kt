package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.DuplicateException
import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.*
import com.agilxp.fessedebouc.model.GroupDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class GroupRepositoryImpl : GroupRepository {

    override suspend fun getGroupById(id: Int): GroupDAO? = suspendTransaction {
        GroupDAO.findById(id)
    }

    override suspend fun createGroup(group: GroupDTO): GroupDAO = suspendTransaction {
        val existingGroup = GroupDAO.find { Groups.name eq group.name }.firstOrNull()
        if (existingGroup != null) {
            throw DuplicateException("Group already exists")
        }
        val newGroup = GroupDAO.new {
            name = group.name
            description = group.description ?: ""
        }
        newGroup
    }

    override suspend fun updateGroup(groupId: Int, group: GroupDTO): GroupDAO = suspendTransaction {
        val existingGroup = GroupDAO.find { Groups.name eq group.name }.firstOrNull()
        if (existingGroup != null && groupId != existingGroup.id.value) {
            throw DuplicateException("Group name already in use")
        }
        Groups.update(where = { Groups.id eq groupId }) {
            it[name] = group.name
            it[description] = group.description ?: ""
        }
        GroupDAO[groupId]
    }

    override suspend fun addUserToGroup(group: GroupDAO, user: UserDAO, admin: Boolean): Unit = suspendTransaction {
        UserGroups.insert {
            it[userId] = user.id
            it[groupId] = group.id
            it[isAdmin] = admin
        }
    }

    override suspend fun removeUserFromGroup(
        groupId: Int,
        userId: Int
    ): Unit = suspendTransaction {
        UserGroups.deleteWhere {
            (UserGroups.userId eq userId) and (UserGroups.groupId eq groupId)
        }
    }

    override suspend fun getGroupsForUser(user: UserDAO): List<GroupDAO> = suspendTransaction {
        UserGroups.selectAll()
            .where { UserGroups.userId eq user.id }
            .map { GroupDAO[it[UserGroups.groupId].value] }
    }

    override suspend fun findByName(groupName: String): List<GroupDAO> = suspendTransaction {
        GroupDAO.find { Groups.name.lowerCase() like "%${groupName.lowercase()}%" }.toList()
    }
}