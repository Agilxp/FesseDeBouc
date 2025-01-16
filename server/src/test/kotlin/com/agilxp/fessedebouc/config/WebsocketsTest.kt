package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.*
import com.agilxp.fessedebouc.db.*
import com.agilxp.fessedebouc.model.JoinGroupRequestDTO
import com.agilxp.fessedebouc.model.UserStatusDTO
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class WebsocketsTest {

    @AfterTest
    fun tearDown() {
        container.stop()
    }

    @Test
    fun testConnectionFail() = testApplication {
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
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
            }
        }
        var counter = 0
        client.webSocket("/ws/me") {
            for (frame in incoming) {
                counter++
                if (counter == 5) {
                    close(CloseReason(CloseReason.Codes.NORMAL, "Client disconnected"))
                }
            }
        }
        assertEquals(0, counter)
    }

    @Test
    fun testConnection() = testApplication {
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
                JoinGroupRequests.insert {
                    it[id] = invitationUUID
                    it[email] = "user@example.com"
                    it[status] = RequestStatus.PENDING
                    it[group] = EntityID(groupUUID, Groups)
                    it[type] = RequestType.INVITATION
                }
                val req = JoinGroupRequestDAO.all()
                assertEquals(1, req.count())
                assertEquals("user@example.com", req.first().email)
                assertEquals(RequestStatus.PENDING, req.first().status)
            }
        }

        val userClient = createClient {
            install(ContentNegotiation) {
                json()
            }
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
            }
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer ${getNonAdminUserToken()}")
            }
        }

        val adminClient = createClient {
            install(ContentNegotiation) {
                json()
            }
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
            }
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer ${getAdminUserToken()}")
            }
        }

        val userStatus = mutableListOf<UserStatusDTO>()
        val adminStatus = mutableListOf<UserStatusDTO>()
        runBlocking {
            launch {
                println("Connecting user client")
                userClient.webSocket("/ws/me?at=${getNonAdminUserToken()}") {
                    for (frame in incoming) {
                        val updatedStatus = converter!!.deserialize<UserStatusDTO>(frame)
                        userStatus.add(updatedStatus)
                        close(CloseReason(CloseReason.Codes.NORMAL, "Client disconnected"))
                    }
                }
            }
            delay(500)
            println("Connecting admin client")
            adminClient.webSocket("/ws/me?at=${getAdminUserToken()}") {
                for (frame in incoming) {
                    val updatedStatus = converter!!.deserialize<UserStatusDTO>(frame)
                    adminStatus.add(updatedStatus)
                    close(CloseReason(CloseReason.Codes.NORMAL, "Client disconnected"))
                }
            }
            val emptyStatus = UserStatusDTO(emptyList(), emptyList(), emptyList())
            val emptyStatuses = mutableListOf(emptyStatus)
            assertContentEquals(emptyStatuses, adminStatus)
            val expectedUserStatus = UserStatusDTO(
                emptyList(),
                listOf(
                    JoinGroupRequestDTO(
                        invitationUUID.toString(), existingGroup.copy(
                            id = groupUUID.toString(),
                            users = listOf(getAdminUser()),
                            admins = listOf(getAdminUser())
                        ), "PENDING"
                    )
                ),
                emptyList()
            )
            val userStatuses = mutableListOf(expectedUserStatus)
            assertContentEquals(userStatuses, userStatus)
        }
    }
}