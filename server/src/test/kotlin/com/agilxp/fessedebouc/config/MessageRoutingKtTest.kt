package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.*
import com.agilxp.fessedebouc.db.*
import com.agilxp.fessedebouc.db.UserGroups.groupId
import com.agilxp.fessedebouc.model.MessageDTO
import com.agilxp.fessedebouc.model.PostMessageDTO
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
import org.testcontainers.shaded.org.apache.commons.io.FileUtils
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalEncodingApi::class)
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
        val response = client.get("/messages/$groupUUID") {
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
                    it[userId] = EntityID(adminUUID, Users)
                    it[groupId] = EntityID(groupUUID, Groups)
                    it[isAdmin] = true
                }
                Messages.insert {
                    it[groupId] = EntityID(groupUUID, Groups)
                    it[sender] = EntityID(adminUUID, Users)
                    it[content] = "First message"
                }
                Messages.insert {
                    it[groupId] = EntityID(groupUUID, Groups)
                    it[sender] = EntityID(adminUUID, Users)
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
        val response = client.get("/messages/$groupUUID") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val messages: List<String> = response.body<List<MessageDTO>>().map { it.content!! }
        assertEquals(listOf("First message", "Second message"), messages)
    }

    @Test
    fun testGetMessagesNotInGroup() = testApplication {
        application {
            testModule()
            transaction {
                Messages.insert {
                    it[groupId] = EntityID(groupUUID, Groups)
                    it[sender] = EntityID(adminUUID, Users)
                    it[content] = "First message"
                }
                Messages.insert {
                    it[groupId] = EntityID(groupUUID, Groups)
                    it[sender] = EntityID(adminUUID, Users)
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
        val response = client.get("/messages/$groupUUID") {
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
                    it[userId] = EntityID(adminUUID, Users)
                    it[groupId] = EntityID(groupUUID, Groups)
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
        val postMessage = PostMessageDTO(content = "Best message ever")
        val response = client.post("/messages/$groupUUID") {
            contentType(ContentType.Application.Json)
            setBody(postMessage)
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

    @Test
    fun testPostMessageWithImage() = testApplication {
        application {
            testModule()
            transaction {
                UserGroups.insert {
                    it[userId] = EntityID(adminUUID, Users)
                    it[groupId] = EntityID(groupUUID, Groups)
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
        val expected = this::class.java.getResource("application.conf")
        val postMessage = PostMessageDTO(content = "Best message ever", document = Base64.encode(expected!!.readBytes()))
        val response = client.post("/messages/$groupUUID") {
            contentType(ContentType.Application.Json)
            setBody(postMessage)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        transaction {
            val messages = MessageDAO.all()
            assertEquals(1, messages.count())
            val myMessage = messages.first()
            assertEquals("Best message ever", myMessage.content)
            val f = File("tmp.conf")
            FileUtils.writeByteArrayToFile(f, Base64.decode(myMessage.document!!))
            assertEquals(expected.readBytes(), f.readBytes())
            assertEquals("admin", myMessage.sender.name)
            assertEquals("My First Group", myMessage.group.name)
        }
    }
}