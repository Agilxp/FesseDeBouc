package com.agilxp.fessedebouc

import com.agilxp.fessedebouc.config.configureDatabases
import com.agilxp.fessedebouc.config.configureRouting
import com.agilxp.fessedebouc.db.Users
import com.agilxp.fessedebouc.model.JWTConfig
import com.agilxp.fessedebouc.model.UserDTO
import com.agilxp.fessedebouc.model.UserSession
import com.agilxp.fessedebouc.model.createToken
import com.agilxp.fessedebouc.repository.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Clock
import kotlin.test.Test

class ApplicationKtTest {

    @Test
    fun testMainModule() = testApplication {
        application {
            module()
        }
    }

}

fun Application.configureTestAuth(jwtConfig: JWTConfig) {
    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.maxAgeInSeconds = 1800
            cookie.path = "/"
        }
    }
    install(Authentication) {
        session<UserSession>("auth-session") {
            validate { session ->
                if (session.accessToken.isNotBlank()) {
                    session
                } else {
                    null
                }
            }
            challenge {
                call.respondRedirect("/login")
            }
        }
        jwt(jwtConfig.name) {
            realm = jwtConfig.realm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtConfig.secret))
                    .withAudience(jwtConfig.audience)
                    .withIssuer(jwtConfig.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtConfig.audience)) JWTPrincipal(credential.payload) else null
            }
            challenge { scheme, realm ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    "Token to access ${HttpHeaders.WWWAuthenticate} $scheme realm=\"$realm\" is either invalid or expired."
                )
            }
        }
    }
}

val container = PostgreSQLContainer<Nothing>("postgres:16")
val jwtConfig = JWTConfig("name", "realm", "secret", "audience", "issuer")

fun Application.testModule() {
    val dbConfig = environment.config.config("ktor.db").dbConfig()
    container.withDatabaseName(dbConfig.url.split("/").last())
    container.withUsername(dbConfig.username)
    container.withPassword(dbConfig.password)
    container.withReuse(true)
    container.exposedPorts = listOf(5433)
    container.portBindings = listOf("5433:5432")
    if (!container.isRunning) {
        container.start()
    }
    val groupRepository = GroupRepositoryImpl()
    val userRepository = UserRepositoryImpl()
    val messageRepository = MessageRepositoryImpl()
    val eventRepository = EventRepositoryImpl()
    val joinGroupRequestRepository = JoinGroupRequestRepositoryImpl()
    configureTestAuth(jwtConfig)
    configureDatabases(dbConfig)
    val admin = getAdminUser()
    val nonAdmin = getNonAdminUser()
    transaction {
        Users.insert {
            it[name] = admin.name
            it[email] = admin.email
            it[google_id] = admin.googleId
        }
        Users.insert {
            it[name] = nonAdmin.name
            it[email] = nonAdmin.email
            it[google_id] = nonAdmin.googleId
        }
    }
    configureRouting(
        groupRepository,
        userRepository,
        messageRepository,
        eventRepository,
        joinGroupRequestRepository,
        jwtConfig
    )
}

fun getAdminUser(): UserDTO {
    val adminUser = UserDTO(
        "admin",
        "admin@example.com",
        "GOOGLE1"
    )
    return adminUser
}

fun getNonAdminUser(): UserDTO {
    val adminUser = UserDTO(
        "user",
        "user@example.com",
        "GOOGLE2"
    )
    return adminUser
}

fun getAdminUserToken(): String {
    val admin = getAdminUser()
    val adminUserToken = jwtConfig.createToken(
        clock = Clock.systemUTC(),
        accessToken = "access-token",
        userId = 1,
        userEmail = admin.email,
        googleId = admin.googleId,
        expirationSeconds = 3600
    )
    return adminUserToken
}

