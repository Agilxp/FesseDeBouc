package com.agilxp.fessedebouc.db

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object Users : IntIdTable("users") {
    val name = varchar("name", 255)
    val email = varchar("email", 255)
}

class UserDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserDAO>(Users)
    var name by Users.name
    var email by Users.email
    val groups by GroupDAO via UserGroups
}

@Serializable
data class User(
    val id: Int,
    val name: String,
    val email: String
)

fun userDAOToModel(userDAO: UserDAO) = User(
    id = userDAO.id.value,
    name = userDAO.name,
    email = userDAO.email
)

