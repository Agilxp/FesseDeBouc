package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.UserDAO
import com.agilxp.fessedebouc.db.Users

class PostgresUserRepository : UserRepository {

    override suspend fun getUserById(id: Int): UserDAO? = suspendTransaction {
        UserDAO.findById(id)
    }

    override suspend fun getUserByEmail(email: String): UserDAO? = suspendTransaction {
        val user = UserDAO.find { Users.email eq email }.firstOrNull()
        user
    }

    override suspend fun getUserByGoogleId(googleId: String): UserDAO? = suspendTransaction {
        val user = UserDAO.find { Users.google_id eq googleId }.firstOrNull()
        user
    }

    override suspend fun createUser(userName: String, userEmail: String, googleId: String): UserDAO = suspendTransaction {
        UserDAO.new {
            name = userName
            email = userEmail
            this.googleId = googleId
        }
    }
}