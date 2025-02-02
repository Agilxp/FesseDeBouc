package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.SimpleMessageDTO
import com.agilxp.fessedebouc.db.Group
import com.agilxp.fessedebouc.db.RequestStatus
import com.agilxp.fessedebouc.db.RequestType
import com.agilxp.fessedebouc.db.User
import com.agilxp.fessedebouc.model.*
import com.agilxp.fessedebouc.repository.*
import com.agilxp.fessedebouc.util.EmailUtils
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.TokenExpiredException
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.exceptions.ExposedSQLException
import java.util.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.seconds

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
        exception<AuthorizationException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, SimpleMessageDTO(cause.message))
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
        exception<TokenExpiredException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, SimpleMessageDTO(cause.message ?: ""))
        }
    }
    install(WebSockets) {
        pingPeriod = 10.seconds
        timeout = 10.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    val jwtVerifier = JWT
        .require(Algorithm.HMAC256(jwtConfig.secret))
        .withAudience(jwtConfig.audience)
        .withIssuer(jwtConfig.issuer)
        .build()
    routing {
        options("/{...}") {
            call.respond(HttpStatusCode.OK)
        }
        route("/ws") {
            webSocket("/me") {
                try {
                    val token = call.request.queryParameters["at"]
                    val user = getUserFromToken(jwtVerifier, token, userRepository)
                    val events = eventRepository.findUnansweredEventsForUser(user).map { it.toDto() }
                    val invitations =
                        joinGroupRequestRepository.findInvitationByUserEmail(user.email).map { it.toDto() }
                    val userStatus = UserStatusDTO(events, invitations, emptyList())
                    val status = StatusConnection(userStatus)
                    sendSerialized(userStatus)
                    while (true) {
                        delay(30000)
                        val events = eventRepository.findUnansweredEventsForUser(user).map { it.toDto() }
                        val invitations =
                            joinGroupRequestRepository.findInvitationByUserEmail(user.email).map { it.toDto() }
                        val newUserStatus = UserStatusDTO(events, invitations, emptyList())
                        if (status.hasChanges(newUserStatus)) {
                            sendSerialized(newUserStatus)
                        }
                    }
                } catch (e: Exception) {
                    log.error("Error in WebSocket status processing: ${e.message}")
                } finally {
                    println("Removing session")
                    close(CloseReason(CloseReason.Codes.NORMAL, "Closing..."))
                }
            }
            val messagingConnections = Collections.synchronizedSet<MessagingConnection>(LinkedHashSet())
            route("/messages") {
                webSocket("/{groupId}") {
                    val token = call.request.queryParameters["at"]
                    try {
                        val groupId = UUID.fromString(call.parameters["groupId"])
                        if (groupId != null) {
                            val user = getUserFromToken(jwtVerifier, token, userRepository)
                            val group = isUserInGroup(user, groupId, groupRepository)
                            val thisConnection = MessagingConnection(this, user, group)
                            println("Added session? " + messagingConnections.add(thisConnection))

                            try {
                                for (frame in incoming) {
                                    frame as? Frame.Text ?: continue
                                    val incomingMessage =
                                        Json.decodeFromString(PostMessageDTO.serializer(), frame.readText())
                                    if (incomingMessage.isEmpty()) {
                                        outgoing.send(Frame.Text("Message content cannot be empty"))
                                    } else {
                                        messageRepository.addMessageToGroup(incomingMessage, user, group)
                                        messagingConnections.filter { it.group == group }.forEach {
                                            it.session.send(
                                                Frame.Text(
                                                    Json.encodeToString(
                                                        MessageDTO.serializer(),
                                                        incomingMessage.toMessageDTO(user.toDto(), group.toDto())
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                log.error("Error in WebSocket message processing: ${e.message}")
                            } finally {
                                messagingConnections.remove(thisConnection)
                            }
                        } else {
                            throw BadRequestException("Group id missing")
                        }
                    } catch (e: Exception) {
                        log.error("Error in WebSocket connection: ${e.message}")
                    } finally {
                        println("Removing session")
                        close(CloseReason(CloseReason.Codes.NORMAL, "Closing..."))
                    }
                }
            }
        }
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
                            throw AuthorizationException("User ${user.id} not admin in group.")
                        }
                    }
                    route("/admin") {
                        put("/{userId}") {
                            val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                            val groupId = UUID.fromString(call.parameters["groupId"])
                            val userId = UUID.fromString(call.parameters["userId"])
                            if (groupId != null && userId != null && isGroupAdmin(user, groupId, groupRepository)) {
                                val group = groupRepository.getGroupById(groupId)
                                    ?: throw BadRequestException("Invalid group id")
                                val newAdmin = userRepository.getUserById(userId) ?: throw BadRequestException("Invalid user id")
                                if (group.admins.toList().contains(newAdmin)) {
                                    // Nothing to do
                                } else {
                                    groupRepository.addAdminToGroup(group, newAdmin)
                                }
                                call.respond(HttpStatusCode.OK)
                            } else {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        }
                        delete("/{userId}") {
                            val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                            val groupId = UUID.fromString(call.parameters["groupId"])
                            val userId = UUID.fromString(call.parameters["userId"])
                            if (groupId != null && userId != null && isGroupAdmin(user, groupId, groupRepository)) {
                                val group = groupRepository.getGroupById(groupId)
                                    ?: throw BadRequestException("Invalid group id")
                                val oldAdmin = userRepository.getUserById(userId) ?: throw BadRequestException("Invalid user id")
                                if (group.admins.toList().contains(user)) {
                                    groupRepository.removeAdminFromGroup(group, oldAdmin)
                                } else {
                                    // Nothing to do
                                }
                                call.respond(HttpStatusCode.OK)
                            }
                        }
                    }
                    route("/kick") {
                        delete("/{userId}") {
                            val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                            val groupId = UUID.fromString(call.parameters["groupId"])
                            val userId = UUID.fromString(call.parameters["userId"])
                            if (groupId != null && userId != null && (isGroupAdmin(user, groupId, groupRepository) || user.id == userId)) {
                                groupRepository.removeUserFromGroup(groupId, userId)
                                call.respond(HttpStatusCode.OK)
                            } else {
                                throw AuthorizationException("User ${user.id} not admin in group.")
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
                                throw AuthorizationException("User ${user.id} not admin in group.")
                            }
                        }

                        route("/{invitationId}") {
                            get("/{action}") {
                                val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                                val groupId = UUID.fromString(call.parameters["groupId"])
                                val invitationId = UUID.fromString(call.parameters["invitationId"])
                                val action = call.parameters["action"] ?: throw BadRequestException("Invalid URL")
                                if (invitationId != null && groupId != null) {
                                    val group = groupRepository.getGroupById(groupId)
                                        ?: throw BadRequestException("Invalid group id")
                                    val invitation = joinGroupRequestRepository.findByIdAndGroup(
                                        invitationId,
                                        group.id,
                                    )
                                    if (invitation != null) {
                                        if (invitation.email == user.email) {
                                            if (invitation.status == RequestStatus.PENDING) {
                                                var pastTenseAction = ""
                                                var text = ""
                                                when (action) {
                                                    "accept" -> {
                                                        try {
                                                            groupRepository.addUserToGroup(group, user, false)
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
                                            throw AuthorizationException("User email not matching invitation email")
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
            route("/invitations") {
                get("/mine") {
                    val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                    val myInvitations = joinGroupRequestRepository.findInvitationByUserEmail(user.email)
                    call.respond(myInvitations.map { it.toDto() })
                }
            }
            get("/me") {
                val user = getInfoFromPrincipal(call, jwtConfig, userRepository)
                val events = eventRepository.findUnansweredEventsForUser(user).map { it.toDto() }
                val invitations = joinGroupRequestRepository.findInvitationByUserEmail(user.email).map { it.toDto() }
                call.respond(UserStatusDTO(events, invitations, emptyList()))
            }
        }
    }
}

suspend fun isUserInGroup(user: User, groupId: UUID, groupRepository: GroupRepository): Group {
    val group = groupRepository.getGroupById(groupId) ?: throw BadRequestException("Invalid group id")
    if (group.users.contains(user)) {
        return group
    } else {
        throw AuthorizationException("User not in group")
    }
}

suspend fun isGroupAdmin(user: User, groupId: UUID, groupRepository: GroupRepository): Boolean {
    val group = groupRepository.getGroupById(groupId) ?: throw BadRequestException("Invalid group id")
    if (group.admins.contains(user)) {
        return true
    } else {
        throw AuthorizationException("User not admin in group")
    }
}

suspend fun getUserFromToken(jwtVerifier: JWTVerifier, token: String?, userRepository: UserRepository): User {
    if (token == null) {
        throw AuthenticationException("No token")
    }
    val principal = JWTPrincipal(jwtVerifier.verify(token))
    return validatePrincipal(principal, userRepository)
}

suspend fun getInfoFromPrincipal(call: ApplicationCall, jwtConfig: JWTConfig, userRepository: UserRepository): User {
    val principal = call.principal<JWTPrincipal>(jwtConfig.name)
    return validatePrincipal(principal, userRepository)
}

suspend fun validatePrincipal(principal: JWTPrincipal?, userRepository: UserRepository): User {
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

data class AuthorizationException(override val message: String) : Exception()

data class BadRequestException(override val message: String) : Exception()

data class DuplicateException(override val message: String) : Exception()
