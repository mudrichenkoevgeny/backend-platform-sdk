package io.github.mudrichenkoevgeny.backend.feature.user.testutil

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

object ExposedTestDb {

    @Volatile
    private var initialized: Boolean = false

    fun initOnce() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            Database.connect(
                url = JDBC_URL,
                driver = JDBC_DRIVER,
                user = JDBC_USER,
                password = JDBC_PASSWORD
            )

            // Ensure there is an open connection for Exposed transactions.
            TransactionManager.manager.defaultIsolationLevel = java.sql.Connection.TRANSACTION_READ_COMMITTED

            initialized = true
        }
    }

    fun createSchema(vararg tables: org.jetbrains.exposed.v1.core.Table) {
        transaction {
            SchemaUtils.create(*tables)
        }
    }

    fun dropSchema(vararg tables: org.jetbrains.exposed.v1.core.Table) {
        transaction {
            SchemaUtils.drop(*tables)
        }
    }

    suspend fun <T> tx(block: suspend () -> T): T {
        return suspendTransaction { block() }
    }

    private const val JDBC_DRIVER = "org.h2.Driver"
    private const val JDBC_URL = "jdbc:h2:mem:feature_user_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;"
    private const val JDBC_USER = "sa"
    private const val JDBC_PASSWORD = ""
}

