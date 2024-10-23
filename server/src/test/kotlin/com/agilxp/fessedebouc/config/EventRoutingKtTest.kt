package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.container
import com.agilxp.fessedebouc.db.*
import com.agilxp.fessedebouc.getAdminUserToken
import com.agilxp.fessedebouc.model.EventDTO
import com.agilxp.fessedebouc.testModule
import com.agilxp.fessedebouc.util.EmailUtils
import com.agilxp.fessedebouc.util.KOffsetDateTimeSerializer
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
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
    fun test401() = testApplication {
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
        var response = client.get("/events/group/1") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        response = client.post("/events/group/1") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }


    @Test
    fun testSuccess() = testApplication {
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
        var response = client.post("/events/group/1") {
            contentType(ContentType.Application.Json)
            setBody(event)
        }
        assertEquals(HttpStatusCode.Created, response.status)
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
        response = client.get("/events/group/1") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val eventResponse = response.body<List<EventDTO>>()
        assertEquals(listOf(event.name), response.body<List<EventDTO>>().map { it.name })
        val expectedStart = KOffsetDateTimeSerializer.deserialize(event.start).toInstant()
        assertEquals(expectedStart, KOffsetDateTimeSerializer.deserialize(eventResponse.first().start).toInstant())
        verify(exactly = 1) { EmailUtils.sendEmail(any(), any(), any()) }
        confirmVerified(EmailUtils)
        response = client.get("/events/reply/1/accept") {
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
        response = client.get("/events/reply/1/maybe") {
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
        response = client.get("/events/reply/1/decline") {
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