package io.github.mudrichenkoevgeny.backend.core.database.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T =
    withContext(Dispatchers.IO) {
        suspendTransaction {
            block()
        }
    }