package com.agilxp.fessedebouc.config

import com.agilxp.fessedebouc.db.*
import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/fessedebouc",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "mysecretpassword"
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