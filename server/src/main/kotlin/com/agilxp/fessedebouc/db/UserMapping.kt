package com.agilxp.fessedebouc.db

import kotlinx.serialization.Serializable
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
    var google_id by Users.google_id
    val groups by GroupDAO via UserGroups
}

@Serializable
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val googleId: String,
)

fun userDAOToModel(userDAO: UserDAO) = User(
    id = userDAO.id.value,
    name = userDAO.name,
    email = userDAO.email,
    googleId = userDAO.google_id
)

