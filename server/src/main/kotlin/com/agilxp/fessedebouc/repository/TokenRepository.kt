package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.Token
import java.util.*

interface TokenRepository {
    suspend fun getTokenByCode(code: UUID): Token?
    suspend fun getTokenByRefreshToken(refreshToken: String): Token?
    suspend fun saveToken(token: Token)
}