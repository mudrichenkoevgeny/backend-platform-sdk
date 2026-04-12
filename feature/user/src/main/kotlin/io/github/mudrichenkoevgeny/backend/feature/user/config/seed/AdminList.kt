package io.github.mudrichenkoevgeny.backend.feature.user.config.seed

import kotlinx.serialization.Serializable

/**
 * Container for a list of admin accounts loaded from configuration.
 *
 * This structure is typically deserialized from JSON and then used by seed routines
 * to create initial admin users in the system.
 *
 * @param admins collection of admin accounts to create.
 */
@Serializable
data class AdminList(
    val admins: List<AdminAccount>
)