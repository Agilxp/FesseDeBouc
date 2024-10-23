package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.User
import com.agilxp.fessedebouc.db.UserDAO
import com.agilxp.fessedebouc.db.Users

class UserRepositoryImpl : UserRepository {

    override suspend fun getUserById(id: Int): User? = suspendTransaction {
        UserDAO.findById(id)?.toModel()
    }

    override suspend fun getUserByEmail(email: String): User? = suspendTransaction {
        val user = UserDAO.find { Users.email eq email }.firstOrNull()
        user?.toModel()
    }

    override suspend fun getUserByGoogleId(googleId: String): User? = suspendTransaction {
        val user = UserDAO.find { Users.google_id eq googleId }.firstOrNull()
        user?.toModel()
    }

    override suspend fun createUser(userName: String, userEmail: String, googleId: String): User = suspendTransaction {
        UserDAO.new {
            name = userName
            email = userEmail
            this.googleId = googleId
        }.toModel()
    }
}