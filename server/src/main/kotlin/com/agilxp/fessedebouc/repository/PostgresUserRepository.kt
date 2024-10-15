package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.User
import com.agilxp.fessedebouc.db.UserDAO
import com.agilxp.fessedebouc.db.userDAOToModel

class PostgresUserRepository : UserRepository {

    override suspend fun getUserById(id: Int): User? = suspendTransaction {
        if (UserDAO.findById(id) == null) {
            null
        } else {
            userDAOToModel(UserDAO.findById(id)!!)
        }
    }
}