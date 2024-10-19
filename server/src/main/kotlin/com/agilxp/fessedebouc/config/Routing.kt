package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.db.RequestStatus
import com.agilxp.fessedebouc.db.RequestType
import com.agilxp.fessedebouc.db.User
import com.agilxp.fessedebouc.model.*
import com.agilxp.fessedebouc.repository.*
import com.agilxp.fessedebouc.util.EmailUtils
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.jetbrains.exposed.exceptions.ExposedSQLException

fun Application.configureRouting(
    groupRepository: GroupRepository,
    userRepository: UserRepository,
    messageRepository: MessageRepository,
    eventRepository: EventRepository,
    joinGroupRequestRepository: JoinGroupRequestRepository,
    jwtConfig: JWTConfig,
) {
    install(ContentNegotiation) {
        json()
    }
    install(StatusPages) {
        exception<AuthenticationException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, cause.message)
        }
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message)
        }
        exception<DuplicateException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, cause.message)
        }
    }
    routing {
        authenticate("auth-session") {
            authenticate(jwtConfig.name) {
                route("/groups") {
                    get("/mine") {
                        val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                        val myGroups = groupRepository.getGroupsForUser(user)
                        call.respond(myGroups)
                    }
                    get("/search") {
                        val groupName: String = call.request.queryParameters["name"] ?: throw BadRequestException("Missing parameter")
                        call.respond(groupRepository.findByName(groupName))
                    }
                    post {
                        val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                        val groupToCreate = call.receive<GroupDTO>()
                        val createdGroup = groupRepository.createGroup(groupToCreate)
                        groupRepository.addUserToGroup(createdGroup, user, true)
                        call.respond(HttpStatusCode.Created, createdGroup)
                    }
                    route("/{groupId}") {
                        put {
                            val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                            val groupId = call.parameters["groupId"]?.toIntOrNull()
                            val groupToUpdate = call.receive<GroupDTO>()
                            if (groupId != null && isGroupAdmin(user, groupId, groupRepository)) {
                                groupRepository.updateGroup(groupId, groupToUpdate)
                                call.respond(HttpStatusCode.OK)
                            } else {
                                throw AuthenticationException("User ${user.id} not admin in group.")
                            }
                        }
                        route("/request") {
                            post("/send") {
                                val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                                val groupId = call.parameters["groupId"]?.toIntOrNull()
                                if (groupId != null) {
                                    val group = groupRepository.getGroupById(groupId)
                                        ?: throw BadRequestException("Invalid group id")
                                    val invitation =
                                        joinGroupRequestRepository.createRequest(
                                            user.email,
                                            RequestType.REQUEST,
                                            groupId
                                        )
                                    group.admins.forEach {
                                        EmailUtils.sendEmail(
                                            it.email,
                                            "${user.name} has requested to join ${group.name}",
                                            "${user.name} has requested to join ${group.name}.\nTo accept the request, go to http://localhost:8080/groups/${group.id}/request/${invitation.id}/accept\n" +
                                                    "To deny the request, go to http://localhost:8080/groups/${group.id}/request/${invitation.id}/deny\nThis is sent to all administrators of the group."
                                        )
                                    }
                                    call.respond(HttpStatusCode.OK)
                                } else {
                                    throw BadRequestException("Invalid group id")
                                }
                            }
                            route("/{invitationId}") {
                                get("/{action}") {
                                    val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                                    val groupId = call.parameters["groupId"]?.toIntOrNull()
                                    val invitationId = call.parameters["invitationId"]?.toIntOrNull()
                                    val action = call.parameters["action"] ?: throw BadRequestException("Invalid URL")
                                    if (invitationId != null && groupId != null && isGroupAdmin(
                                            user,
                                            groupId,
                                            groupRepository
                                        )
                                    ) {
                                        val group = groupRepository.getGroupById(groupId)
                                            ?: throw BadRequestException("Invalid group id")
                                        val invitation = joinGroupRequestRepository.findByIdAndGroup(
                                            invitationId,
                                            group.id,
                                        )
                                        if (invitation != null) {
                                            if (invitation.status == RequestStatus.PENDING) {
                                                when (action) {
                                                    "accept" -> {
                                                        try {
                                                            val u = userRepository.getUserByEmail(invitation.email)
                                                                ?: throw BadRequestException("Invalid request to join group")
                                                            groupRepository.addUserToGroup(group, u, false)
                                                        } catch (e: ExposedSQLException) {
                                                            println("Seems like this user is already in the group")
                                                        }
                                                        joinGroupRequestRepository.acceptRequest(invitation)
                                                    }

                                                    "deny" -> {
                                                        joinGroupRequestRepository.declineRequest(invitation)
                                                    }

                                                    else -> throw BadRequestException("Invalid action")
                                                }
                                                call.respond(HttpStatusCode.OK)
                                            } else {
                                                call.respond(
                                                    HttpStatusCode.OK,
                                                    "Invitation already ${invitation.status.name.lowercase()}"
                                                )
                                            }
                                        } else {
                                            throw BadRequestException("Not a valid invitation to accept")
                                        }
                                    }
                                }
                            }
                        }
                        route("/invite") {
                            post("/send") {
                                val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                                val groupId = call.parameters["groupId"]?.toIntOrNull()
                                if (groupId != null && isGroupAdmin(user, groupId, groupRepository)) {
                                    val group = groupRepository.getGroupById(groupId)
                                        ?: throw BadRequestException("Invalid group id")
                                    val email = call.receive<InvitationDTO>().email
                                    val invitation =
                                        joinGroupRequestRepository.createRequest(
                                            email,
                                            RequestType.INVITATION,
                                            groupId
                                        )
                                    EmailUtils.sendEmail(
                                        email,
                                        "${user.name} has invited you to join ${group.name}",
                                        "${user.name} has invited you to join ${group.name}.\nTo accept the invitation, go to http://localhost:8080/groups/${group.id}/invite/${invitation.id}/accept\nYou will need to log in or create a free account if you don't already have one."
                                    )
                                    call.respond(HttpStatusCode.OK)
                                } else {
                                    throw AuthenticationException("User ${user.id} not admin in group.")
                                }
                            }
                            route("/{invitationId}") {
                                get("/accept") {
                                    val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                                    val groupId = call.parameters["groupId"]?.toIntOrNull()
                                    val invitationId = call.parameters["invitationId"]?.toIntOrNull()
                                    if (invitationId != null && groupId != null) {
                                        val group = groupRepository.getGroupById(groupId)
                                            ?: throw BadRequestException("Invalid group id")
                                        val invitation = joinGroupRequestRepository.findByIdGroupAndEmail(
                                            invitationId,
                                            group.id,
                                            user.email
                                        )
                                        if (
                                            invitation != null
                                        ) {
                                            try {
                                                groupRepository.addUserToGroup(group, user, false)
                                            } catch (e: ExposedSQLException) {
                                                println("Seems like this user is already in the group")
                                            }
                                            joinGroupRequestRepository.acceptRequest(invitation)
                                            call.respond(HttpStatusCode.OK)
                                        } else {
                                            throw BadRequestException("Not a valid invitation to accept")
                                        }
                                    } else {
                                        throw BadRequestException("Invalid URL")
                                    }
                                }
                            }
                        }
                    }
                }
                route("/messages") {
                    get("/{groupId}") {
                        val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                        val groupId = call.parameters["groupId"]?.toIntOrNull()
                        if (groupId != null && isUserInGroup(user, groupId, groupRepository)) {
                            val messages = messageRepository.getMessagesForGroup(groupId)
                            call.respond(messages)
                        } else {
                            throw BadRequestException("Group id missing")
                        }
                    }
                    post("/{groupId}") {
                        val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                        val groupId = call.parameters["groupId"]?.toIntOrNull()
                        if (groupId != null && isUserInGroup(user, groupId, groupRepository)) {
                            val message = call.receive<String>()
                            if (message.isEmpty()) {
                                throw BadRequestException("Message cannot be empty")
                            }
                            println("Message: $message")
                            messageRepository.addMessageToGroup(message, user.id, groupId)
                            call.respond(HttpStatusCode.OK)
                        } else {
                            throw BadRequestException("Group id missing")
                        }
                    }
                }
                route("/users") {
                    get("/{groupId}") {
                        val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                        val groupId = call.parameters["groupId"]?.toIntOrNull()
                        if (groupId != null && isUserInGroup(user, groupId, groupRepository)) {
                            val users = groupRepository.getGroupById(groupId)?.users ?: emptyList()
                            call.respond(users)
                        } else {
                            throw BadRequestException("Group id missing")
                        }
                    }
                }
                route("/events") {
                    get("/{groupId}") {
                        val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                        val groupId = call.parameters["groupId"]?.toIntOrNull()
                        if (groupId != null && isUserInGroup(user, groupId, groupRepository)) {
                            val events = eventRepository.getEventsForGroup(groupId)
                            call.respond(events)
                        } else {
                            throw BadRequestException("Group id missing")
                        }
                    }
                    post("/{groupId}") {
                        val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                        val groupId = call.parameters["groupId"]?.toIntOrNull()
                        if (groupId != null && isUserInGroup(user, groupId, groupRepository)) {
                            val event = call.receive<EventDTO>()
                            eventRepository.createEvent(
                                event,
                                user.id,
                                groupId
                            )
                            call.respond(HttpStatusCode.OK)
                            // TODO send email to everyone in the group
                        } else {
                            throw BadRequestException("Group id missing")
                        }
                    }
                }
                route("/accept") {
                    route("/event") {
                        get("/{eventId}") {
                            val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                            val eventId = call.parameters["eventId"]?.toIntOrNull()
                            if (eventId != null) {
                                val event = eventRepository.getEventById(eventId)
                                if (event == null) {
                                    throw BadRequestException("Invalid event id")
                                }
                                if (isUserInGroup(user, event.group.id, groupRepository)) {
                                    eventRepository.acceptEvent(event, user)
                                } else {
                                    throw BadRequestException("User is not associated with the group for this event")
                                }
                                call.respond(HttpStatusCode.OK)
                            } else {
                                throw BadRequestException("Event id missing")
                            }
                        }
                    }
                }
            }
        }
    }
}

