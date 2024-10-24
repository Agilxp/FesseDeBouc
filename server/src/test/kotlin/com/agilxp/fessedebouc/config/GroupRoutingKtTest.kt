package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.*
import com.agilxp.fessedebouc.db.Groups
import com.agilxp.fessedebouc.db.UserGroups
import com.agilxp.fessedebouc.db.Users
import com.agilxp.fessedebouc.model.GroupDTO
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals


class GroupRoutingKtTest {

    @AfterTest
    fun tearDown() {
        container.stop()
    }

    @Test
    fun testGetGroupEmpty() = testApplication {
        environment {
            config = ApplicationConfig("application.conf")
        }
        application {
            testModule()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer ${getAdminUserToken()}")
            }
        }
        val response = client.get("/groups/mine")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList(), response.body<List<GroupDTO>>())
    }

    @Test
    fun testCreateGroup() = testApplication {
        environment {
            config = ApplicationConfig("application.conf")
        }
        application {
            testModule()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer ${getAdminUserToken()}")
            }
        }
        val response = client.post("/groups") {
            contentType(ContentType.Application.Json)
            setBody(group1)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(group1, response.body())
    }

    @Test
    fun testGetGroupNotEmpty() = testApplication {
        environment {
            config = ApplicationConfig("application.conf")
        }
        application {
            testModule()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer ${getAdminUserToken()}")
            }
        }
        client.post("/groups") {
            contentType(ContentType.Application.Json)
            setBody(group1)
        }
        val response = client.get("/groups/mine")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listOf(group1), response.body<List<GroupDTO>>())
    }

    @Test
    fun testDuplicateGroup() = testApplication {
        environment {
            config = ApplicationConfig("application.conf")
        }
        application {
            testModule()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer ${getAdminUserToken()}")
            }
        }
        var response = client.post("/groups") {
            contentType(ContentType.Application.Json)
            setBody(group1)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(group1, response.body())
        response = client.post("/groups") {
            contentType(ContentType.Application.Json)
            setBody(group1)
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun testSearchGroup() = testApplication {
        environment {
            config = ApplicationConfig("application.conf")
        }
        application {
            testModule()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer ${getAdminUserToken()}")
            }
        }
        client.post("/groups") {
            contentType(ContentType.Application.Json)
            setBody(group1)
        }
        var response = client.get("/groups/search?name=group")
        assertEquals(HttpStatusCode.OK, response.status)
        assertContentEquals(listOf(existingGroup, group1), response.body<List<GroupDTO>>())
        response = client.get("/groups/search?name=not")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList(), response.body<List<GroupDTO>>())
        response = client.get("/groups/search?name=")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList(), response.body<List<GroupDTO>>())
    }

    @Test
    fun testUpdateGroup() = testApplication {
        environment {
            config = ApplicationConfig("application.conf")
        }
        application {
            testModule()
            transaction {
                UserGroups.insert {
                    it[userId] = EntityID(adminUUID, Users)
                    it[groupId] = EntityID(groupUUID, Groups)
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
        val updateGroup = group1.copy(name = "Updated Group Name")
        val response = client.put("/groups/$groupUUID") {
            contentType(ContentType.Application.Json)
            setBody(updateGroup)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

}

val group1 = GroupDTO("Group 1", "Best group ever")
val existingGroup = GroupDTO("My First Group", "Best group ever")