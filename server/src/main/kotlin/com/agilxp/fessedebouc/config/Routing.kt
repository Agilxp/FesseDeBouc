package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.db.GroupDAO
import com.agilxp.fessedebouc.model.JWTConfig
import com.agilxp.fessedebouc.repository.GroupRepository
import com.agilxp.fessedebouc.repository.MessageRepository
import com.agilxp.fessedebouc.repository.UserRepository
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
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
                route("/groups") {
                    get {
                        val groups = groupRepository.getAllPublicGroups()
                        call.respond(groups)
                    }
                }
                route("/messages") {
                    get {
                        val messages = messageRepository.getMessagesForGroup(1)
                        call.respond(messages)
                    }
                }
            }
        }
    }
}