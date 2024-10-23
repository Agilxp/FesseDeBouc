package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.container
import com.agilxp.fessedebouc.db.*
import com.agilxp.fessedebouc.getAdminUserToken
import com.agilxp.fessedebouc.model.InvitationDTO
import com.agilxp.fessedebouc.testModule
import com.agilxp.fessedebouc.util.EmailUtils
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
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
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
        val response = client.post("/groups/1/request/send") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        transaction {
            assertEquals(1, JoinGroupRequests.selectAll().count())
        }
    }

    @Test
    fun testSendRequestAlreadyIn() = testApplication {
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
        val response = client.post("/groups/1/request/send") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("You are already in the group: My First Group.", response.body<String>())
    }

    @Test
    fun testKick() = testApplication {
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
        val response = client.delete("/groups/1/kick/2") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        transaction {
            assertEquals(1, UserGroups.selectAll().count())
        }
    }

    @Test
    fun testSendInvitation() = testApplication {
        application {
            testModule()
            transaction {
                UserGroups.insert {
                    it[userId] = EntityID(1, Users)
                    it[groupId] = EntityID(1, Groups)
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
        val response = client.post("/groups/1/invite/send") {
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
        application {
            testModule()
            transaction {
                UserGroups.insert {
                    it[userId] = EntityID(1, Users)
                    it[groupId] = EntityID(1, Groups)
                    it[isAdmin] = true
                }
                JoinGroupRequests.insert {
                    it[email] = "user@example.com"
                    it[status] = RequestStatus.PENDING
                    it[group] = EntityID(1, Groups)
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
        val response = client.get("/groups/1/invite/1/accept")
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
        application {
            testModule()
            transaction {
                UserGroups.insert {
                    it[userId] = EntityID(1, Users)
                    it[groupId] = EntityID(1, Groups)
                    it[isAdmin] = true
                }
                JoinGroupRequests.insert {
                    it[email] = "user@example.com"
                    it[status] = RequestStatus.PENDING
                    it[group] = EntityID(1, Groups)
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
        val response = client.get("/groups/1/invite/1/deny")
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
