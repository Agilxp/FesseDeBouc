package com.agilxp.fessedebouc.httpclient

import com.agilxp.fessedebouc.ConflictException
import com.agilxp.fessedebouc.UnknownServerException
import com.agilxp.fessedebouc.getPlatform
import com.agilxp.fessedebouc.model.GroupDTO
import com.agilxp.fessedebouc.model.InvitationDTO
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.coroutines.cancellation.CancellationException

class GroupHttpClient {

    companion object {
        @Throws(UnknownServerException::class, CancellationException::class)
        suspend fun getMyGroups(): List<GroupDTO> {
            val response = getPlatform().client.get("groups/mine")
            println("Response in Group client: $response")
            return response.body<List<GroupDTO>>()
        }

        @Throws(UnknownServerException::class, CancellationException::class)
        suspend fun searchGroup(searchQuery: String): List<GroupDTO> {
            return getPlatform().client.get("groups/search?name=$searchQuery").body<List<GroupDTO>>()
        }

        @Throws(UnknownServerException::class, ConflictException::class, CancellationException::class)
        suspend fun createGroup(group: GroupDTO): GroupDTO {
            return getPlatform().client.post("groups") {
                contentType(ContentType.Application.Json)
                setBody(group)
            }.body()
        }

        @Throws(UnknownServerException::class, ConflictException::class, CancellationException::class)
        suspend fun sendInvitation(email: String, groupUUID: String?) {
            if (groupUUID == null) {
                throw Exception("No group ID")
            }
            println("Sending invitation (client)")
            val response = getPlatform().client.post("groups/$groupUUID/invite/send") {
                contentType(ContentType.Application.Json)
                setBody(InvitationDTO(email))
            }
            println("Response in Group client: $response")
        }
    }
}