package com.agilxp.fessedebouc

import com.agilxp.fessedebouc.config.configureAuth
import com.agilxp.fessedebouc.config.configureDatabases
import com.agilxp.fessedebouc.config.configureRouting
import com.agilxp.fessedebouc.model.JWTConfig
import com.agilxp.fessedebouc.model.OAuthConfig
import com.agilxp.fessedebouc.repository.PostgresGroupRepository
import com.agilxp.fessedebouc.repository.PostgresMessageRepository
import com.agilxp.fessedebouc.repository.PostgresUserRepository
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.netty.*
import java.time.Clock

fun main(args: Array<String>): Unit = EngineMain.main(args)

val applicationHttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}


fun Application.module(httpClient: HttpClient = applicationHttpClient) {
    val jwtConfig = environment.config.config("ktor.auth.jwt").jwtConfig()
    val oauthConfig = environment.config.config("ktor.auth.oauth.google").oauthConfig()
    val groupRepository = PostgresGroupRepository()
    val userRepository = PostgresUserRepository()
    val messageRepository = PostgresMessageRepository()
    configureAuth(clock = Clock.systemUTC(), jwtConfig = jwtConfig, oauthConfig = oauthConfig)
    configureDatabases()
    configureRouting(groupRepository, userRepository, messageRepository, jwtConfig)
}

fun ApplicationConfig.jwtConfig(): JWTConfig =
    JWTConfig(
        name = property("name").getString(),
        realm = property("realm").getString(),
        secret = property("secret").getString(),
        audience = property("audience").getString(),
        issuer = property("issuer").getString(),
        expirationSeconds = property("expirationSeconds").getString().toLong()
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