suspend fun isUserInGroup(user: User, groupId: Int, groupRepository: GroupRepository): Boolean {
    val group = groupRepository.getGroupById(groupId) ?: throw BadRequestException("Invalid group id")
    if (group.users.contains(user)) {
        return true
    } else {
        throw AuthenticationException("User not in group")
    }
}

suspend fun isGroupAdmin(user: User, groupId: Int, groupRepository: GroupRepository): Boolean {
    val group = groupRepository.getGroupById(groupId) ?: throw BadRequestException("Invalid group id")
    if (group.admins.contains(user)) {
        return true
    } else {
        throw AuthenticationException("User not in group")
    }
}

suspend fun getInfoFromPrincipal(call: ApplicationCall, jwtConfig: JWTConfig, userRepository: UserRepository): User {
    val principal = call.principal<JWTPrincipal>(jwtConfig.name)
    if (principal != null) {
        val userId: Int =
            principal.payload.getClaim("user_id").asInt() ?: throw AuthenticationException("Invalid principal")
        val email: String =
            principal.payload.getClaim("user_email").asString() ?: throw AuthenticationException("Invalid principal")
        val googleId: String =
            principal.payload.getClaim("google_id").asString() ?: throw AuthenticationException("Invalid principal")
        val user = userRepository.getUserById(userId) ?: throw AuthenticationException("Invalid principal")
        if (user.email == email && user.googleId == googleId) {
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

data class DuplicateException(override val message: String) : Exception()
