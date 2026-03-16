package io.github.mudrichenkoevgeny.backend.core.common.config.seed

import kotlinx.serialization.Serializable

/**
 * Single admin account entry used for initial application seeding.
 *
 * Typically loaded from a JSON configuration file via [AdminList].
 *
 * @param email admin login email.
 * @param password plaintext password that will be hashed during seeding.
 */
@Serializable
data class AdminAccount(
    val email: String,
    val password: String
)