package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.db.Group
import com.agilxp.fessedebouc.db.RequestStatus
import com.agilxp.fessedebouc.db.RequestType
import com.agilxp.fessedebouc.db.User
import com.agilxp.fessedebouc.model.*
import com.agilxp.fessedebouc.repository.*
import com.agilxp.fessedebouc.util.EmailUtils
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.exceptions.ExposedSQLException
import java.util.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
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
            call.respond(HttpStatusCode.Unauthorized, SimpleMessageDTO(cause.message))
        }
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, SimpleMessageDTO(cause.message))
        }
        exception<DuplicateException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, SimpleMessageDTO(cause.message))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, SimpleMessageDTO(cause.message ?: ""))
        }
    }
    routing {
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
                        val groupId = UUID.fromString(call.parameters["groupId"])
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
                            val groupId = UUID.fromString(call.parameters["groupId"])
                            val userId = UUID.fromString(call.parameters["userId"])
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
                            val groupId = UUID.fromString(call.parameters["groupId"])
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
                    }
                    route("/invite") {
                        post("/send") {
                            val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                            val groupId = UUID.fromString(call.parameters["groupId"])
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
                            get("/{action}") {
                                val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                                val groupId = UUID.fromString(call.parameters["groupId"])
                                val invitationId = UUID.fromString(call.parameters["invitationId"])
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
                }
            }
            route("/messages") {
                get("/{groupId}") {
                    val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                    val groupId = UUID.fromString(call.parameters["groupId"])
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
                    val groupId = UUID.fromString(call.parameters["groupId"])
                    if (groupId != null) {
                        val group = isUserInGroup(user, groupId, groupRepository)
                        val multipart = call.receiveMultipart()
                        try {
                            val message = PostMessageDTO()
                            multipart.forEachPart { partData ->
                                when (partData) {
                                    is PartData.FormItem -> {
                                        //to read additional parameters that we sent with the image
                                        if (partData.name == "content") {
                                            message.content = partData.value
                                        }
                                    }

                                    is PartData.FileItem -> {
                                        val channel: ByteReadChannel = partData.provider.invoke()
                                        message.document = Base64.encode(channel.toInputStream().readAllBytes())
                                        message.documentFileName = partData.originalFileName
                                        message.documentContentType = partData.contentType?.toString()
                                    }

                                    is PartData.BinaryItem -> Unit
                                    else -> Unit
                                }
                            }
                            if (message.isEmpty()) {
                                throw BadRequestException("Message cannot be empty")
                            }
                            messageRepository.addMessageToGroup(message, user, group)
                            call.respond(HttpStatusCode.Created)
                        } catch (e: Exception) {
                            this@configureRouting.log.error("Exception when receiving multipart data: ${e.message}")
                            call.respond(HttpStatusCode.InternalServerError, "Error")
                        }
                    } else {
                        throw BadRequestException("Group id missing")
                    }
                }
            }
            route("/users") {
                get("/{groupId}") {
                    val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                    val groupId = UUID.fromString(call.parameters["groupId"])
                    val group = isUserInGroup(user, groupId, groupRepository)
                    val users = group.users.map { it.toDto() }
                    call.respond(users)
                }
            }
            route("/events") {
                route("/group") {
                    get("/{groupId}") {
                        val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                        val groupId = UUID.fromString(call.parameters["groupId"])
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
                        val groupId = UUID.fromString(call.parameters["groupId"])
                        if (groupId != null) {
                            val group = isUserInGroup(user, groupId, groupRepository)
                            val event = call.receive<EventDTO>()
                            val newEvent = eventRepository.createEvent(
                                event,
                                user,
                                group
                            )
                            group.users.forEach {
                                if (it.id != user.id) {
                                    EmailUtils.sendEmail(
                                        it.email,
                                        "New event created: ${event.name}",
                                        "Event ${event.name} from ${event.start} to ${event.end} at ${event.location}\n${event.description}"
                                    )
                                }
                            }
                            call.respond(HttpStatusCode.Created, newEvent.id.toString())
                        } else {
                            throw BadRequestException("Group id missing")
                        }
                    }
                }
                route("/reply") {
                    route("/{eventId}") {
                        get("/{action}") {
                            val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                            val eventId = UUID.fromString(call.parameters["eventId"])
                            val action = call.parameters["action"]
                            if (eventId != null && action != null) {
                                val event = eventRepository.getEventById(eventId)
                                    ?: throw BadRequestException("Invalid event id")
                                isUserInGroup(user, event.group.id, groupRepository)
                                when (action) {
                                    "accept" -> eventRepository.acceptEvent(event, user)
                                    "decline" -> eventRepository.declineEvent(event, user)
                                    "maybe" -> eventRepository.maybeEvent(event, user)
                                    else -> throw BadRequestException("Invalid action")
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

suspend fun isUserInGroup(user: User, groupId: UUID, groupRepository: GroupRepository): Group {
    val group = groupRepository.getGroupById(groupId) ?: throw BadRequestException("Invalid group id")
    if (group.users.contains(user)) {
        return group
    } else {
        throw AuthenticationException("User not in group")
    }
}

suspend fun isGroupAdmin(user: User, groupId: UUID, groupRepository: GroupRepository): Boolean {
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
        val userId: UUID =
            UUID.fromString(principal.payload.getClaim("user_id").asString())
                ?: throw AuthenticationException("Invalid principal")
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
        throw AuthenticationException("No principal found")
    }
}

data class AuthenticationException(override val message: String) : Exception()

data class BadRequestException(override val message: String) : Exception()

data class DuplicateException(override val message: String) : Exception()

@Serializable
data class SimpleMessageDTO(val message: String)
