package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.User

interface UserRepository {
    suspend fun getUserById(id: Int): User?
    suspend fun getUserByEmail(email: String): User?
    suspend fun getUserByGoogleId(googleId: String): User?
    suspend fun createUser(name: String, email: String, googleId: String): User
}