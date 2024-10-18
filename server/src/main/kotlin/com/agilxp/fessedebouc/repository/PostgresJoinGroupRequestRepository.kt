package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.config.suspendTransaction
import com.agilxp.fessedebouc.db.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.update

class PostgresJoinGroupRequestRepository : JoinGroupRequestRepository {
    override suspend fun findByIdGroupAndEmail(
        requestId: Int,
        groupId: Int,
        inviteeEmail: String
    ): JoinGroupRequestDAO? = suspendTransaction {
        JoinGroupRequestDAO
            .find { (JoinGroupRequests.id eq requestId) and (JoinGroupRequests.group eq groupId) and (JoinGroupRequests.email eq inviteeEmail) }
            .firstOrNull()
    }

    override suspend fun findByIdAndGroup(requestId: Int, groupId: Int): JoinGroupRequestDAO?  = suspendTransaction {
        JoinGroupRequestDAO
            .find { (JoinGroupRequests.id eq requestId) and (JoinGroupRequests.group eq groupId) }
            .firstOrNull()
    }

    override suspend fun createRequest(
        inviteeEmail: String,
        requestType: RequestType,
        groupId: Int
    ): JoinGroupRequestDAO = suspendTransaction {
        JoinGroupRequestDAO.new {
            email = inviteeEmail
            type = requestType
            group = GroupDAO[groupId]
        }
    }

    override suspend fun acceptRequest(request: JoinGroupRequestDAO): Unit = suspendTransaction {
        request.status = RequestStatus.ACCEPTED
        JoinGroupRequests.update(where = { JoinGroupRequests.id eq request.id }) {
            it[status] = request.status
        }
    }

    override suspend fun declineRequest(request: JoinGroupRequestDAO): Unit = suspendTransaction {
        request.status = RequestStatus.DECLINED
        JoinGroupRequests.update(where = { JoinGroupRequests.id eq request.id }) {
            it[status] = request.status
        }
    }
}