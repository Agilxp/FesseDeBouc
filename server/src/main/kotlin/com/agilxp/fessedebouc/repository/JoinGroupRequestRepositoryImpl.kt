package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.update
import java.util.*

class JoinGroupRequestRepositoryImpl : JoinGroupRequestRepository {

    override suspend fun findByGroupEmailAndStatus(
        groupId: UUID,
        inviteeEmail: String,
        statuses: List<RequestStatus>
    ): JoinGroupRequest? = suspendTransaction {
        JoinGroupRequestDAO
            .find { (JoinGroupRequests.group eq groupId) and (JoinGroupRequests.email eq inviteeEmail) and (JoinGroupRequests.status inList statuses) }
            .map { it.toModel() }
            .firstOrNull()
    }

    override suspend fun findByIdAndGroup(requestId: UUID, groupId: UUID): JoinGroupRequest?  = suspendTransaction {
        JoinGroupRequestDAO
            .find { (JoinGroupRequests.id eq requestId) and (JoinGroupRequests.group eq groupId) }
            .map { it.toModel() }
            .firstOrNull()
    }

    override suspend fun findInvitationByUserEmail(userEmail: String): List<JoinGroupRequest> = suspendTransaction {
        JoinGroupRequestDAO
            .find { (JoinGroupRequests.email eq userEmail) and (JoinGroupRequests.type eq RequestType.INVITATION) }
            .map { it.toModel() }
            .toList()
    }

    override suspend fun createRequest(
        inviteeEmail: String,
        requestType: RequestType,
        groupId: UUID
    ): JoinGroupRequest = suspendTransaction {
        JoinGroupRequestDAO.new {
            email = inviteeEmail
            type = requestType
            group = GroupDAO[groupId]
        }.toModel()
    }

    override suspend fun acceptRequest(acceptRequest: JoinGroupRequest): Unit = suspendTransaction {
        val request = acceptRequest.copy(status = RequestStatus.ACCEPTED)
        JoinGroupRequests.update(where = { JoinGroupRequests.id eq request.id }) {
            it[status] = request.status
        }
    }

    override suspend fun declineRequest(declineRequest: JoinGroupRequest): Unit = suspendTransaction {
        val request = declineRequest.copy(status = RequestStatus.DECLINED)
        JoinGroupRequests.update(where = { JoinGroupRequests.id eq request.id }) {
            it[status] = request.status
        }
    }
}