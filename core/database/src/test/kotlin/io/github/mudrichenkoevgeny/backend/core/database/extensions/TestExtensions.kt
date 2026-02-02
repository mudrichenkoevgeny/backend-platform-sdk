package io.github.mudrichenkoevgeny.backend.core.database.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

suspend fun Table.exists(): Boolean = try {
    withContext(Dispatchers.IO) {
        suspendTransaction {
            this@exists.selectAll().limit(1).firstOrNull()
        }
    }
    true
} catch (_: Exception) {
    false
}