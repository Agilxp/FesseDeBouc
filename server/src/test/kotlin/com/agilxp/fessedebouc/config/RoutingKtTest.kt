package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.container
import com.agilxp.fessedebouc.getAdminUserToken
import com.agilxp.fessedebouc.model.GroupDTO
import com.agilxp.fessedebouc.testModule
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals


class RoutingKtTest {

    @AfterTest
    fun tearDown() {
        container.stop()
    }

    @Test
    fun testGetGroupEmpty() = testApplication {
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
    fun testSearchGroup() = testApplication {
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
        assertEquals(listOf(group1), response.body<List<GroupDTO>>())
        response = client.get("/groups/search?name=not")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList(), response.body<List<GroupDTO>>())
        response = client.get("/groups/search?name=")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList(), response.body<List<GroupDTO>>())
    }
}

val group1 = GroupDTO("Group 1", "Best group ever")