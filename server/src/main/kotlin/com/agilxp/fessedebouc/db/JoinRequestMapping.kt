package com.agilxp.fessedebouc.db

import com.agilxp.fessedebouc.model.JoinGroupRequestDTO
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

enum class RequestType {
    REQUEST,
    INVITATION,
}

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
}

object JoinGroupRequests : UUIDTable("invitations") {
    val email = varchar("email", 255)
    val type = enumerationByName("type", 20, RequestType::class)
    val status = enumerationByName("status", 20, RequestStatus::class).default(RequestStatus.PENDING)
    val group = reference("group_id", Groups)
}

class JoinGroupRequestDAO(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<JoinGroupRequestDAO>(JoinGroupRequests)
    var email by JoinGroupRequests.email
    var type by JoinGroupRequests.type
    var status by JoinGroupRequests.status
    var group by GroupDAO referencedOn JoinGroupRequests.group

    fun toModel() = JoinGroupRequest(id.value, email, type, status, group.toModel())
}

data class JoinGroupRequest(
    val id: UUID,
    val email: String,
    val type: RequestType,
    val status: RequestStatus,
    val group: Group,
) {
    fun toDto() = JoinGroupRequestDTO(
        id = id.toString(),
        group = group.toDto(),
        status = status.name
    )
}