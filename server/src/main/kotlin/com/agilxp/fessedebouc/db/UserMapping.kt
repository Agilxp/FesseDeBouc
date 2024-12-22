package com.agilxp.fessedebouc.db

import com.agilxp.fessedebouc.model.UserDTO
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Users : UUIDTable("users") {
    val name = varchar("name", 255)
    val email = varchar("email", 255)
    val google_id = varchar("google_id", 255)
}

class UserDAO(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserDAO>(Users)
    var name by Users.name
    var email by Users.email
    var googleId by Users.google_id
    val groups by GroupDAO via UserGroups

    fun toModel() = User(
        id = id.value,
        name = name,
        email = email,
        googleId = googleId
    )
}

data class User(
    val id: UUID,
    val name: String,
    val email: String,
    val googleId: String,
) {
    fun toDto() = UserDTO(id.toString(), name, email, googleId)
}