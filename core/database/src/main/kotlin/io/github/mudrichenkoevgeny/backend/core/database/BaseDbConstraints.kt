package io.github.mudrichenkoevgeny.backend.core.database

/**
 * Default maximum lengths for database string columns used across SDK tables.
 * Use these constants when defining Exposed table columns to keep schema consistent.
 */
object BaseDbConstraints {
    const val DEFAULT_MAX_LENGTH = 255
    const val ENUM_MAX_LENGTH = 32
    const val IP_MAX_LENGTH = 64
    const val LANGUAGE_MAX_LENGTH = 16
    const val VERSION_MAX_LENGTH = 64
}