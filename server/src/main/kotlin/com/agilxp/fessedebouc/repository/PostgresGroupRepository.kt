package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.*
import com.agilxp.fessedebouc.model.GroupDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class PostgresGroupRepository : GroupRepository {

    override suspend fun getGroupById(id: Int): Group? = suspendTransaction {
        if (GroupDAO.findById(id) == null) {
            null
        } else {
            groupDAOToModel(GroupDAO.findById(id)!!)
        }
    }

    override suspend fun createGroup(group: GroupDTO): Group = suspendTransaction {
        val newGroup = GroupDAO.new {
            name = group.name
            description = group.description ?: ""
        }
        groupDAOToModel(newGroup)
    }

    override suspend fun updateGroup(group: Group): Group = suspendTransaction {
        Groups.update {
            it[id] = group.id
            it[name] = group.name
            it[description] = group.description
        }
        groupDAOToModel(GroupDAO[group.id])
    }

    override suspend fun addUserToGroup(group: Group, user: User, admin: Boolean): Unit = suspendTransaction {
        UserGroups.upsert {
            it[userId] = user.id
            it[groupId] = group.id
            it[isAdmin] = admin
        }
    }

    override suspend fun removeUserFromGroup(
        group: Group,
        user: User
    ): Unit = suspendTransaction {
        UserGroups.deleteWhere(1) {
            (userId eq user.id) and (groupId eq group.id)
        }
    }

    override suspend fun getGroupsForUser(user: User): List<Group> = suspendTransaction {
        UserGroups.selectAll()
            .where { UserGroups.userId eq user.id }
            .map { groupDAOToModel(GroupDAO[it[UserGroups.groupId].value]) }
    }

    override suspend fun findByName(groupName: String): List<GroupDTO> = suspendTransaction {
        GroupDAO.find { Groups.name.lowerCase() like "%${groupName.lowercase()}%" }
            .map { GroupDTO(it.id.value, it.name, it.description) }
    }
}