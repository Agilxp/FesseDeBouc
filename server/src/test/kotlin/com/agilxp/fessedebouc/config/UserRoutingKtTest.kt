package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.container
import com.agilxp.fessedebouc.db.Groups
import com.agilxp.fessedebouc.db.UserGroups
import com.agilxp.fessedebouc.db.Users
import com.agilxp.fessedebouc.getAdminUserToken
import com.agilxp.fessedebouc.model.UserDTO
import com.agilxp.fessedebouc.testModule
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserRoutingKtTest {

    @AfterTest
    fun tearDown() {
        container.stop()
    }

    @Test
    fun testBadRequest() = testApplication {
        application {
            testModule()
            transaction {
                UserGroups.insert {
                    it[userId] = EntityID(1, Users)
                    it[groupId] = EntityID(1, Groups)
                    it[isAdmin] = true
                }
                UserGroups.insert {
                    it[userId] = EntityID(2, Users)
                    it[groupId] = EntityID(1, Groups)
                    it[isAdmin] = true
                }
            }
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer ${getAdminUserToken()}")
            }
        }
        val response = client.get("/users/abc") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testNotInGroup() = testApplication {
        application {
            testModule()
            transaction {
                UserGroups.insert {
                    it[userId] = EntityID(2, Users)
                    it[groupId] = EntityID(1, Groups)
                    it[isAdmin] = true
                }
            }
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer ${getAdminUserToken()}")
            }
        }
        val response = client.get("/users/1") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun getUsers() = testApplication {
        application {
            testModule()
            transaction {
                UserGroups.insert {
                    it[userId] = EntityID(1, Users)
                    it[groupId] = EntityID(1, Groups)
                    it[isAdmin] = true
                }
                UserGroups.insert {
                    it[userId] = EntityID(2, Users)
                    it[groupId] = EntityID(1, Groups)
                    it[isAdmin] = true
                }
            }
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer ${getAdminUserToken()}")
            }
        }
        val response = client.get("/users/1") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val users: List<String> = response.body<List<UserDTO>>().map { it.name }
        assertEquals(listOf("admin", "user"), users)
    }
}