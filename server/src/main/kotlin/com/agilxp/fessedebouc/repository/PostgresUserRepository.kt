package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.User
import com.agilxp.fessedebouc.db.UserDAO
import com.agilxp.fessedebouc.db.Users
import com.agilxp.fessedebouc.db.userDAOToModel

class PostgresUserRepository : UserRepository {

    override suspend fun getUserById(id: Int): User? = suspendTransaction {
        val user = UserDAO.findById(id)
        if (user == null) {
            null
        } else {
            userDAOToModel(user)
        }
    }

    override suspend fun getUserByEmail(email: String): User? = suspendTransaction {
        val user = UserDAO.find { Users.email eq email }.firstOrNull()
        if (user == null) {
            null
        } else {
            userDAOToModel(user)
        }
    }

    override suspend fun getUserByGoogleId(googleId: String): User? = suspendTransaction {
        val user = UserDAO.find { Users.google_id eq googleId }.firstOrNull()
        if (user == null) {
            null
        } else {
            userDAOToModel(user)
        }
    }

    override suspend fun createUser(name: String, email: String, googleId: String): User = suspendTransaction {
        userDAOToModel(UserDAO.new {
            name
            email
            google_id = googleId
        })
    }
}