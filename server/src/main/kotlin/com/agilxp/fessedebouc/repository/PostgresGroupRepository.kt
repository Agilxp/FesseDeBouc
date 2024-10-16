package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.Group
import com.agilxp.fessedebouc.db.GroupDAO
import com.agilxp.fessedebouc.db.groupDAOToModel

class PostgresGroupRepository: GroupRepository {

    override suspend fun getGroupById(id: Int): Group? = suspendTransaction {
        if (GroupDAO.findById(id) == null) {
            null
        } else {
            groupDAOToModel(GroupDAO.findById(id)!!)
        }
    }
}