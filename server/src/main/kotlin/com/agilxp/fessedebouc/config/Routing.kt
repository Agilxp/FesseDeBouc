package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.db.GroupDAO
import com.agilxp.fessedebouc.db.RequestStatus
import com.agilxp.fessedebouc.db.RequestType
import com.agilxp.fessedebouc.db.UserDAO
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
                        call.respond(myGroups.map { it.toDto() })
                    }
                    get("/search") {
                        val groupName: String =
                            call.request.queryParameters["name"] ?: throw BadRequestException("Missing parameter")
                        if (groupName.trim().isEmpty()) {
                            call.respond(emptyList<GroupDTO>())
                        } else {
                            call.respond(groupRepository.findByName(groupName).map { it.toDto() })
                        }
                    }
                    post {
                        val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                        val groupToCreate = call.receive<GroupDTO>()
                        val createdGroup = groupRepository.createGroup(groupToCreate)
                        groupRepository.addUserToGroup(createdGroup, user, true)
                        call.respond(HttpStatusCode.Created, createdGroup.toDto())
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
                        route("/kick") {
                            delete("/{userId}") {
                                val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                                val groupId = call.parameters["groupId"]?.toIntOrNull()
                                val userId = call.parameters["userId"]?.toIntOrNull()
                                if (groupId != null && userId != null && isGroupAdmin(user, groupId, groupRepository)) {
                                    groupRepository.removeUserFromGroup(groupId, userId)
                                    call.respond(HttpStatusCode.OK)
                                } else {
                                    throw AuthenticationException("User ${user.id} not admin in group.")
                                }
                            }
                        }
                        route("/request") {
                            post("/send") {
                                val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                                val groupId = call.parameters["groupId"]?.toIntOrNull()
                                if (groupId != null) {
                                    val group = groupRepository.getGroupById(groupId)
                                        ?: throw BadRequestException("Invalid group id")
                                    val joinRequest = joinGroupRequestRepository.findByGroupEmailAndStatus(
                                        groupId,
                                        user.email,
                                        listOf(RequestStatus.PENDING)
                                    )
                                    if (joinRequest != null) {
                                        throw BadRequestException("You already requested to join ${group.name}.")
                                    }
                                    if (group.users.toList().contains(user)) {
                                        throw BadRequestException("You are already in the group: ${group.name}.")
                                    }
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
                                            group.id.value,
                                        )
                                        if (invitation != null) {
                                            if (invitation.status == RequestStatus.PENDING) {
                                                var pastTenseAction = ""
                                                var text = ""
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
                                                        pastTenseAction = "accepted"
                                                        text = "You can now access the group in the application."
                                                    }

                                                    "deny" -> {
                                                        joinGroupRequestRepository.declineRequest(invitation)
                                                        pastTenseAction = "denied"
                                                        text =
                                                            "An admin has denied your request. You might need to contact them directly by other means to get an invitation."
                                                    }

                                                    else -> throw BadRequestException("Invalid action")
                                                }
                                                EmailUtils.sendEmail(
                                                    invitation.email,
                                                    "Your request to join ${group.name} has been ${pastTenseAction}.",
                                                    text
                                                )
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
                                            group.id.value,
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
                        if (groupId != null) {
                            val group = isUserInGroup(user, groupId, groupRepository)
                            val messages = messageRepository.getMessagesForGroup(group)
                            call.respond(messages.map { it.toDto() })
                        } else {
                            throw BadRequestException("Group id missing")
                        }
                    }
                    post("/{groupId}") {
                        val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                        val groupId = call.parameters["groupId"]?.toIntOrNull()
                        if (groupId != null) {
                            val group = isUserInGroup(user, groupId, groupRepository)
                            val message = call.receive<String>()
                            if (message.isEmpty()) {
                                throw BadRequestException("Message cannot be empty")
                            }
                            println("Message: $message")
                            messageRepository.addMessageToGroup(message, user, group)
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
                        if (groupId != null) {
                            val group = isUserInGroup(user, groupId, groupRepository)
                            val users = group.users.map { it.toDTO() }
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
                        if (groupId != null) {
                            val group = isUserInGroup(user, groupId, groupRepository)
                            val events = eventRepository.getEventsForGroup(group).map { it.toDto() }
                            call.respond(events)
                        } else {
                            throw BadRequestException("Group id missing")
                        }
                    }
                    post("/{groupId}") {
                        val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                        val groupId = call.parameters["groupId"]?.toIntOrNull()
                        if (groupId != null) {
                            val group = isUserInGroup(user, groupId, groupRepository)
                            val event = call.receive<EventDTO>()
                            eventRepository.createEvent(
                                event,
                                user,
                                group
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
                                    ?: throw BadRequestException("Invalid event id")
                                isUserInGroup(user, event.group.id.value, groupRepository)
                                eventRepository.acceptEvent(event, user)
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

suspend fun isUserInGroup(user: UserDAO, groupId: Int, groupRepository: GroupRepository): GroupDAO {
    val group = groupRepository.getGroupById(groupId) ?: throw BadRequestException("Invalid group id")
    if (group.users.contains(user)) {
        return group
    } else {
        throw AuthenticationException("User not in group")
    }
}

suspend fun isGroupAdmin(user: UserDAO, groupId: Int, groupRepository: GroupRepository): Boolean {
    val group = groupRepository.getGroupById(groupId) ?: throw BadRequestException("Invalid group id")
    if (group.admins.contains(user)) {
        return true
    } else {
        throw AuthenticationException("User not in group")
    }
}

suspend fun getInfoFromPrincipal(call: ApplicationCall, jwtConfig: JWTConfig, userRepository: UserRepository): UserDAO {
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
