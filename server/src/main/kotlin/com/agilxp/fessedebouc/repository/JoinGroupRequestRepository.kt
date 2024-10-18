package com.agilxp.fessedebouc.repository

import com.agilxp.fessedebouc.db.JoinGroupRequestDAO
import com.agilxp.fessedebouc.db.RequestType

interface JoinGroupRequestRepository {
    suspend fun findByIdGroupAndEmail(requestId: Int, groupId: Int, inviteeEmail: String): JoinGroupRequestDAO?
    suspend fun createRequest(inviteeEmail: String, requestType: RequestType, groupId: Int): JoinGroupRequestDAO
    suspend fun acceptRequest(request: JoinGroupRequestDAO)
    suspend fun declineRequest(request: JoinGroupRequestDAO)
}