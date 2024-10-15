package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.User

interface UserRepository {
    suspend fun getUserById(id: Int): User?
}