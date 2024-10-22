package com.agilxp.fessedebouc.db

import com.agilxp.fessedebouc.model.GroupDTO
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.reflect.KProperty


object Groups : IntIdTable("groups") {
    val name = varchar("name", 255).uniqueIndex()
    val description = varchar("description", 255)
}

object UserGroups : IntIdTable("user_groups") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val groupId = reference("group_id", Groups, onDelete = ReferenceOption.CASCADE)
    val isAdmin = bool("is_admin")
    init {
        index(true, userId, groupId)
    }
}

class GroupDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<GroupDAO>(Groups)

    var name by Groups.name
    var description by Groups.description

    val users by UserDAO via UserGroups
    val admins get() = mapAdmin(this, GroupDAO::users)

    fun toDto() = GroupDTO(name, description)
}


fun mapAdmin(o: GroupDAO, unused: KProperty<*>): SizedIterable<UserDAO> {
    if (o.id._value == null) return emptySized()
    return transaction {
        val query = {
            UserDAO.wrapRows(
                Users
                    .join(
                        UserGroups,
                        JoinType.INNER,
                    ).selectAll()
                    .where { UserGroups.groupId eq o.id and (UserGroups.isAdmin eq true) }
            )
        }
        entityCache.getOrPutReferrers(o.id, UserGroups.userId, query)
    }
}