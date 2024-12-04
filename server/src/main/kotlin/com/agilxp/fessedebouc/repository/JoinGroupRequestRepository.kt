package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.JoinGroupRequest
import com.agilxp.fessedebouc.db.RequestStatus
import com.agilxp.fessedebouc.db.RequestType
import java.util.*

interface JoinGroupRequestRepository {
    suspend fun findByGroupEmailAndStatus(groupId: UUID, inviteeEmail: String, statuses: List<RequestStatus>): JoinGroupRequest?
    suspend fun findByIdAndGroup(requestId: UUID, groupId: UUID): JoinGroupRequest?
    suspend fun findInvitationByUserEmail(userEmail: String): List<JoinGroupRequest>
    suspend fun createRequest(inviteeEmail: String, requestType: RequestType, groupId: UUID): JoinGroupRequest
    suspend fun acceptRequest(acceptRequest: JoinGroupRequest)
    suspend fun declineRequest(declineRequest: JoinGroupRequest)
}