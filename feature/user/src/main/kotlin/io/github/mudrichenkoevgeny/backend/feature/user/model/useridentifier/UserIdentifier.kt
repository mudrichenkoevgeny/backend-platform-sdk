package io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import java.time.Instant

/**
 * User identifier bound to an auth provider (email/phone/external provider).
 *
 * A single logical user can have multiple identifiers (e.g. email + Google).
 *
 * @property id Stable identifier of this identifier record.
 * @property userId Owner user id.
 * @property userAuthProvider Auth provider backing this identifier.
 * @property identifier Provider-specific identifier (e.g. email, phone, external subject).
 * @property passwordHash Password hash for password-based providers; `null` for external providers.
 * @property createdAt Creation time.
 * @property updatedAt Last update time, if known.
 */
data class UserIdentifier(
    val id: UserIdentifierId,
    val userId: UserId,
    val userAuthProvider: UserAuthProvider,
    val identifier: String,
    val passwordHash: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant?
)