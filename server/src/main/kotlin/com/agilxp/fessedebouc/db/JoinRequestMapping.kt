package com.agilxp.fessedebouc.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

enum class RequestType {
    REQUEST,
    INVITATION
}

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
}

object JoinGroupRequests : IntIdTable("invitations") {
    val email = varchar("email", 255)
    val type = enumerationByName("type", 20, RequestType::class)
    val status = enumerationByName("status", 20, RequestStatus::class).default(RequestStatus.PENDING)
    val group = reference("group_id", Groups)
}

class JoinGroupRequestDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<JoinGroupRequestDAO>(JoinGroupRequests)
    var email by JoinGroupRequests.email
    var type by JoinGroupRequests.type
    var status by JoinGroupRequests.status
    var group by GroupDAO referencedOn JoinGroupRequests.group
}