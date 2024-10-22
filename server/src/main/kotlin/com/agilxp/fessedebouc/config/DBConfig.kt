package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.db.*
import com.agilxp.fessedebouc.model.DBConfigProperties
import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases(dbConfig: DBConfigProperties) {
    Database.connect(
        url = dbConfig.url,
        driver = dbConfig.driver,
        user = dbConfig.username,
        password = dbConfig.password
    )
    transaction {
        SchemaUtils.createMissingTablesAndColumns(Users)
        SchemaUtils.createMissingTablesAndColumns(Groups)
        SchemaUtils.createMissingTablesAndColumns(UserGroups)
        SchemaUtils.createMissingTablesAndColumns(Messages)
        SchemaUtils.createMissingTablesAndColumns(Events)
        SchemaUtils.createMissingTablesAndColumns(EventParticipants)
        SchemaUtils.createMissingTablesAndColumns(JoinGroupRequests)
    }
}

suspend fun <T> suspendTransaction(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)