package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.Group
import com.agilxp.fessedebouc.db.GroupDAO
import com.agilxp.fessedebouc.db.Groups
import com.agilxp.fessedebouc.db.User
import com.agilxp.fessedebouc.db.UserGroups
import com.agilxp.fessedebouc.db.groupDAOToModel
import com.agilxp.fessedebouc.model.GroupDTO
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert

class PostgresGroupRepository : GroupRepository {

    override suspend fun getGroupById(id: Int): Group? = suspendTransaction {
        if (GroupDAO.findById(id) == null) {
            null
        } else {
            groupDAOToModel(GroupDAO.findById(id)!!)
        }
    }

    override suspend fun createGroup(group: GroupDTO): Group = suspendTransaction {
        val group = GroupDAO.new {
            name = group.name
            description = group.description ?: ""
        }
        groupDAOToModel(group)
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
}