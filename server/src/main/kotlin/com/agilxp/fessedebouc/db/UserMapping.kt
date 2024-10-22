package com.agilxp.fessedebouc.db

import com.agilxp.fessedebouc.model.UserDTO
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Users : IntIdTable("users") {
    val name = varchar("name", 255)
    val email = varchar("email", 255)
    val google_id = varchar("google_id", 255)
}

class UserDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserDAO>(Users)
    var name by Users.name
    var email by Users.email
    var googleId by Users.google_id
    val groups by GroupDAO via UserGroups

    fun toDTO(): UserDTO = UserDTO(name, email, googleId)
}