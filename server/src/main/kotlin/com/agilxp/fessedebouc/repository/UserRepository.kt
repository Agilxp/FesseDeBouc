package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.UserDAO

interface UserRepository {
    suspend fun getUserById(id: Int): UserDAO?
    suspend fun getUserByEmail(email: String): UserDAO?
    suspend fun getUserByGoogleId(googleId: String): UserDAO?
    suspend fun createUser(userName: String, userEmail: String, googleId: String): UserDAO
}