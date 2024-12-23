package com.agilxp.fessedebouc.db

import com.agilxp.fessedebouc.model.GroupDTO
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.reflect.KProperty

object Groups : UUIDTable("groups") {
    val name = varchar("name", 255).uniqueIndex()
    val description = varchar("description", 255)
}

object UserGroups : UUIDTable("user_groups") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val groupId = reference("group_id", Groups, onDelete = ReferenceOption.CASCADE)
    val isAdmin = bool("is_admin")

    init {
        index(true, userId, groupId)
    }
}

class GroupDAO(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<GroupDAO>(Groups)

    var name by Groups.name
    var description by Groups.description

    val users by UserDAO via UserGroups
    val admins get() = mapAdmin(this, GroupDAO::users)

    fun toModel() = Group(
        id.value,
        name,
        description,
        users.map { it.toModel() },
        admins.map { it.toModel() }
    )
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

data class Group(
    val id: UUID,
    val name: String,
    val description: String,
    val users: List<User>,
    val admins: List<User>
) {
    fun toDto() = GroupDTO(name, description, id.toString(), users.map { it.toDto() }, admins.map { it.toDto() })
}