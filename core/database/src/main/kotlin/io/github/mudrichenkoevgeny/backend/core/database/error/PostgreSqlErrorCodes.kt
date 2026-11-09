package io.github.mudrichenkoevgeny.backend.core.database.error

/**
 * Standard PostgreSQL SQLState error codes.
 */
public object PostgreSqlErrorCodes {
    /**
     * SQLState for unique index / primary key constraint violations (`23505`).
     */
    public const val UNIQUE_VIOLATION: String = "23505"
}
