package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.container
import com.agilxp.fessedebouc.db.*
import com.agilxp.fessedebouc.db.UserGroups.groupId
import com.agilxp.fessedebouc.getAdminUserToken
import com.agilxp.fessedebouc.model.MessageDTO
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
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageRoutingKtTest {

    @AfterTest
    fun tearDown() {
        container.stop()
    }

    @Test
    fun testNoMessages() = testApplication {
        application {
            testModule()
            transaction {
                UserGroups.insert {
                    it[userId] = EntityID(1, Users)
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
        val response = client.get("/messages/1") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList(), response.body<List<MessageDTO>>())
    }

    @Test
    fun testGetMessages() = testApplication {
        application {
            testModule()
            transaction {
                UserGroups.insert {
                    it[userId] = EntityID(1, Users)
                    it[groupId] = EntityID(1, Groups)
                    it[isAdmin] = true
                }
                Messages.insert {
                    it[groupId] = EntityID(1, Groups)
                    it[sender] = EntityID(1, Users)
                    it[content] = "First message"
                }
                Messages.insert {
                    it[groupId] = EntityID(1, Groups)
                    it[sender] = EntityID(1, Users)
                    it[content] = "Second message"
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
        val response = client.get("/messages/1") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val messages: List<String> = response.body<List<MessageDTO>>().map { it.content }
        assertEquals(listOf("First message", "Second message"), messages)
    }

    @Test
    fun testGetMessagesNotInGroup() = testApplication {
        application {
            testModule()
            transaction {
                Messages.insert {
                    it[groupId] = EntityID(1, Groups)
                    it[sender] = EntityID(1, Users)
                    it[content] = "First message"
                }
                Messages.insert {
                    it[groupId] = EntityID(1, Groups)
                    it[sender] = EntityID(1, Users)
                    it[content] = "Second message"
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
        val response = client.get("/messages/1") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testPostMessage() = testApplication {
        application {
            testModule()
            transaction {
                UserGroups.insert {
                    it[userId] = EntityID(1, Users)
                    it[groupId] = EntityID(1, Groups)
                    it[isAdmin] = true
                }
                assertEquals(0, Messages.selectAll().count())
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
        val response = client.post("/messages/1") {
            contentType(ContentType.Application.Json)
            setBody("Best message ever")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        transaction {
            val messages = MessageDAO.all()
            assertEquals(1, messages.count())
            val myMessage = messages.first()
            assertEquals("Best message ever", myMessage.content)
            assertEquals("admin", myMessage.sender.name)
            assertEquals("My First Group", myMessage.group.name)
        }
    }
}