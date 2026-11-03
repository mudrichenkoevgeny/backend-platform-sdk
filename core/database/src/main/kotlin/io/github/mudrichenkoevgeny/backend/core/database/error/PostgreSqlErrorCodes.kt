package io.github.mudrichenkoevgeny.backend.core.database.error

/**
 * Standard PostgreSQL SQLState error codes.
 */
object PostgreSqlErrorCodes {
    /**
     * SQLState for unique index / primary key constraint violations (`23505`).
     */
    const val UNIQUE_VIOLATION = "23505"
}
