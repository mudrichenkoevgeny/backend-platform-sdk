package io.github.mudrichenkoevgeny.backend.feature.user.domain.model

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole

/**
 * Generic row-level visibility predicate based on owner user roles.
 *
 * Repository applies this as:
 * - owner role is in [allowedUserRoles]
 *
 * If [allowedUserRoles] is empty, listing returns no rows.
 */
data class UserRoleAccessFilter(
    val allowedUserRoles: Set<UserRole>
)
