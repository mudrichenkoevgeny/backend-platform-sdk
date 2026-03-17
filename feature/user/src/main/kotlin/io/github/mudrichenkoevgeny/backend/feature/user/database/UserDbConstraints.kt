package io.github.mudrichenkoevgeny.backend.feature.user.database

/**
 * Database schema constraints used by the user feature tables.
 *
 * These constants should stay aligned with the corresponding column definitions and migrations.
 */
object UserDbConstraints {
    const val PASSWORD_HASH_MAX_LENGTH = 255
}