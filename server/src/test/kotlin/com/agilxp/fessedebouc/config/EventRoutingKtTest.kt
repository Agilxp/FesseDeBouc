package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.*
import com.agilxp.fessedebouc.db.EventDAO
import com.agilxp.fessedebouc.db.Groups
import com.agilxp.fessedebouc.db.UserGroups
import com.agilxp.fessedebouc.db.Users
import com.agilxp.fessedebouc.model.EventDTO
import com.agilxp.fessedebouc.util.EmailUtils
import com.agilxp.fessedebouc.util.KOffsetDateTimeSerializer
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EventRoutingKtTest {

    @BeforeTest
    fun setup() {
        mockkObject(EmailUtils)
        every { EmailUtils.sendEmail(any(), any(), any()) } returns Unit
    }

    @AfterTest
    fun tearDown() {
        container.stop()
    }

    @Test
    fun test403() = testApplication {
        environment {
            config = ApplicationConfig("application.conf")
        }
        application {
            testModule()
            transaction {
                UserGroups.insert {
                    it[userId] = EntityID(userUUID, Users)
                    it[groupId] = EntityID(groupUUID, Groups)
                    it[isAdmin] = false
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
        var response = client.get("/events/group/$groupUUID") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
        response = client.post("/events/group/$groupUUID") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }


    @Test
    fun testSuccess() = testApplication {
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
                UserGroups.insert {
                    it[userId] = EntityID(userUUID, Users)
                    it[groupId] = EntityID(groupUUID, Groups)
                    it[isAdmin] = false
                }
                val events = EventDAO.all()
                assertEquals(0, events.count())
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
        val event = EventDTO(
            "First event",
            "",
            KOffsetDateTimeSerializer.serialize(OffsetDateTime.now()),
            KOffsetDateTimeSerializer.serialize(OffsetDateTime.now().plusHours(2)),
            "My home"
        )
        var response = client.post("/events/group/$groupUUID") {
            contentType(ContentType.Application.Json)
            setBody(event)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val eventUUID = response.body<String>()
        transaction {
            val events = EventDAO.all()
            assertEquals(1, events.count())
            val e = events.first()
            assertEquals("First event", e.name)
            assertEquals(1, e.unanswered.count())
            assertEquals(0, e.declined.count())
            assertEquals(0, e.maybe.count())
            assertEquals(0, e.accepted.count())
        }
        response = client.get("/events/group/$groupUUID") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val eventResponse = response.body<List<EventDTO>>()
        assertEquals(listOf(event.name), response.body<List<EventDTO>>().map { it.name })
        val expectedStart = KOffsetDateTimeSerializer.deserialize(event.start).toInstant()
        assertEquals(expectedStart, KOffsetDateTimeSerializer.deserialize(eventResponse.first().start).toInstant())
        verify(exactly = 1) { EmailUtils.sendEmail(any(), any(), any()) }
        confirmVerified(EmailUtils)
        response = client.get("/events/reply/$eventUUID/accept") {
            assertEquals(HttpStatusCode.OK, response.status)
        }
        transaction {
            val events = EventDAO.all()
            assertEquals(1, events.count())
            val e = events.first()
            assertEquals("First event", e.name)
            assertEquals(1, e.unanswered.count())
            assertEquals(0, e.declined.count())
            assertEquals(0, e.maybe.count())
            assertEquals(1, e.accepted.count())
        }
        response = client.get("/events/reply/$eventUUID/maybe") {
            assertEquals(HttpStatusCode.OK, response.status)
        }
        transaction {
            val events = EventDAO.all()
            assertEquals(1, events.count())
            val e = events.first()
            assertEquals("First event", e.name)
            assertEquals(1, e.unanswered.count())
            assertEquals(0, e.declined.count())
            assertEquals(1, e.maybe.count())
            assertEquals(0, e.accepted.count())
        }
        response = client.get("/events/reply/$eventUUID/decline") {
            assertEquals(HttpStatusCode.OK, response.status)
        }
        transaction {
            val events = EventDAO.all()
            assertEquals(1, events.count())
            val e = events.first()
            assertEquals("First event", e.name)
            assertEquals(1, e.unanswered.count())
            assertEquals(1, e.declined.count())
            assertEquals(0, e.maybe.count())
            assertEquals(0, e.accepted.count())
        }
    }
}