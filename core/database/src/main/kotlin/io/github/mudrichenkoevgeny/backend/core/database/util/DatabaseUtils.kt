package io.github.mudrichenkoevgeny.backend.core.database.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

/**
 * Runs the given block inside an Exposed [Transaction] on [Dispatchers.IO].
 *
 * Use for suspend database operations (e.g. in use cases or repositories). The transaction is committed on success.
 *
 * @param block code to run in the transaction.
 * @return the result of [block].
 */
suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T =
    withContext(Dispatchers.IO) {
        suspendTransaction {
            block()
        }
    }