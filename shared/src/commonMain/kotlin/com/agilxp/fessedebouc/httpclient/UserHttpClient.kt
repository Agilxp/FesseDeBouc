package com.agilxp.fessedebouc.httpclient

import com.agilxp.fessedebouc.getPlatform
import com.agilxp.fessedebouc.model.UserStatusDTO
import io.ktor.client.call.*
import io.ktor.client.request.*

class UserHttpClient {

    companion object {
        suspend fun getUserStatus(): UserStatusDTO {
            return getPlatform().client.get("/api/me").body<UserStatusDTO>()
        }
    }
}