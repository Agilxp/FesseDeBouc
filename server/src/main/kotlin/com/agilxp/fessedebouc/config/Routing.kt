package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.db.GroupDAO
import com.agilxp.fessedebouc.db.Message
import com.agilxp.fessedebouc.db.User
import com.agilxp.fessedebouc.model.JWTConfig
import com.agilxp.fessedebouc.model.UserSession
import com.agilxp.fessedebouc.repository.GroupRepository
import com.agilxp.fessedebouc.repository.MessageRepository
import com.agilxp.fessedebouc.repository.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting(
    groupRepository: GroupRepository,
    userRepository: UserRepository,
    messageRepository: MessageRepository,
    jwtConfig: JWTConfig,
) {
    install(ContentNegotiation) {
        json()
    }
    routing {
        authenticate("auth-session") {
            authenticate(jwtConfig.name) {
                route("/messages") {
                    get("/{groupId}") {
                        try {
                            val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                            val groupId = call.parameters["groupId"]?.toIntOrNull()
                            if (groupId != null && isUserInGroup(user, groupId, groupRepository)) {
                                val messages = messageRepository.getMessagesForGroup(groupId)
                                call.respond(messages)
                            } else {
                                throw BadRequestException("Group id missing")
                            }
                        } catch (e: AuthenticationException) {
                            call.respond(status = HttpStatusCode.Unauthorized, message = e.message)
                        } catch (e: BadRequestException) {
                            call.respond(status = HttpStatusCode.BadRequest, message = e.message)
                        }
                    }
                    post("/{groupId}") {
                        try {
                            val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                            val groupId = call.parameters["groupId"]?.toIntOrNull()
                            if (groupId != null && isUserInGroup(user, groupId, groupRepository)) {
                                val message = call.receive<String>()
                                if (message.isEmpty()) {
                                    throw BadRequestException("Message cannot be empty")
                                }
                                println("Message: $message")
                                messageRepository.addMessageForGroup(message, user.id, groupId)
                                call.respond(HttpStatusCode.OK)
                            } else {
                                throw BadRequestException("Group id missing")
                            }
                        } catch (e: AuthenticationException) {
                            call.respond(status = HttpStatusCode.Unauthorized, message = e.message)
                        } catch (e: BadRequestException) {
                            call.respond(status = HttpStatusCode.BadRequest, message = e.message)
                        }
                    }
                }
                route("/users") {
                    get("/{groupId}") {
                        try {
                            val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                            val groupId = call.parameters["groupId"]?.toIntOrNull()
                            if (groupId != null && isUserInGroup(user, groupId, groupRepository)) {
                                val users = groupRepository.getGroupById(groupId)?.users ?: emptyList()
                                call.respond(users)
                            } else {
                                throw BadRequestException("Group id missing")
                            }
                        } catch (e: AuthenticationException) {
                            call.respond(status = HttpStatusCode.Unauthorized, message = e.message)
                        } catch (e: BadRequestException) {
                            call.respond(status = HttpStatusCode.BadRequest, message = e.message)
                        }
                    }
                }
            }
        }
    }
}

suspend fun isUserInGroup(user: User, groupId: Int, groupRepository: GroupRepository): Boolean {
    val group = groupRepository.getGroupById(groupId)
    if (group == null) {
        throw BadRequestException("Invalid group id")
    }
    if (group.users.contains(user)) {
        return true
    } else {
        throw AuthenticationException("User not in group")
    }
}

suspend fun getInfoFromPrincipal(call: ApplicationCall, jwtConfig: JWTConfig, userRepository: UserRepository): User {
    val principal = call.principal<JWTPrincipal>(jwtConfig.name)
    if (principal != null) {
        val userId = principal.payload.getClaim("user_id").asInt()
        val email = principal.payload.getClaim("user_email").asString()
        val googleId = principal.payload.getClaim("google_id").asString()
        val user = userRepository.getUserById(userId)
        if (user?.email == email && user.googleId == googleId) {
            return user
        } else {
            throw AuthenticationException("Invalid principal")
        }
    } else {
        val userSession = call.principal<UserSession>("auth-session")
        if (userSession != null) {
            val user = userRepository.getUserById(userSession.userId)
            if (user?.email == userSession.userEmail && user.googleId == userSession.googleId) {
                return user
            } else {
                throw AuthenticationException("Invalid principal")
            }
        } else {
            throw AuthenticationException("No principal found")
        }
    }
}

data class AuthenticationException(override val message: String) : Exception()

data class BadRequestException(override val message: String) : Exception()
