package io.github.mudrichenkoevgeny.backend.core.database.extensions

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun Table.exists(): Boolean = try {
    newSuspendedTransaction(Dispatchers.IO) {
        this@exists.selectAll().limit(1).firstOrNull()
    }
    true
} catch (_: Exception) {
    false
}