package com.agilxp.fessedebouc.httpclient

import com.agilxp.fessedebouc.ConflictException
import com.agilxp.fessedebouc.UnknownServerException
import com.agilxp.fessedebouc.getPlatform
import com.agilxp.fessedebouc.model.GroupDTO
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.coroutines.cancellation.CancellationException

class GroupHttpClient {

    companion object {
        @Throws(UnknownServerException::class, CancellationException::class)
        suspend fun getMyGroups(): List<GroupDTO> {
            return getPlatform().client.get("/groups/mine").body<List<GroupDTO>>()
        }

        @Throws(UnknownServerException::class, CancellationException::class)
        suspend fun searchGroup(searchQuery: String): List<GroupDTO> {
            return getPlatform().client.get("/groups/search?name=$searchQuery").body<List<GroupDTO>>()
        }

        @Throws(UnknownServerException::class, ConflictException::class, CancellationException::class)
        suspend fun createGroup(group: GroupDTO): GroupDTO {
            return getPlatform().client.post("/groups") {
                contentType(ContentType.Application.Json)
                setBody(group)
            }.body()
        }
    }
}