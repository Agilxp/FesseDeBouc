package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.Token
import com.agilxp.fessedebouc.db.TokenDAO
import com.agilxp.fessedebouc.db.Tokens
import org.jetbrains.exposed.sql.upsert
import java.util.*

class TokenRepositoryImpl: TokenRepository {
    override suspend fun getTokenByCode(code: UUID): Token? = suspendTransaction {
        TokenDAO.find { Tokens.code eq code }.firstOrNull()?.toModel()
    }

    override suspend fun getTokenByRefreshToken(refreshToken: String): Token? = suspendTransaction {
        TokenDAO.find { Tokens.refreshToken eq refreshToken }.firstOrNull()?.toModel()
    }

    override suspend fun saveToken(token: Token): Unit = suspendTransaction {
        Tokens.upsert {
            it[userId] = token.userId
            it[accessToken] = token.accessToken
            it[refreshToken] = token.refreshToken
            it[code] = token.code
        }
    }
}