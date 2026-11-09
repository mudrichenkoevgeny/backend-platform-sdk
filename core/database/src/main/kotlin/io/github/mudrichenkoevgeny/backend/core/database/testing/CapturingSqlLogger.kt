package io.github.mudrichenkoevgeny.backend.core.database.testing

import org.jetbrains.exposed.v1.core.SqlLogger
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.core.statements.expandArgs

/**
 * Custom [SqlLogger] that captures all generated SQL statements and expanded arguments
 * during transaction execution.
 */
public class CapturingSqlLogger : SqlLogger {
    public val statements: MutableList<String> = mutableListOf<String>()

    override fun log(
        context: StatementContext,
        transaction: Transaction
    ) {
        statements.add(context.expandArgs(transaction))
    }

    public fun clear() {
        statements.clear()
    }
}
