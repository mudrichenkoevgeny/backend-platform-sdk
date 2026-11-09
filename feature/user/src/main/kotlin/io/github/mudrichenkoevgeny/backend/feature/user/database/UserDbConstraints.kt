package io.github.mudrichenkoevgeny.backend.feature.user.database

/**
 * Database schema constraints used by the user feature tables.
 *
 * These constants should stay aligned with the corresponding column definitions and migrations.
 */
public object UserDbConstraints {
    public const val PASSWORD_HASH_MAX_LENGTH: Int = 255
}
