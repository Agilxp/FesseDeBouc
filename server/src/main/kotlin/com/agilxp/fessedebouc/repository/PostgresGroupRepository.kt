package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.Group
import com.agilxp.fessedebouc.db.GroupDAO
import com.agilxp.fessedebouc.db.Groups
import com.agilxp.fessedebouc.db.groupDAOToModel

class PostgresGroupRepository: GroupRepository {
    override suspend fun getAllPublicGroups(): List<Group>  = suspendTransaction {
        GroupDAO.find { (Groups.public eq true) }.map(::groupDAOToModel)
    }

    override suspend fun getGroupById(id: Int): Group? = suspendTransaction {
        if (GroupDAO.findById(id) == null) {
            null
        } else {
            groupDAOToModel(GroupDAO.findById(id)!!)
        }
    }
}