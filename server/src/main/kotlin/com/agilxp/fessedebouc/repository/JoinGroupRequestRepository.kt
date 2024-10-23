package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.JoinGroupRequestDAO
import com.agilxp.fessedebouc.db.RequestStatus
import com.agilxp.fessedebouc.db.RequestType
import java.util.*

interface JoinGroupRequestRepository {
    suspend fun findByGroupEmailAndStatus(groupId: UUID, inviteeEmail: String, statuses: List<RequestStatus>): JoinGroupRequestDAO?
    suspend fun findByIdAndGroup(requestId: UUID, groupId: UUID): JoinGroupRequestDAO?
    suspend fun createRequest(inviteeEmail: String, requestType: RequestType, groupId: UUID): JoinGroupRequestDAO
    suspend fun acceptRequest(request: JoinGroupRequestDAO)
    suspend fun declineRequest(request: JoinGroupRequestDAO)
}