package com.agilxp.fessedebouc

import com.agilxp.fessedebouc.config.configureAuth
import com.agilxp.fessedebouc.config.configureDatabases
import com.agilxp.fessedebouc.config.configureRouting
import com.agilxp.fessedebouc.model.DBConfigProperties
import com.agilxp.fessedebouc.model.JWTConfig
import com.agilxp.fessedebouc.model.OAuthConfig
import com.agilxp.fessedebouc.repository.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import java.time.Clock

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowCredentials = true
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }
    val jwtConfig = environment.config.config("ktor.auth.jwt").jwtConfig()
    val oauthConfig = environment.config.config("ktor.auth.oauth.google").oauthConfig()
    val dbConfig = environment.config.config("ktor.db").dbConfig()
    val groupRepository = GroupRepositoryImpl()
    val userRepository = UserRepositoryImpl()
    val messageRepository = MessageRepositoryImpl()
    val eventRepository = EventRepositoryImpl()
    val joinGroupRequestRepository = JoinGroupRequestRepositoryImpl()
    val tokenRepository = TokenRepositoryImpl()
    configureAuth(
        clock = Clock.systemUTC(),
        jwtConfig = jwtConfig,
        oauthConfig = oauthConfig,
        userRepository = userRepository,
        tokenRepository = tokenRepository,
    )
    configureDatabases(dbConfig)
    configureRouting(
        groupRepository,
        userRepository,
        messageRepository,
        eventRepository,
        joinGroupRequestRepository,
        jwtConfig
    )
}

fun ApplicationConfig.jwtConfig(): JWTConfig =
    JWTConfig(
        name = property("name").getString(),
        realm = property("realm").getString(),
        secret = property("secret").getString(),
        audience = property("audience").getString(),
        issuer = property("issuer").getString(),
    )

fun ApplicationConfig.oauthConfig(): OAuthConfig =
    OAuthConfig(
        name = property("name").getString(),
        clientId = property("clientId").getString(),
        clientSecret = property("clientSecret").getString(),
        accessTokenUrl = property("accessTokenUrl").getString(),
        authorizeUrl = property("authorizeUrl").getString(),
        redirectUrl = property("redirectUrl").getString(),
        userInfoUrl = property("userInfoUrl").getString(),
        defaultScopes = property("defaultScopes").getList()
    )

fun ApplicationConfig.dbConfig(): DBConfigProperties =
    DBConfigProperties(
        url = property("url").getString(),
        driver = property("driver").getString(),
        username = property("username").getString(),
        password = property("password").getString(),
    )
