package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.*
import com.agilxp.fessedebouc.db.*
import com.agilxp.fessedebouc.db.UserGroups.groupId
import com.agilxp.fessedebouc.model.MessageDTO
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@OptIn(ExperimentalEncodingApi::class)
class MessageRoutingKtTest {

    @AfterTest
    fun tearDown() {
        container.stop()
    }

    @Test
    fun testNoMessages() = testApplication {
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
        val response = client.get("/messages/$groupUUID") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList(), response.body<List<MessageDTO>>())
    }

    @Test
    fun testGetMessages() = testApplication {
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
        environment {
            config = ApplicationConfig("application.conf")
        }
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
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun testPostMessage() = testApplication {
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
        val response = client.submitFormWithBinaryData(
            "/messages/$groupUUID",
            formData = formData {
                append("content", "Best message ever")
            }
        ) {
            onUpload { bytesSentTotal, contentLength ->
                println("Sent $bytesSentTotal bytes from $contentLength")
            }
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
        val expected = this::class.java.getResource("/application.conf")
        val response = client.submitFormWithBinaryData(
            "/messages/$groupUUID",
            formData = formData {
                append("content", "Best message ever")
                append("document", expected!!.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, "text/plain")
                    append(HttpHeaders.ContentDisposition, "filename=application.conf")
                })
            }
        ) {
            onUpload { bytesSentTotal, contentLength ->
                println("Sent $bytesSentTotal bytes from $contentLength")
            }
        }
        assertEquals(HttpStatusCode.Created, response.status)
        transaction {
            val messages = MessageDAO.all()
            assertEquals(1, messages.count())
            val myMessage = messages.first()
            assertEquals("Best message ever", myMessage.content)
            val f = File("tmp.conf")
            FileUtils.writeByteArrayToFile(f, Base64.decode(myMessage.document!!))
            assertContentEquals(expected!!.readBytes(), f.readBytes())
            assertEquals("text/plain", myMessage.documentContentType)
            assertEquals("application.conf", myMessage.documentFileName)
            assertEquals("admin", myMessage.sender.name)
            assertEquals("My First Group", myMessage.group.name)
            // Cleanup
            f.delete()
        }
    }
}