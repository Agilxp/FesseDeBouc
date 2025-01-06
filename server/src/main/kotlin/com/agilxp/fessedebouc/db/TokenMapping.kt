package com.agilxp.fessedebouc.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import java.util.*

object Tokens : UUIDTable("tokens") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val accessToken = text("accessToken")
    val refreshToken = text("refreshToken")
    val code = uuid("code").nullable()
}

class TokenDAO(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<TokenDAO>(Tokens)

    var userId by Tokens.userId
    var accessToken by Tokens.accessToken
    var refreshToken by Tokens.refreshToken
    var code by Tokens.code

    fun toModel() = Token(userId.value, accessToken, refreshToken, code)
}

data class Token(
    val userId: UUID,
    val accessToken: String,
    val refreshToken: String,
    val code: UUID?
)
