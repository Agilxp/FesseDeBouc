package com.agilxp.fessedebouc

import com.agilxp.fessedebouc.model.GroupDTO
import io.ktor.client.request.get

class GroupClient {

    suspend fun getGroups(): List<GroupDTO> {
        AuthClient.client.get()
        return emptyList()
    }
}