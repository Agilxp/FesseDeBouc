package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.*
import com.agilxp.fessedebouc.db.*
import com.agilxp.fessedebouc.model.InvitationDTO
import com.agilxp.fessedebouc.util.EmailUtils
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.*
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


class InvitationRoutingKtTest {

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
    fun testSendRequest() = testApplication {
        environment {
            config = ApplicationConfig("application.conf")
        }
        application {
            testModule()
            transaction {
                assertEquals(0, JoinGroupRequests.selectAll().count())
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
        val response = client.post("/groups/$groupUUID/request/send") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        transaction {
            assertEquals(1, JoinGroupRequests.selectAll().count())
        }
    }

    @Test
    fun testSendRequestAlreadyIn() = testApplication {
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
        val response = client.post("/groups/$groupUUID/request/send") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("You are already in the group: My First Group.", response.body<String>())
    }

    @Test
    fun testKick() = testApplication {
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
                assertEquals(2, UserGroups.selectAll().count())
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
        val response = client.delete("/groups/$groupUUID/kick/$userUUID") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        transaction {
            assertEquals(1, UserGroups.selectAll().count())
        }
    }

    @Test
    fun testSendInvitation() = testApplication {
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
                assertEquals(0, JoinGroupRequests.selectAll().count())
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
        val response = client.post("/groups/$groupUUID/invite/send") {
            contentType(ContentType.Application.Json)
            setBody(InvitationDTO("dummy@example.com"))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        transaction {
            val req = JoinGroupRequestDAO.all()
            assertEquals(1, req.count())
            assertEquals("dummy@example.com", req.first().email)
            assertEquals(RequestStatus.PENDING, req.first().status)
        }
        verify(exactly = 1) { EmailUtils.sendEmail(any(), any(), any())  }
        confirmVerified(EmailUtils)
    }

    @Test
    fun testAcceptInvitation() = testApplication {
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
                    it[type] = RequestType.REQUEST
                }
                val req = JoinGroupRequestDAO.all()
                assertEquals(1, req.count())
                assertEquals("user@example.com", req.first().email)
                assertEquals(RequestStatus.PENDING, req.first().status)
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
        val response = client.get("/groups/$groupUUID/invite/$invitationUUID/accept")
        assertEquals(HttpStatusCode.OK, response.status)
        transaction {
            val req = JoinGroupRequestDAO.all()
            assertEquals(1, req.count())
            assertEquals("user@example.com", req.first().email)
            assertEquals(RequestStatus.ACCEPTED, req.first().status)
        }
        verify(exactly = 1) { EmailUtils.sendEmail(any(), any(), any())  }
        confirmVerified(EmailUtils)
    }

    @Test
    fun testDenyInvitation() = testApplication {
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
                    it[type] = RequestType.REQUEST
                }
                val req = JoinGroupRequestDAO.all()
                assertEquals(1, req.count())
                assertEquals("user@example.com", req.first().email)
                assertEquals(RequestStatus.PENDING, req.first().status)
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
        val response = client.get("/groups/$groupUUID/invite/$invitationUUID/deny")
        assertEquals(HttpStatusCode.OK, response.status)
        transaction {
            val req = JoinGroupRequestDAO.all()
            assertEquals(1, req.count())
            assertEquals("user@example.com", req.first().email)
            assertEquals(RequestStatus.DECLINED, req.first().status)
        }
        verify(exactly = 1) { EmailUtils.sendEmail(any(), any(), any())  }
        confirmVerified(EmailUtils)
    }

}
